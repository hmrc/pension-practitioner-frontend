/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.individual

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.address.UseAddressForContactFormProvider
import models.requests.DataRequest
import models.{Address, Mode, NormalMode, TolerantAddress}
import navigators.CompoundNavigator
import pages.individual.{AreYouUKResidentPage, IndividualAddressPage, IndividualManualAddressPage, UseAddressForContactPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.AuthMustHaveNoEnrolmentWithIV
import utils.countryOptions.CountryOptions
import viewmodels.{CommonViewModel, Radios}
import views.html.address.UseAddressForContactView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UseAddressForContactController @Inject()(override val messagesApi: MessagesApi,
                                               userAnswersCacheConnector: UserAnswersCacheConnector,
                                               navigator: CompoundNavigator,
                                               @AuthMustHaveNoEnrolmentWithIV authenticate: AuthAction,
                                               getData: DataRetrievalAction,
                                               requireData: DataRequiredAction,
                                               formProvider: UseAddressForContactFormProvider,
                                               val controllerComponents: MessagesControllerComponents,
                                               countryOptions: CountryOptions,
                                               useAddressForContactView: UseAddressForContactView
                                              )(implicit ec: ExecutionContext) extends FrontendBaseController
  with I18nSupport with Retrievals {


  private def form(implicit messages: Messages): Form[Boolean] =
    formProvider(messages("useAddressForContact.error.required", messages("individual.you")))

  def onPageLoad(mode: Mode): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      request.userAnswers.get(AreYouUKResidentPage) match {
        case Some(true) =>
          val preparedForm = request.userAnswers.get(UseAddressForContactPage).fold(form)(form.fill)
          getJson(preparedForm) { json =>
            Future.successful(Ok(useAddressForContactView(
              routes.UseAddressForContactController.onSubmit(),
              preparedForm,
              Radios.yesNo(preparedForm("value")),
              (json \ "viewmodel" \ "entityType").asOpt[String].getOrElse(""),
              (json \ "viewmodel" \ "entityName").asOpt[String].getOrElse(""),
              (json \ "address").asOpt[Seq[String]].getOrElse(Seq.empty[String])
            )))
          }
        case _ => Future.successful(
          Redirect(controllers.individual.routes.AreYouUKResidentController.onPageLoad(mode))
        )
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => {
          getJson(formWithErrors) { json =>
            Future.successful(BadRequest(useAddressForContactView(
              routes.UseAddressForContactController.onSubmit(),
              formWithErrors,
              Radios.yesNo(formWithErrors("value")),
              (json \ "viewmodel" \ "entityType").asOpt[String].getOrElse(""),
              (json \ "viewmodel" \ "entityName").asOpt[String].getOrElse(""),
              (json \ "address").asOpt[Seq[String]].getOrElse(Seq.empty[String])
            )))
          }
        },
        value => {
          IndividualAddressPage.retrieve.map { address =>
            val ua = request.userAnswers.setOrException(UseAddressForContactPage, value)
            val updatedAnswers = if (value) {
              ua.setOrException(IndividualManualAddressPage, address)
            } else {
              ua
            }
            userAnswersCacheConnector.save(updatedAnswers.data).map { _ =>
              Redirect(navigator.nextPage(UseAddressForContactPage, NormalMode, updatedAnswers))
            }
          }
        }
      )
  }

  private def getJson(form: Form[Boolean])(block: JsObject => Future[Result])
                     (implicit request: DataRequest[AnyContent]): Future[Result] =
    IndividualAddressPage.retrieve.map { address =>
      val json = Json.obj(
        "viewmodel" -> CommonViewModel("individual.you", "individual.you", routes.UseAddressForContactController.onSubmit().url),
        "radios" -> Radios.yesNo(form("value")),
        "address" -> address.lines(countryOptions),
        "entity" -> "Individual"
      )
      block(json)
    }

  protected def getResolvedAddress(tolerantAddress: TolerantAddress): Option[Address] = {
    tolerantAddress.addressLine1 match {
      case None => None
      case Some(aLine1) =>
        tolerantAddress.addressLine2 match {
          case None => None
          case Some(aLine2) => Some(Address(
            aLine1,
            aLine2,
            tolerantAddress.addressLine3,
            tolerantAddress.addressLine4,
            tolerantAddress.postcode,
            tolerantAddress.countryOpt.getOrElse("GB")
          ))
        }
    }
  }
}
