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

package controllers.address

import connectors.AddressLookupConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import forms.FormsHelper.formWithError
import models.requests.DataRequest
import models.{Mode, TolerantAddress}
import navigators.CompoundNavigator
import pages.QuestionPage
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Result}
import play.twirl.api.Html
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

trait PostcodeController extends FrontendBaseController with Retrievals {

  protected def userAnswersCacheConnector: UserAnswersCacheConnector
  protected def navigator: CompoundNavigator
  protected def form(implicit messages: Messages): Form[String]
  protected def addressLookupConnector: AddressLookupConnector

  def get(twirlTemplate: Html): Future[Result] = {
    Future.successful(Ok(twirlTemplate))
  }

  def post(mode: Mode, postcodePage: QuestionPage[Seq[TolerantAddress]],
           errorMessage: String, formToTemplate: Form[String] => Html)
          (implicit request: DataRequest[AnyContent], ec: ExecutionContext, hc: HeaderCarrier, messages: Messages): Future[Result] = {
    form.bindFromRequest().fold(
      formWithErrors =>
        Future.successful(BadRequest(formToTemplate(formWithErrors))),
      value =>
          addressLookupConnector.addressLookupByPostCode(value).flatMap {
            case Nil =>
              val formWithErrors = formWithError(form, errorMessage)
              Future.successful(BadRequest(formToTemplate(formWithErrors)))

            case addresses =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(postcodePage, addresses))
                _ <- userAnswersCacheConnector.save(updatedAnswers.data)
              } yield Redirect(navigator.nextPage(postcodePage, mode, updatedAnswers))
          }

    )
  }

}
