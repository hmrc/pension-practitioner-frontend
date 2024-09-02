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

import config.FrontendAppConfig
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.partnership.IsPartnershipRegisteredInUkFormProvider
import models.NormalMode
import navigators.CompoundNavigator
import pages.partnership.IsPartnershipRegisteredInUkPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.{NunjucksSupport, Radios}
import utils.annotations.AuthMustHaveNoEnrolmentWithNoIV
import utils.TwirlMigration

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import views.html.partnership.IsPartnershipRegisteredInUkView

class IsPartnershipRegisteredInUkController @Inject()(override val messagesApi: MessagesApi,
                                      userAnswersCacheConnector: UserAnswersCacheConnector,
                                      navigator: CompoundNavigator,
                                      @AuthMustHaveNoEnrolmentWithNoIV authenticate: AuthAction,
                                      getData: DataRetrievalAction,
                                      requireData: DataRequiredAction,
                                      formProvider: IsPartnershipRegisteredInUkFormProvider,
                                      val controllerComponents: MessagesControllerComponents,
                                      config: FrontendAppConfig,
                                      renderer: Renderer,
                                      isPartnershipRegisteredInUkView: IsPartnershipRegisteredInUkView,
                                      twirlMigration: TwirlMigration
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with
  I18nSupport with NunjucksSupport with Retrievals {

  private val form = formProvider()

  def onPageLoad(): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      val preparedForm = request.userAnswers.get (IsPartnershipRegisteredInUkPage) match {
        case None => form
        case Some (value) => form.fill (value)
      }

      val json = Json.obj(
        "form" -> preparedForm,
        "submitUrl" -> routes.IsPartnershipRegisteredInUkController.onSubmit().url,
        "radios" -> Radios.yesNo (preparedForm("value"))
      )

      val template = twirlMigration.duoTemplate(
        renderer.render(
          "partnership/isPartnershipRegisteredInUk.njk", json
        ),
        isPartnershipRegisteredInUkView(
          routes.IsPartnershipRegisteredInUkController.onSubmit(),
          preparedForm,
          TwirlMigration.toTwirlRadios(Radios.yesNo (preparedForm("value")))
        )
      )
    template.map(Ok(_))
  }

  def onSubmit(): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => {

          val json = Json.obj(
            "form"   -> formWithErrors,
            "submitUrl"   -> routes.IsPartnershipRegisteredInUkController.onSubmit().url,
            "radios" -> Radios.yesNo(formWithErrors("value"))
          )

          val template = twirlMigration.duoTemplate(
            renderer.render(
              "partnership/isPartnershipRegisteredInUk.njk", json
            ),
            isPartnershipRegisteredInUkView(
              routes.IsPartnershipRegisteredInUkController.onSubmit(),
              formWithErrors,
              TwirlMigration.toTwirlRadios(Radios.yesNo (formWithErrors("value")))
            )
          )
          template.map(BadRequest(_))
        },
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(IsPartnershipRegisteredInUkPage, value))
            _ <- userAnswersCacheConnector.save( updatedAnswers.data)
          } yield Redirect(navigator.nextPage(IsPartnershipRegisteredInUkPage, NormalMode, updatedAnswers))
      )
  }
}
