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

package controllers.individual

import controllers.actions.IdentifierAction
import models.Address
import play.api.mvc.AnyContent

import scala.concurrent.ExecutionContext
import models.requests.DataRequest
import play.api.mvc.MessagesControllerComponents
import play.api.data.Form
import play.api.libs.json.JsObject
import play.api.mvc.Action
import connectors.cache.UserAnswersCacheConnector
import config.FrontendAppConfig
import javax.inject.Inject
import play.api.libs.json.Json
import controllers.address.ManualAddressController
import navigators.CompoundNavigator
import controllers.actions.DataRetrievalAction
import forms.address.AddressFormProvider
import models.Mode
import play.api.i18n.I18nSupport
import utils.countryOptions.CountryOptions
import controllers.Retrievals
import play.api.i18n.MessagesApi
import renderer.Renderer

import scala.concurrent.Future
import play.api.i18n.Messages
import controllers.actions.DataRequiredAction
import pages.individual.AreYouUKResidentPage
import pages.individual.IndividualDetailsPage
import pages.individual.IndividualManualAddressPage
import uk.gov.hmrc.viewmodels.NunjucksSupport


class IndividualContactAddressController @Inject()(
  override val messagesApi: MessagesApi,
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
)(implicit ec: ExecutionContext)
  extends ManualAddressController
    with Retrievals
    with I18nSupport
    with NunjucksSupport {

  def form(implicit messages: Messages): Form[Address] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      (AreYouUKResidentPage and IndividualDetailsPage).retrieve.right.map {
        retrievedData =>
          val json = retrievedData match {
            case areYouUKResident ~ individualDetails =>
              val filledForm = request.userAnswers
                .get(IndividualManualAddressPage)
                .fold(form)(form.fill)
              commonJson(mode, individualDetails.fullName, filledForm, areYouUKResident)
          }
          renderer.render(viewTemplate, json).map(Ok(_))
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      (AreYouUKResidentPage and IndividualDetailsPage).retrieve.right.map {
        retrievedData =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors => {
                val json = retrievedData match {
                  case isUK ~ individualDetails =>
                    commonJson(mode, individualDetails.fullName, formWithErrors, isUK = isUK)
                }
                renderer.render(viewTemplate, json).map(BadRequest(_))
              },
              value =>
                for {
                  updatedAnswers <- Future
                    .fromTry(request.userAnswers.set(IndividualManualAddressPage, value))
                  _ <- userAnswersCacheConnector.save(updatedAnswers.data)
                } yield {
                  Redirect(
                    navigator.nextPage(IndividualManualAddressPage, mode, updatedAnswers)
                  )
                }
            )
      }
    }

  private def commonJson(
    mode: Mode,
    individualDetails: String,
    form: Form[Address],
    isUK: Boolean
  )(implicit request: DataRequest[AnyContent]): JsObject = {
    val messages = request2Messages
    val extraJson = if (isUK) {
      Json.obj("postcodeFirst" -> true)
    } else {
      Json.obj()
    }

    val pageTitle = messages("individual.address.title")
    val h1 = messages("individual.address.title")

    Json.obj(
      "submitUrl" -> routes.IndividualContactAddressController.onSubmit(mode).url,
      "postcodeEntry" -> true,
      "form" -> form,
      "countries" -> jsonCountries(form.data.get("country"), config)(messages),
      "pageTitle" -> pageTitle,
      "h1" -> h1
    ) ++ extraJson
  }
}
