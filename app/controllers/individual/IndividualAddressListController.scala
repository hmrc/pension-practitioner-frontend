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
import controllers.address.{AddressListController, AddressPages}
import forms.address.AddressListFormProvider
import models.Mode
import navigators.CompoundNavigator
import pages.individual.{IndividualAddressListPage, IndividualDetailsPage, IndividualManualAddressPage, IndividualPostcodePage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import utils.countryOptions.CountryOptions
import viewmodels.{CommonViewModel, CommonViewModelTwirl}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class IndividualAddressListController @Inject()(override val messagesApi: MessagesApi,
                                                val userAnswersCacheConnector: UserAnswersCacheConnector,
                                                val navigator: CompoundNavigator,
                                                authenticate: AuthAction,
                                                getData: DataRetrievalAction,
                                                requireData: DataRequiredAction,
                                                formProvider: AddressListFormProvider,
                                                val controllerComponents: MessagesControllerComponents,
                                                countryOptions: CountryOptions,
                                                addressListView: views.html.individual.AddressListView
                                               )(implicit ec: ExecutionContext) extends AddressListController
  with Retrievals with I18nSupport {


  def form(implicit messages: Messages): Form[Int] =
    formProvider(messages("individual.addressList.error.required"))

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        getFormToJson(mode).retrieve.map(
          get(_,routes.IndividualAddressListController.onSubmit(mode),
            routes.IndividualContactAddressController.onSubmit(mode).url,
            (model: CommonViewModelTwirl, radios: Seq[RadioItem]) => addressListView(model, form, radios))
        )
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        val addressPages: AddressPages = AddressPages(IndividualPostcodePage, IndividualAddressListPage, IndividualManualAddressPage)
        getFormToJson(mode).retrieve.map(
          post(mode, _,
            addressPages,
            manualUrlCall = routes.IndividualContactAddressController.onPageLoad(mode),
            routes.IndividualAddressListController.onSubmit(mode),
            (model: CommonViewModelTwirl, radios: Seq[RadioItem], formWithErrors: Form[Int]) => addressListView(model,
              formWithErrors, radios)
        )
}

  private def getFormToJson(mode: Mode): Retrieval[Form[Int] => JsObject] =
    Retrieval(
      implicit request =>
        (IndividualDetailsPage.and(IndividualPostcodePage)).retrieve.map {
          case individualName ~ addresses =>
          form =>
            Json.obj(
              "addresses" -> transformAddressesForTemplate(addresses, countryOptions),
              "viewmodel" -> CommonViewModel(
                "individual",
                individualName.fullName,
                routes.IndividualAddressListController.onSubmit(mode).url,
                Some(routes.IndividualContactAddressController.onSubmit(mode).url))
            )
        }
    )
}
