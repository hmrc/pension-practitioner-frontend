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

import config.FrontendAppConfig
import connectors.AddressLookupConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.actions._
import forms.FormsHelper.formWithError
import forms.address.PostcodeFormProvider
import javax.inject.Inject
import models.Mode
import models.requests.DataRequest
import navigators.CompoundNavigator
import pages.company.CompanyPostcodePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport

import scala.concurrent.{ExecutionContext, Future}

class CompanyAddressController @Inject()(override val messagesApi: MessagesApi,
                                         userAnswersCacheConnector: UserAnswersCacheConnector,
                                         navigator: CompoundNavigator,
                                         identify: IdentifierAction,
                                         getData: DataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         formProvider: PostcodeFormProvider,
                                         val controllerComponents: MessagesControllerComponents,
                                         config: FrontendAppConfig,
                                         renderer: Renderer
                                                         )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with NunjucksSupport {

  private def form(implicit messages: Messages): Form[String] =
    formProvider(
      messages("company.postcode.error.required", messages("company")),
      messages("company.postcode.error.invalid", messages("company"))
    )

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async {
      implicit request =>
          renderer.render("address/postcode.njk", Json.obj()).map(Ok(_))

    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async {
      implicit request =>

          form.bindFromRequest().fold(
            formWithErrors => {

                renderer.render("address/postcode.njk", Json.obj()).map(BadRequest(_))
            },
            value =>

                  for {
                    updatedAnswers <- Future.fromTry(request.userAnswers.set(CompanyPostcodePage.path, Json.obj()))
                    _ <- userAnswersCacheConnector.save(updatedAnswers.data)
                  } yield Redirect(navigator.nextPage(CompanyPostcodePage, mode, updatedAnswers))

          )

    }
}
