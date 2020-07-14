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

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import controllers.address.{AddressListController, AddressPages}
import forms.address.AddressListFormProvider
import javax.inject.Inject
import models.Mode
import models.requests.DataRequest
import navigators.CompoundNavigator
import pages.company.{CompanyAddressListPage, CompanyAddressPage, CompanyPostcodePage}
import pages.company.BusinessNamePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.countryOptions.CountryOptions
import viewmodels.CommonViewModel

import scala.concurrent.ExecutionContext

class CompanyAddressListController @Inject()(override val messagesApi: MessagesApi,
                                             val userAnswersCacheConnector: UserAnswersCacheConnector,
                                             val navigator: CompoundNavigator,
                                             identify: IdentifierAction,
                                             getData: DataRetrievalAction,
                                             requireData: DataRequiredAction,
                                             formProvider: AddressListFormProvider,
                                             val controllerComponents: MessagesControllerComponents,
                                             countryOptions: CountryOptions,
                                             val renderer: Renderer
                                         )(implicit ec: ExecutionContext) extends AddressListController
                                          with Retrievals with I18nSupport with NunjucksSupport {


  def form(implicit messages: Messages): Form[Int] =
    formProvider(messages("addressList.error.invalid", messages("company")))

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async {
      implicit request =>
        getFormToJson(mode).retrieve.right.map(get)
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async {
      implicit request =>
        val addressPages: AddressPages = AddressPages(CompanyPostcodePage, CompanyAddressListPage, CompanyAddressPage)
        getFormToJson(mode).retrieve.right.map(post(mode, _, addressPages))
    }

  def getFormToJson(mode: Mode)(implicit request: DataRequest[AnyContent]): Retrieval[Form[Int] => JsObject] =
    Retrieval(
      implicit request =>
      (BusinessNamePage and CompanyPostcodePage).retrieve.right.map {
        case companyName ~ addresses =>
          form => Json.obj(
            "form" -> form,
            "addresses" -> transformAddressesForTemplate(addresses, countryOptions),
            "viewmodel" -> CommonViewModel(
              "company",
              companyName,
              routes.CompanyAddressListController.onSubmit(mode).url,
              Some(routes.CompanyAddressController.onPageLoad(mode).url)
            ))
      }
  )

}
