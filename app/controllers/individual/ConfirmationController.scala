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

package controllers.individual

import controllers.Retrievals
import controllers.actions._
import javax.inject.Inject
import models.WhatTypeBusiness.Companyorpartnership
import models.requests.DataRequest
import pages.WhatTypeBusinessPage
import pages.individual.IndividualDetailsPage
import pages.individual.IndividualEmailPage
import pages.register.company.CompanyNamePage
import play.api.i18n.I18nSupport
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.twirl.api.Html
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport
import viewmodels.CommonViewModel

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class ConfirmationController @Inject()(override val messagesApi: MessagesApi,
                                       identify: IdentifierAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       renderer: Renderer
                                         )(implicit ec: ExecutionContext) extends FrontendBaseController
  with Retrievals with I18nSupport with NunjucksSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      request.userAnswers.get(IndividualEmailPage) match {
        case Some(email) =>
          val json: JsObject = Json.obj(
            "panelHtml" -> confirmationPanelText("1234567890").toString(),
            "email" -> email,
            "submitUrl" -> controllers.routes.SignOutController.signOut().url
          )

          renderer.render("individual/confirmation.njk", json).map(Ok(_))
        case _ => Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
      }
  }

  private def confirmationPanelText(pspId: String)(implicit messages: Messages): Html = {
    Html(s"""<p>${{ messages("confirmation.psp.id") }}</p>
         |<span class="heading-large govuk-!-font-weight-bold">$pspId</span>""".stripMargin)
  }

}
