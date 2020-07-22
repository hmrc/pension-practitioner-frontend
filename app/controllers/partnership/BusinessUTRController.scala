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
import forms.BusinessUTRFormProvider
import javax.inject.Inject
import models.NormalMode
import models.requests.DataRequest
import navigators.CompoundNavigator
import pages.partnership.BusinessUTRPage
import pages.register.BusinessTypePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport

import scala.concurrent.{ExecutionContext, Future}

class BusinessUTRController @Inject()(override val messagesApi: MessagesApi,
                                      userAnswersCacheConnector: UserAnswersCacheConnector,
                                      navigator: CompoundNavigator,
                                      identify: IdentifierAction,
                                      getData: DataRetrievalAction,
                                      requireData: DataRequiredAction,
                                      formProvider: BusinessUTRFormProvider,
                                      val controllerComponents: MessagesControllerComponents,
                                      config: FrontendAppConfig,
                                      renderer: Renderer
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with NunjucksSupport with Retrievals {

  protected def form
  (implicit request: DataRequest[AnyContent]): Form[String] = formProvider.apply

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      BusinessTypePage.retrieve.right.map { businessType =>
        val preparedForm = request.userAnswers.get(BusinessUTRPage) match {
          case None => form
          case Some(value) => form.fill(value)
        }

        val json = Json.obj(
          "form" -> preparedForm,
          "submitUrl" -> routes.BusinessUTRController.onSubmit().url,
          "businessType" -> s"whatTypeBusiness.$businessType"
        )

        renderer.render("businessUTR.njk", json).map(Ok(_))
      }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      BusinessTypePage.retrieve.right.map { businessType =>
        form.bindFromRequest().fold(
          formWithErrors => {

            val json = Json.obj(
              "form" -> formWithErrors,
              "submitUrl" -> routes.BusinessUTRController.onSubmit().url,
              "businessType" -> s"whatTypeBusiness.$businessType"
            )

            renderer.render("businessUTR.njk", json).map(BadRequest(_))
          },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(BusinessUTRPage, value))
              _ <- userAnswersCacheConnector.save(updatedAnswers.data)
            } yield Redirect(navigator.nextPage(BusinessUTRPage, NormalMode, updatedAnswers))
        )
      }
  }
}