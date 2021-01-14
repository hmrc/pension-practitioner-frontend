/*
 * Copyright 2021 HM Revenue & Customs
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

import connectors.AddressLookupConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import controllers.address.PostcodeController
import forms.address.PostcodeFormProvider
import javax.inject.Inject
import models.Mode
import models.requests.DataRequest
import navigators.CompoundNavigator
import pages.partnership.{PartnershipPostcodePage, BusinessNamePage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.{Json, JsObject}
import play.api.mvc.{AnyContent, MessagesControllerComponents, Action}
import renderer.Renderer
import uk.gov.hmrc.viewmodels.NunjucksSupport
import viewmodels.CommonViewModel

import scala.concurrent.ExecutionContext

class PartnershipPostcodeController @Inject()(override val messagesApi: MessagesApi,
                                              val userAnswersCacheConnector: UserAnswersCacheConnector,
                                              val navigator: CompoundNavigator,
                                              authenticate: AuthAction,
                                              getData: DataRetrievalAction,
                                              requireData: DataRequiredAction,
                                              formProvider: PostcodeFormProvider,
                                              val addressLookupConnector: AddressLookupConnector,
                                              val controllerComponents: MessagesControllerComponents,
                                              val renderer: Renderer
                                         )(implicit ec: ExecutionContext) extends PostcodeController
                                          with Retrievals with I18nSupport with NunjucksSupport {

  def form(implicit messages: Messages): Form[String] =
    formProvider(
      messages("postcode.error.required", messages("partnership")),
      messages("postcode.error.invalid", messages("partnership"))
    )

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        getFormToJson(mode).retrieve.right.map(get)
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        getFormToJson(mode).retrieve.right.map(
          post(mode, _, PartnershipPostcodePage, "partnership.postcode.error.invalid")
        )
    }

  def getFormToJson(mode: Mode)(implicit request: DataRequest[AnyContent]): Retrieval[Form[String] => JsObject] =
    Retrieval(
      implicit request =>
        BusinessNamePage.retrieve.right.map { partnershipName =>
            form => Json.obj(
              "form" -> form,
              "viewmodel" -> CommonViewModel(
                "partnership",
                partnershipName,
                routes.PartnershipPostcodeController.onSubmit(mode).url,
                Some(routes.PartnershipContactAddressController.onPageLoad(mode).url)
              ))
        }
    )
}
