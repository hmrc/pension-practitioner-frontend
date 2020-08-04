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

package controllers.partnership

import config.FrontendAppConfig
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import controllers.address.ManualAddressController
import forms.address.AddressFormProvider
import javax.inject.Inject
import models.requests.DataRequest
import models.Address
import models.Mode
import navigators.CompoundNavigator
import pages.QuestionPage
import pages.partnership.BusinessNamePage
import pages.partnership.PartnershipAddressPage
import pages.register.AreYouUKCompanyPage
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Result
import renderer.Renderer
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.countryOptions.CountryOptions
import viewmodels.CommonViewModel

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class PartnershipAddressController @Inject()(override val messagesApi: MessagesApi,
  val userAnswersCacheConnector: UserAnswersCacheConnector,
  val navigator: CompoundNavigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: AddressFormProvider,
  countryOptions: CountryOptions,
  val controllerComponents: MessagesControllerComponents,
  val config: FrontendAppConfig,
  val renderer: Renderer
)(implicit ec: ExecutionContext) extends ManualAddressController
  with Retrievals with I18nSupport with NunjucksSupport {

  def form(implicit messages: Messages): Form[Address] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async {
      implicit request =>
        getFormToJsonForLoad(mode).retrieve.right.map(get)
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async {
      implicit request =>
        getFormToJsonForSubmit(mode).retrieve.right.map(post(mode, _, PartnershipAddressPage))
    }

  override def post(mode: Mode, json: Form[Address] => JsObject, addressPage: QuestionPage[Address])
    (implicit request: DataRequest[AnyContent], ec: ExecutionContext, hc: HeaderCarrier, messages: Messages): Future[Result] = {

    form(messages).bind(retrieveFieldsFromRequestAndAddCountryForUK).fold(
      formWithErrors =>
        renderer.render(viewTemplate, json(formWithErrors)).map(BadRequest(_)),
      value =>
        for {
          updatedAnswers <- Future.fromTry(request.userAnswers.set(addressPage, value))
          _ <- userAnswersCacheConnector.save(updatedAnswers.data)
        } yield {
          Redirect(navigator.nextPage(addressPage, mode, updatedAnswers))
        }
    )
  }

  private def getFormToJsonForLoad(mode: Mode)(implicit request: DataRequest[AnyContent]): Retrieval[Form[Address] => JsObject] =
    Retrieval(
      implicit request => {
        (AreYouUKCompanyPage and BusinessNamePage).retrieve.right.map {
          case true ~ companyName =>
            form =>
              commonJson(mode, companyName) ++
                Json.obj("form" -> request.userAnswers.get(PartnershipAddressPage).fold(form)(form.fill))
          case false ~ companyName =>
            form =>
              val filledForm = request.userAnswers.get(PartnershipAddressPage).fold(form)(form.fill)
              commonJson(mode, companyName) ++
                Json.obj(
                  "form" -> filledForm,
                  "countries" -> jsonCountries(filledForm.data.get("country"), config)(request2Messages)
                )
        }
      }
    )

  private def getFormToJsonForSubmit(mode: Mode)(implicit request: DataRequest[AnyContent]): Retrieval[Form[Address] => JsObject] =
    Retrieval(
      implicit request => {
        (AreYouUKCompanyPage and BusinessNamePage).retrieve.right.map {
          case true ~ companyName =>
            form => commonJson(mode, companyName) ++ Json.obj("form" -> form)
          case false ~ companyName =>
            form => commonJson(mode, companyName) ++
              Json.obj(
                "form" -> form,
                "countries" -> jsonCountries(form.data.get("country"), config)(request2Messages)
              )
        }
      }
    )

  private def commonJson(mode: Mode, companyName: String)(implicit request: DataRequest[AnyContent]) = {
    Json.obj(
      "viewmodel" -> CommonViewModel(
        "partnership",
        companyName,
        routes.PartnershipAddressController.onSubmit(mode).url),
      "postcodeEntry" -> true
    )
  }
}
