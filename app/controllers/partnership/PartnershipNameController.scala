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
import controllers.Variation
import controllers.actions._
import forms.BusinessNameFormProvider
import javax.inject.Inject
import models.{Mode, NormalMode}
import navigators.CompoundNavigator
import pages.NameChange
import pages.partnership.BusinessNamePage
import pages.register.AreYouUKCompanyPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, MessagesControllerComponents, Action}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.annotations.AuthWithIVNoEnrolment

import scala.concurrent.{Future, ExecutionContext}

class PartnershipNameController @Inject()(override val messagesApi: MessagesApi,
                                          userAnswersCacheConnector: UserAnswersCacheConnector,
                                          navigator: CompoundNavigator,
                                          @AuthWithIVNoEnrolment authenticate: AuthAction,
                                          getData: DataRetrievalAction,
                                          requireData: DataRequiredAction,
                                          formProvider: BusinessNameFormProvider,
                                          val controllerComponents: MessagesControllerComponents,
                                          config: FrontendAppConfig,
                                          renderer: Renderer
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController
                                        with I18nSupport with NunjucksSupport with Variation {

  private def form: Form[String] = formProvider("partnershipName.error.required", "partnershipName.error.invalid", "partnershipName.error.length")

  def onPageLoad(mode: Mode): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
        val preparedForm = request.userAnswers.get(BusinessNamePage) match {
          case None => form
          case Some(value) => form.fill(value)
        }

      val extraJson = request.userAnswers.get(AreYouUKCompanyPage) match {
        case Some(true) => Json.obj("hintMessageKey" -> "businessName.hint")
        case _ => Json.obj()
      }

        val json = Json.obj(
          "form" -> preparedForm,
          "submitUrl" -> routes.PartnershipNameController.onSubmit(mode).url,
          "entityName" -> "partnership"
        ) ++ extraJson

        renderer.render("businessName.njk", json).map(Ok(_))
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

            renderer.render("businessName.njk", json).map(BadRequest(_))
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
