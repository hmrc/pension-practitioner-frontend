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

package controllers.register

import config.FrontendAppConfig
import connectors.cache.UserAnswersCacheConnector
import controllers.actions._
import forms.register.AreYouUKCompanyFormProvider
import javax.inject.Inject
import models.NormalMode
import navigators.CompoundNavigator
import pages.register.AreYouUKCompanyPage
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport
import uk.gov.hmrc.viewmodels.Radios
import utils.annotations.AuthWithIVNoEnrolment

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class AreYouUKCompanyController @Inject()(override val messagesApi: MessagesApi,
                                      userAnswersCacheConnector: UserAnswersCacheConnector,
                                      navigator: CompoundNavigator,
                                      @AuthWithIVNoEnrolment authenticate: AuthAction,
                                      getData: DataRetrievalAction,
                                      requireData: DataRequiredAction,
                                      formProvider: AreYouUKCompanyFormProvider,
                                      val controllerComponents: MessagesControllerComponents,
                                      config: FrontendAppConfig,
                                      renderer: Renderer
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with NunjucksSupport {

  private val form = formProvider()

  def onPageLoad(): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
        val preparedForm = request.userAnswers.get(AreYouUKCompanyPage) match {
          case None => form
          case Some (value) => form.fill (value)
        }

        val json = Json.obj(
          "form" -> preparedForm,
          "submitUrl" -> routes.AreYouUKCompanyController.onSubmit().url,
          "radios" -> Radios.yesNo (preparedForm("value"))
        )

      renderer.render ("register/areYouUKCompany.njk", json).map(Ok (_))
  }

  def onSubmit(): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
        form.bindFromRequest().fold(
          formWithErrors => {

            val json = Json.obj(
              "form"   -> formWithErrors,
              "submitUrl"   -> routes.AreYouUKCompanyController.onSubmit().url,
              "radios" -> Radios.yesNo(formWithErrors("value"))
            )

            renderer.render("register/areYouUKCompany.njk", json).map(BadRequest(_))
          },
          value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(AreYouUKCompanyPage, value))
                _ <- userAnswersCacheConnector.save( updatedAnswers.data)
              } yield Redirect(navigator.nextPage(AreYouUKCompanyPage, NormalMode, updatedAnswers))
        )
  }
}
