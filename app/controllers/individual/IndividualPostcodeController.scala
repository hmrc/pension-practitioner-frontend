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

import connectors.AddressLookupConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import controllers.address.PostcodeController
import forms.address.PostcodeFormProvider
import models.Mode
import navigators.CompoundNavigator
import pages.individual.IndividualPostcodePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.annotations.AuthWithIV
import views.html.individual.PostcodeView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class IndividualPostcodeController @Inject()(override val messagesApi: MessagesApi,
                                             val userAnswersCacheConnector: UserAnswersCacheConnector,
                                             val navigator: CompoundNavigator,
                                             @AuthWithIV
                                             authenticate: AuthAction,
                                             getData: DataRetrievalAction,
                                             requireData: DataRequiredAction,
                                             formProvider: PostcodeFormProvider,
                                             val addressLookupConnector: AddressLookupConnector,
                                             val controllerComponents: MessagesControllerComponents,
                                             postCodeView: PostcodeView
                                            )(implicit ec: ExecutionContext) extends PostcodeController
  with Retrievals with I18nSupport {

  def form(implicit messages: Messages): Form[String] =
    formProvider(
      messages("individual.postcode.error.required"),
      messages("individual.postcode.error.invalid")
    )


  def onPageLoad(mode: Mode): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      get(postCodeView(
        routes.IndividualPostcodeController.onSubmit(mode),
        routes.IndividualContactAddressController.onPageLoad(mode).url,
        form
      ))
  }


  def onSubmit(mode: Mode): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      val twirlTemplate = postCodeView(
        routes.IndividualPostcodeController.onSubmit(mode),
        routes.IndividualContactAddressController.onPageLoad(mode).url,
        _
      )
      post(mode, IndividualPostcodePage, "error.postcode.noResults", twirlTemplate)
  }
}
