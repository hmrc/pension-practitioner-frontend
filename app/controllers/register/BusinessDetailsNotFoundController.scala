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
import controllers.actions._
import javax.inject.Inject
import models.NormalMode
import navigators.CompoundNavigator
import pages.register.BusinessDetailsNotFoundPage
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController

import scala.concurrent.ExecutionContext

class BusinessDetailsNotFoundController @Inject()(
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  navigator: CompoundNavigator,
  config: FrontendAppConfig,
    val controllerComponents: MessagesControllerComponents,
    renderer: Renderer
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val enterDetailsUrl = navigator.nextPage(BusinessDetailsNotFoundPage, NormalMode, request.userAnswers).url
      val json = Json.obj(
        "companiesHouseUrl" -> config.companiesHouseFileChangesUrl,
        "hmrcUrl" -> config.hmrcChangesMustReportUrl,
        "hmrcTaxHelplineUrl" -> config.hmrcTaxHelplineUrl,
        "enterDetailsAgainUrl" -> enterDetailsUrl
      )
      renderer.render("register/businessDetailsNotFound.njk", json).map(Ok(_))
  }
}
