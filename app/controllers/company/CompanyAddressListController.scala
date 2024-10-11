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

package controllers.company

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import controllers.address.{AddressListController, AddressPages}
import forms.address.AddressListFormProvider
import models.Mode
import navigators.CompoundNavigator
import pages.company.{BusinessNamePage, CompanyAddressListPage, CompanyAddressPage, CompanyPostcodePage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import utils.countryOptions.CountryOptions
import viewmodels.{CommonViewModel, CommonViewModelTwirl}
import views.html.address.AddressListView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class CompanyAddressListController @Inject()(override val messagesApi: MessagesApi,
                                             val userAnswersCacheConnector: UserAnswersCacheConnector,
                                             val navigator: CompoundNavigator,
                                             authenticate: AuthAction,
                                             getData: DataRetrievalAction,
                                             requireData: DataRequiredAction,
                                             formProvider: AddressListFormProvider,
                                             val controllerComponents: MessagesControllerComponents,
                                             countryOptions: CountryOptions,
                                             val renderer: Renderer,
                                             addressListView: AddressListView
                                         )(implicit ec: ExecutionContext) extends AddressListController
                                          with Retrievals with I18nSupport {


  def form(implicit messages: Messages): Form[Int] =
    formProvider(messages("addressList.error.required", messages("company")))

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        getFormToJson(mode).retrieve.map(x =>
          get(
            x,
            routes.CompanyAddressListController.onSubmit(mode),
            routes.CompanyContactAddressController.onPageLoad(mode).url,
            (model: CommonViewModelTwirl, radios: Seq[RadioItem]) => addressListView(form, radios, model)
          )
        )
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        val addressPages: AddressPages = AddressPages(CompanyPostcodePage, CompanyAddressListPage, CompanyAddressPage)
        getFormToJson(mode).retrieve.map(post(mode, _, addressPages, manualUrlCall = routes.CompanyContactAddressController.onPageLoad(mode),
          routes.CompanyAddressListController.onSubmit(mode),
          (model: CommonViewModelTwirl, radios: Seq[RadioItem], formWithErrors: Form[Int]) => addressListView(formWithErrors, radios, model)
        ))
    }

  def getFormToJson(mode: Mode): Retrieval[Form[Int] => JsObject] =
    Retrieval(
      implicit request =>
      (BusinessNamePage and CompanyPostcodePage).retrieve.map {
        case companyName ~ addresses =>
          form => Json.obj(
            "addresses" -> transformAddressesForTemplate(addresses, countryOptions),
            "viewmodel" -> CommonViewModel(
              "company",
              companyName,
              routes.CompanyAddressListController.onSubmit(mode).url,
              Some(routes.CompanyContactAddressController.onPageLoad(mode).url)
            ))
      }
  )
}
