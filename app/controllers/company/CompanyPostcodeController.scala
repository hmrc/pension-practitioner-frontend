/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.company

import connectors.AddressLookupConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.FormsHelper.formWithError
import forms.address.PostcodeFormProvider
import javax.inject.Inject
import models.Mode
import models.requests.DataRequest
import navigators.CompoundNavigator
import pages.company.{CompanyNamePage, CompanyPostcodePage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.{JsObject, Json, Writes}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport

import scala.concurrent.{ExecutionContext, Future}

class CompanyPostcodeController @Inject()(override val messagesApi: MessagesApi,
                                          userAnswersCacheConnector: UserAnswersCacheConnector,
                                          navigator: CompoundNavigator,
                                          identify: IdentifierAction,
                                          getData: DataRetrievalAction,
                                          requireData: DataRequiredAction,
                                          formProvider: PostcodeFormProvider,
                                          addressLookupConnector: AddressLookupConnector,
                                          val controllerComponents: MessagesControllerComponents,
                                          renderer: Renderer
                                         )(implicit ec: ExecutionContext) extends FrontendBaseController
                                          with Retrievals with I18nSupport with NunjucksSupport {

  private def form(implicit messages: Messages): Form[String] =
    formProvider(
      messages("postcode.error.required", messages("company")),
      messages("postcode.error.invalid", messages("company"))
    )

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData() andThen requireData).async {
      implicit request =>
        getJson(mode, form) { json =>
          renderer.render("address/postcode.njk", json).map(Ok(_))
        }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData() andThen requireData).async {
      implicit request =>

          form.bindFromRequest().fold(
            formWithErrors => {
              getJson(mode, formWithErrors) { json =>
                renderer.render("address/postcode.njk", json).map(BadRequest(_))
            }
            },
            value =>
              addressLookupConnector.addressLookupByPostCode(value).flatMap {
                case Nil =>
                  getJson(mode, formWithError(form, "company.postcode.error.invalid")) { json =>
                    renderer.render("address/postcode.njk", json).map(BadRequest(_))
                  }
                case addresses =>
                  for {
                    updatedAnswers <- Future.fromTry(request.userAnswers.set(CompanyPostcodePage, addresses))
                    _ <- userAnswersCacheConnector.save(updatedAnswers.data)
                  } yield Redirect(navigator.nextPage(CompanyPostcodePage, mode, updatedAnswers))
              }
          )

    }

  private def getJson(mode: Mode, form: Form[String])(block: JsObject => Future[Result])
                     (implicit w: Writes[Form[String]], messages: Messages, request: DataRequest[AnyContent]): Future[Result] =
    CompanyNamePage.retrieve.right.map { companyName =>
      val json = Json.obj(
        "form" -> form,
        "entityType" -> messages("company"),
        "entityName" -> companyName,
        "submitUrl" -> routes.CompanyPostcodeController.onSubmit(mode).url,
        "enterManuallyUrl" -> routes.CompanyAddressController.onPageLoad(mode).url
      )
      block(json)
    }
}
