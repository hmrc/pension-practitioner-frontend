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
import renderer.Renderer
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.countryOptions.CountryOptions

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class PartnershipContactAddressController @Inject()(
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
      (AreYouUKCompanyPage and BusinessNamePage).retrieve.right.map {
        retrievedData =>
          val json = retrievedData match {
            case areYouUKCompany ~ companyName =>
              val filledForm = request.userAnswers
                .get(PartnershipAddressPage)
                .fold(form)(form.fill)
              commonJson(mode, companyName, filledForm, areYouUKCompany)
          }
          renderer.render(viewTemplate, json).map(Ok(_))
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      (AreYouUKCompanyPage and BusinessNamePage).retrieve.right.map {
        retrievedData =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors => {
                val json = retrievedData match {
                  case isUK ~ companyName =>
                    commonJson(mode, companyName, formWithErrors, isUK = isUK)
                }
                renderer.render(viewTemplate, json).map(BadRequest(_))
              },
              value =>
                for {
                  updatedAnswers <- Future
                    .fromTry(request.userAnswers.set(PartnershipAddressPage, value))
                  _ <- userAnswersCacheConnector.save(updatedAnswers.data)
                } yield {
                  Redirect(
                    navigator.nextPage(PartnershipAddressPage, mode, updatedAnswers)
                  )
                }
            )
      }
    }

  private def commonJson(
    mode: Mode,
    companyName: String,
    form: Form[Address],
    isUK: Boolean
  )(implicit request: DataRequest[AnyContent]): JsObject = {
    val messages = request2Messages
    val extraJson = if (isUK) {
      Json.obj("postcodeFirst" -> true)
    } else {
      Json.obj()
    }

    val pageTitle = messages("address.title", companyName)
    val h1 = messages("address.title", companyName)

    Json.obj(
      "submitUrl" -> routes.PartnershipContactAddressController.onSubmit(mode).url,
      "postcodeEntry" -> true,
      "form" -> form,
      "countries" -> jsonCountries(form.data.get("country"), config)(messages),
      "pageTitle" -> pageTitle,
      "h1" -> h1
    ) ++ extraJson
  }
}


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
import pages.partnership.BusinessNamePage
import pages.partnership.PartnershipAddressPage
import pages.register.AreYouUKCompanyPage
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import renderer.Renderer
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.countryOptions.CountryOptions
import viewmodels.CommonViewModel

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

//class PartnershipContactAddressController @Inject()(override val messagesApi: MessagesApi,
//  val userAnswersCacheConnector: UserAnswersCacheConnector,
//  val navigator: CompoundNavigator,
//  identify: IdentifierAction,
//  getData: DataRetrievalAction,
//  requireData: DataRequiredAction,
//  formProvider: AddressFormProvider,
//  countryOptions: CountryOptions,
//  val controllerComponents: MessagesControllerComponents,
//  val config: FrontendAppConfig,
//  val renderer: Renderer
//)(implicit ec: ExecutionContext) extends ManualAddressController
//  with Retrievals with I18nSupport with NunjucksSupport {
//
//  def form(implicit messages: Messages): Form[Address] = formProvider()
//
//  def onPageLoad(mode: Mode): Action[AnyContent] =
//    (identify andThen getData andThen requireData).async {
//      implicit request =>
//        (AreYouUKCompanyPage and BusinessNamePage).retrieve.right.map { retrievedData =>
//          val json = retrievedData match {
//            case true ~ companyName =>
//              commonJson(mode, companyName) ++
//                Json.obj("form" -> request.userAnswers.get(PartnershipAddressPage).fold(form)(form.fill))
//            case false ~ companyName =>
//              val filledForm = request.userAnswers.get(PartnershipAddressPage).fold(form)(form.fill)
//              commonJson(mode, companyName) ++
//                Json.obj(
//                  "form" -> filledForm,
//                  "countries" -> jsonCountries(filledForm.data.get("country"), config)(request2Messages)
//                )
//          }
//          renderer.render(viewTemplate, json).map(Ok(_))
//        }
//    }
//
//  def onSubmit(mode: Mode): Action[AnyContent] =
//    (identify andThen getData andThen requireData).async {
//      implicit request =>
//        (AreYouUKCompanyPage and BusinessNamePage).retrieve.right.map { retrievedData =>
//          form.bind(retrieveFieldsFromRequestAndAddCountryForUK).fold(
//            formWithErrors => {
//              val json = retrievedData match {
//                case true ~ companyName =>
//                  commonJson(mode, companyName) ++ Json.obj("form" -> form)
//                case false ~ companyName =>
//                  commonJson(mode, companyName) ++
//                    Json.obj(
//                      "form" -> formWithErrors,
//                      "countries" -> jsonCountries(formWithErrors.data.get("country"), config)(request2Messages)
//                    )
//              }
//              renderer.render(viewTemplate, json).map(BadRequest(_))
//            },
//            value =>
//              for {
//                updatedAnswers <- Future.fromTry(request.userAnswers.set(PartnershipAddressPage, value))
//                _ <- userAnswersCacheConnector.save(updatedAnswers.data)
//              } yield {
//                Redirect(navigator.nextPage(PartnershipAddressPage, mode, updatedAnswers))
//              }
//          )
//        }
//    }
//
//  private def commonJson(mode: Mode, companyName: String)(implicit request: DataRequest[AnyContent]) = {
//    Json.obj(
//      "viewmodel" -> CommonViewModel(
//        "partnership",
//        companyName,
//        routes.PartnershipContactAddressController.onSubmit(mode).url),
//      "postcodeEntry" -> true
//    )
//  }
//}
