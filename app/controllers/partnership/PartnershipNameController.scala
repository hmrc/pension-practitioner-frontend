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
import controllers.Variation
import controllers.actions._
import forms.BusinessNameFormProvider

import javax.inject.Inject
import models.Mode
import navigators.CompoundNavigator
import pages.NameChange
import pages.partnership.BusinessNamePage
import pages.register.AreYouUKCompanyPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.TwirlMigration
import views.html.BusinessNameView

import scala.concurrent.{ExecutionContext, Future}

class PartnershipNameController @Inject()(override val messagesApi: MessagesApi,
                                          userAnswersCacheConnector: UserAnswersCacheConnector,
                                          navigator: CompoundNavigator,
                                          authenticate: AuthAction,
                                          getData: DataRetrievalAction,
                                          requireData: DataRequiredAction,
                                          formProvider: BusinessNameFormProvider,
                                          val controllerComponents: MessagesControllerComponents,
                                          config: FrontendAppConfig,
                                          renderer: Renderer,
                                          businessNameView: BusinessNameView,
                                          twirlMigration: TwirlMigration
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController
                                        with I18nSupport with NunjucksSupport with Variation {

  private def form: Form[String] = formProvider("partnershipName.error.required", "partnershipName.error.invalid", "partnershipName.error.length")

  def onPageLoad(mode: Mode): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
        val preparedForm = request.userAnswers.get(BusinessNamePage) match {
          case None => form
          case Some(value) => form.fill(value)
        }

        val hint = request.userAnswers.get(AreYouUKCompanyPage) match {
          case Some(true) => Some("businessName.hint")
          case _ => None
        }

        val extraJson = hint.map { hint => Json.obj("hintMessageKey" -> hint)}.getOrElse(Json.obj())

        val json = Json.obj(
          "form" -> preparedForm,
          "submitUrl" -> routes.PartnershipNameController.onSubmit(mode).url,
          "entityName" -> "partnership"
        ) ++ extraJson

        val template = twirlMigration.duoTemplate(
          renderer.render("businessName.njk", json),
          businessNameView("partnership", preparedForm, routes.PartnershipNameController.onSubmit(mode), hint)
        )

        template.map(Ok(_))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
        form.bindFromRequest().fold(
          formWithErrors => {

            val json = Json.obj(
              "form" -> formWithErrors,
              "submitUrl" -> routes.PartnershipNameController.onSubmit(mode).url,
              "entityName" -> "partnership"
            )

            val template = twirlMigration.duoTemplate(
              renderer.render("businessName.njk", json),
              businessNameView("partnership", formWithErrors, routes.PartnershipNameController.onSubmit(mode), None)
            )

            template.map(BadRequest(_))
          },
          value =>
            for {
              ua <- Future.fromTry(request.userAnswers.set(BusinessNamePage, value))
              updatedAnswers <- Future.fromTry(setChangeFlag(ua, NameChange))
              _ <- userAnswersCacheConnector.save( updatedAnswers.data)
            } yield Redirect(navigator.nextPage(BusinessNamePage, mode, updatedAnswers))
        )
  }
}
