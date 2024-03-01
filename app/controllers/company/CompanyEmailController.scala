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

package controllers.company

import connectors.cache.UserAnswersCacheConnector
import controllers.{Retrievals, Variation}
import controllers.actions._
import forms.EmailFormProvider
import javax.inject.Inject
import models.Mode
import models.requests.DataRequest
import navigators.CompoundNavigator
import pages.AddressChange
import pages.company.{BusinessNamePage, CompanyEmailPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.{Writes, Json, JsObject}
import play.api.mvc.{Result, AnyContent, MessagesControllerComponents, Action}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport
import viewmodels.CommonViewModel

import scala.concurrent.{Future, ExecutionContext}

class CompanyEmailController @Inject()(override val messagesApi: MessagesApi,
                                       userAnswersCacheConnector: UserAnswersCacheConnector,
                                       navigator: CompoundNavigator,
                                       authenticate: AuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       formProvider: EmailFormProvider,
                                       val controllerComponents: MessagesControllerComponents,
                                       renderer: Renderer
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController
  with Retrievals with I18nSupport with NunjucksSupport with Variation {

  private def form(implicit messages: Messages): Form[String] =
    formProvider(messages("email.error.required", messages("company")))

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        val formFilled = request.userAnswers.get(CompanyEmailPage).fold(form)(form.fill)
        getJson(mode, formFilled) { json =>
          renderer.render("email.njk", json).map(Ok(_))
        }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        form.bindFromRequest().fold(
          formWithErrors =>
            getJson(mode, formWithErrors) { json =>
              renderer.render("email.njk", json).map(BadRequest(_))
            },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(CompanyEmailPage, value))
              answersWithChangeFlag <- Future.fromTry(setChangeFlag(updatedAnswers, AddressChange))
              _ <- userAnswersCacheConnector.save(answersWithChangeFlag.data)
            } yield Redirect(navigator.nextPage(CompanyEmailPage, mode, answersWithChangeFlag))
        )

    }

  private def getJson(mode: Mode, form: Form[String])(block: JsObject => Future[Result])
                     (implicit w: Writes[Form[String]], request: DataRequest[AnyContent]): Future[Result] =
    BusinessNamePage.retrieve.map { companyName =>
      val json = Json.obj(
        "form" -> Json.toJsFieldJsValueWrapper(form)(w),
        "viewmodel" -> CommonViewModel(
          "company",
          companyName,
          routes.CompanyEmailController.onSubmit(mode).url)
      )
      block(json)
    }
}
