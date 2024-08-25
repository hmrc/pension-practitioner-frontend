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

import connectors.AddressLookupConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import controllers.address.PostcodeController
import controllers.company.routes
import forms.address.PostcodeFormProvider

import javax.inject.Inject
import models.Mode
import navigators.CompoundNavigator
import pages.company.{BusinessNamePage, CompanyPostcodePage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.viewmodels.NunjucksSupport
import viewmodels.CommonViewModel
import views.html.address.PostcodeView

import scala.concurrent.ExecutionContext

class CompanyPostcodeController @Inject()(override val messagesApi: MessagesApi,
                                          val userAnswersCacheConnector: UserAnswersCacheConnector,
                                          val navigator: CompoundNavigator,
                                          authenticate: AuthAction,
                                          getData: DataRetrievalAction,
                                          requireData: DataRequiredAction,
                                          formProvider: PostcodeFormProvider,
                                          val addressLookupConnector: AddressLookupConnector,
                                          val controllerComponents: MessagesControllerComponents,
                                          val renderer: Renderer,
                                          postCodeView: PostcodeView
                                         )(implicit ec: ExecutionContext) extends PostcodeController
                                          with Retrievals with I18nSupport with NunjucksSupport {

  def form(implicit messages: Messages): Form[String] =
    formProvider(
      messages("postcode.error.required", messages("company")),
      messages("postcode.error.invalid", messages("company"))
    )

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        getFormToJson(mode).retrieve.map {func =>
          val jsObject: JsObject = func(form)
        get(func, Some(postCodeView(
          routes.CompanyPostcodeController.onSubmit(mode),
          routes.CompanyContactAddressController.onPageLoad(mode).url,
          "company",
          (jsObject \ "viewmodel" \ "entityName").asOpt[String].getOrElse(""),
          form
        )))}
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        getFormToJson(mode).retrieve.map { formFunc =>
          val jsObject = formFunc(form)
          val twirlTemplate = Some(postCodeView(
            routes.CompanyPostcodeController.onSubmit(mode),
            routes.CompanyContactAddressController.onPageLoad(mode).url,
            "company",
            (jsObject \ "viewmodel" \ "entityName").asOpt[String].getOrElse(""),
            _
          ))
          post(mode, formFunc, CompanyPostcodePage, Messages("error.postcode.noResults"), twirlTemplate)
        }
    }

  def getFormToJson(mode: Mode): Retrieval[Form[String] => JsObject] =
    Retrieval(
      implicit request =>
        BusinessNamePage.retrieve.map { companyName =>
            form => Json.obj(
              "form" -> form,
              "viewmodel" -> CommonViewModel(
                "company",
                companyName,
                routes.CompanyPostcodeController.onSubmit(mode).url,
                Some(routes.CompanyContactAddressController.onPageLoad(mode).url)
              ))
        }
    )
}
