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

package controllers

import audit.{AuditService, PSPStartEvent}
import connectors.cache.UserAnswersCacheConnector
import controllers.actions._
import forms.WhatTypeBusinessFormProvider
import models.{NormalMode, UserAnswers, WhatTypeBusiness}
import navigators.CompoundNavigator
import pages.WhatTypeBusinessPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.TwirlMigration
import utils.annotations.AuthMustHaveNoEnrolmentWithNoIV
import views.html.WhatTypeBusinessView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class WhatTypeBusinessController @Inject()(override val messagesApi: MessagesApi,
                                           userAnswersCacheConnector: UserAnswersCacheConnector,
                                           navigator: CompoundNavigator,
                                           @AuthMustHaveNoEnrolmentWithNoIV authenticate: AuthAction,
                                           getData: DataRetrievalAction,
                                           formProvider: WhatTypeBusinessFormProvider,
                                           val controllerComponents: MessagesControllerComponents,
                                           renderer: Renderer,
                                           auditService: AuditService,
                                           whatTypeBusinessView: WhatTypeBusinessView
                                          )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with NunjucksSupport {

  private val form = formProvider()

  def onPageLoad(): Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>


      val preparedForm = request.userAnswers.flatMap(_.get(WhatTypeBusinessPage)) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      val json = Json.obj(
        "form" -> preparedForm,
        "submitUrl" -> routes.WhatTypeBusinessController.onSubmit().url,
        "radios" -> WhatTypeBusiness.radios(preparedForm)
      )

      val template = TwirlMigration.duoTemplate(
        renderer.render("whatTypeBusiness.njk", json),
        whatTypeBusinessView(
          routes.WhatTypeBusinessController.onSubmit(),
          preparedForm,
          TwirlMigration.toTwirlRadios(WhatTypeBusiness.radios(preparedForm))
        )
      )
    template.map(Ok(_))
  }

  def onSubmit(): Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>
        form.bindFromRequest().fold(
          formWithErrors => {

            val json = Json.obj(
              "form" -> formWithErrors,
              "submitUrl" -> routes.WhatTypeBusinessController.onSubmit().url,
              "radios" -> WhatTypeBusiness.radios(formWithErrors)
            )

            renderer.render("whatTypeBusiness.njk", json).map(BadRequest(_))
          },
          value => {
            val ua = request.userAnswers.getOrElse(UserAnswers())
            for {
              updatedAnswers <- Future.fromTry(ua.set(WhatTypeBusinessPage, value))
              _ <- userAnswersCacheConnector.save( updatedAnswers.data)
            } yield {

              auditService.sendEvent(PSPStartEvent(request.user.userType, request.user.isExistingPSP))
              Redirect(navigator.nextPage(WhatTypeBusinessPage, NormalMode, updatedAnswers))
            }
          }
        )
  }
}
