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

package controllers.register.company

import config.FrontendAppConfig
import connectors.RegistrationConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.DataRetrievals
import controllers.actions._
import forms.register.company.ConfirmAddressFormProvider
import javax.inject.Inject
import models.Mode
import models.NormalMode
import models.register.Organisation
import navigators.CompoundNavigator
import pages.register.company.{BusinessUTRPage, ConfirmAddressPage}
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Results.Redirect
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport
import uk.gov.hmrc.viewmodels.Radios

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class ConfirmAddressController @Inject()(override val messagesApi: MessagesApi,
                                      userAnswersCacheConnector: UserAnswersCacheConnector,
                                      navigator: CompoundNavigator,
                                      identify: IdentifierAction,
                                      getData: DataRetrievalAction,
                                      registrationConnector:RegistrationConnector,
                                      requireData: DataRequiredAction,
                                      formProvider: ConfirmAddressFormProvider,
                                      val controllerComponents: MessagesControllerComponents,
                                      config: FrontendAppConfig,
                                      renderer: Renderer
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with NunjucksSupport {

  private val form = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      DataRetrievals.retrieveCompanyName { pspName =>
        val preparedForm = request.userAnswers.get (ConfirmAddressPage) match {
          case None => form
          case Some (value) => form.fill (value)
        }
        request.userAnswers.get(BusinessUTRPage) match {
          case None => Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
          case Some (utr) =>
            val organisation = Organisation(pspName,)
            val address = registrationConnector.registerWithIdOrganisation(utr)
            val json = Json.obj(
              "form" -> preparedForm,
              "pspName" -> pspName,
              "submitUrl" -> routes.ConfirmAddressController.onSubmit().url,
              "radios" -> Radios.yesNo (preparedForm("value"))
            )

            renderer.render ("register/company/confirmAddress.njk", json).map(Ok (_))
        }

    }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      DataRetrievals.retrieveCompanyName { pspName =>
        form.bindFromRequest().fold(
          formWithErrors => {

            val json = Json.obj(
              "form"   -> formWithErrors,
              "pspName" -> pspName,
              "submitUrl"   -> routes.ConfirmAddressController.onSubmit().url,
              "radios" -> Radios.yesNo(formWithErrors("value"))
            )

            renderer.render("register/company/confirmAddress.njk", json).map(BadRequest(_))
          },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(ConfirmAddressPage, value))
              _ <- userAnswersCacheConnector.save( updatedAnswers.data)
            } yield Redirect(navigator.nextPage(ConfirmAddressPage, NormalMode, updatedAnswers))
        )
      }
  }
}
