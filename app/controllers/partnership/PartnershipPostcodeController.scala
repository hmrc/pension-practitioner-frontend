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

package controllers.partnership

import connectors.AddressLookupConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import controllers.address.PostcodeController
import forms.address.PostcodeFormProvider
import models.Mode
import navigators.CompoundNavigator
import pages.partnership.{BusinessNamePage, PartnershipPostcodePage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import viewmodels.CommonViewModel
import views.html.address.PostcodeView

import javax.inject.Inject
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
                                              postCodeView: PostcodeView
                                         )(implicit ec: ExecutionContext) extends PostcodeController
                                          with Retrievals with I18nSupport {

  def form(implicit messages: Messages): Form[String] =
    formProvider(
      messages("postcode.error.required", messages("partnership")),
      messages("postcode.error.invalid", messages("partnership"))
    )

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        getFormToJson(mode).retrieve.map{ formFunction =>
          val  jsObject: JsObject = formFunction(form)
          get(postCodeView(
          routes.PartnershipPostcodeController.onSubmit(mode),
            routes.PartnershipContactAddressController.onPageLoad(mode).url,
          "partnership",
           (jsObject \ "viewmodel" \ "entityName").asOpt[String].getOrElse(""),
          form
        ))}
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>

        getFormToJson(mode).retrieve.map{formFunction =>
          val  jsObject: JsObject = formFunction(form)
          val twirlTemplate = postCodeView(
            routes.PartnershipPostcodeController.onSubmit(mode),
            routes.PartnershipContactAddressController.onPageLoad(mode).url,
            "partnersihp",
             (jsObject \ "viewmodel" \ "entityName").asOpt[String].getOrElse(""),
            _
          )
          post(mode, PartnershipPostcodePage, "error.postcode.noResults", twirlTemplate)
        }
    }

  def getFormToJson(mode: Mode): Retrieval[Form[String] => JsObject] =
    Retrieval(
      implicit request =>
        BusinessNamePage.retrieve.map { partnershipName =>
            form => Json.obj(
              "viewmodel" -> CommonViewModel(
                "partnership",
                partnershipName,
                routes.PartnershipPostcodeController.onSubmit(mode).url,
                Some(routes.PartnershipContactAddressController.onPageLoad(mode).url)
              ))
        }
    )
}
