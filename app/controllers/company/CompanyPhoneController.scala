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
import controllers.actions._
import controllers.{Retrievals, Variation}
import forms.PhoneFormProvider
import models.Mode
import models.requests.DataRequest
import navigators.CompoundNavigator
import pages.AddressChange
import pages.company.{BusinessNamePage, CompanyPhonePage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.TwirlMigration
import viewmodels.CommonViewModelTwirl
import views.html.PhoneView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CompanyPhoneController @Inject()(override val messagesApi: MessagesApi,
                                       userAnswersCacheConnector: UserAnswersCacheConnector,
                                       navigator: CompoundNavigator,
                                       authenticate: AuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       formProvider: PhoneFormProvider,
                                       val controllerComponents: MessagesControllerComponents,
                                       renderer: Renderer,
                                       phoneView: PhoneView,
                                       twirlMigration: TwirlMigration
                                         )(implicit ec: ExecutionContext) extends FrontendBaseController
  with Retrievals with I18nSupport with NunjucksSupport with Variation {

  private def form(implicit messages: Messages): Form[String] =
    formProvider(messages("phone.error.required", messages("company")))

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        val formFilled = request.userAnswers.get(CompanyPhonePage).fold(form)(form.fill)
        getModel(mode) { model =>
          val template = twirlMigration.duoTemplate(
            renderer.render("phone.njk", TwirlMigration.nunjucksGetJson(formFilled, model.toNunjucks)),
            phoneView(model, formFilled)
          )

          template.map(Ok(_))
        }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        form.bindFromRequest().fold(
          formWithErrors =>
            getModel(mode) { model =>
              val template = twirlMigration.duoTemplate(
                renderer.render("phone.njk", TwirlMigration.nunjucksGetJson(formWithErrors, model.toNunjucks)),
                phoneView(model, formWithErrors)
              )

              template.map(BadRequest(_))
            },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(CompanyPhonePage, value))
              answersWithChangeFlag <- Future.fromTry(setChangeFlag(updatedAnswers, AddressChange))
              _ <- userAnswersCacheConnector.save(answersWithChangeFlag.data)
            } yield Redirect(navigator.nextPage(CompanyPhonePage, mode, answersWithChangeFlag))
        )

    }


  private def getModel(mode: Mode)(block: CommonViewModelTwirl => Future[Result])(implicit request: DataRequest[AnyContent]) = {
    BusinessNamePage.retrieve match {
      case Left(errorResult) => errorResult
      case Right(companyName) => block(
        CommonViewModelTwirl(
          "company",
          companyName,
          routes.CompanyPhoneController.onSubmit(mode)
        )
      )
    }
  }
}
