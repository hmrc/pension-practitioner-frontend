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

import config.FrontendAppConfig
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.address.AddressFormProvider
import javax.inject.Inject
import models.requests.DataRequest
import models.{Address, Mode}
import navigators.CompoundNavigator
import pages.company.{CompanyAddressPage, CompanyNamePage, CompanyPostcodePage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.{JsArray, JsObject, Json, Writes}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport

import scala.concurrent.{ExecutionContext, Future}

class CompanyAddressController @Inject()(override val messagesApi: MessagesApi,
                                          userAnswersCacheConnector: UserAnswersCacheConnector,
                                          navigator: CompoundNavigator,
                                          identify: IdentifierAction,
                                          getData: DataRetrievalAction,
                                          requireData: DataRequiredAction,
                                          formProvider: AddressFormProvider,
                                          val controllerComponents: MessagesControllerComponents,
                                          config: FrontendAppConfig,
                                          renderer: Renderer
                                         )(implicit ec: ExecutionContext) extends FrontendBaseController
  with Retrievals with I18nSupport with NunjucksSupport {

  private def form(implicit messages: Messages): Form[Address] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async {
      implicit request =>
        getJson(mode, form) { json =>
          renderer.render("address/manualAddress.njk", json).map(Ok(_))
        }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async {
      implicit request =>
        form.bindFromRequest().fold(
          formWithErrors =>
            getJson(mode, formWithErrors) { json =>
              renderer.render("address/manualAddress.njk", json).map(BadRequest(_))
            },
          value =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(CompanyAddressPage, value))
                  _ <- userAnswersCacheConnector.save(updatedAnswers.data)
                } yield Redirect(navigator.nextPage(CompanyAddressPage, mode, updatedAnswers))
        )
    }

  private def getJson(mode: Mode, form: Form[Address])(block: JsObject => Future[Result])
                     (implicit w: Writes[Form[Address]], messages: Messages, request: DataRequest[AnyContent]): Future[Result] =
    CompanyNamePage.retrieve.right.map { companyName =>
      val json = Json.obj(
        "form" -> form,
        "entityType" -> messages("company"),
        "entityName" -> companyName,
        "submitUrl" -> routes.CompanyAddressController.onSubmit(mode).url,
        "countries" -> jsonCountries(form.data.get("country"))(request2Messages)
      )
      block(json)
    }

  private def countryJsonElement(tuple: (String, String), isSelected: Boolean): JsArray = Json.arr(
    if (isSelected) {
      Json.obj(
        "value" -> tuple._1,
        "text" -> tuple._2,
        "selected" -> true
      )
    } else {
      Json.obj(
        "value" -> tuple._1,
        "text" -> tuple._2
      )
    }
  )

  private def jsonCountries(countrySelected: Option[String])(implicit messages: Messages): JsArray =
    config.validCountryCodes
      .map(countryCode => (countryCode, messages(s"country.$countryCode")))
      .sortWith(_._2 < _._2)
      .foldLeft(JsArray(Seq(Json.obj("value" -> "", "text" -> "")))) { (acc, nextCountryTuple) =>
        acc ++ countryJsonElement(nextCountryTuple, countrySelected.contains(nextCountryTuple._1))
      }
}
