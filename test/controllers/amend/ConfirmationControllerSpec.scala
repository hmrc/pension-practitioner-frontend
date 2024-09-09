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

package controllers.amend

import controllers.actions.MutableFakeDataRetrievalAction
import controllers.base.ControllerSpecBase
import models.UserAnswers
import models.WhatTypeBusiness.Companyorpartnership
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import pages.company.CompanyEmailPage
import pages.{PspIdPage, WhatTypeBusinessPage}
import play.api.Application
import play.api.mvc.Results.Ok
import play.api.test.Helpers._
import play.twirl.api.Html

import scala.concurrent.Future

class ConfirmationControllerSpec extends ControllerSpecBase {

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()

  override def fakeApplication(): Application =
    applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()
  private val pspId = "1234567890"
  private val email = "a@a.c"

  val userAnswers: UserAnswers = UserAnswers()
    .setOrException(WhatTypeBusinessPage, Companyorpartnership)
    .setOrException(CompanyEmailPage, email)
    .setOrException(PspIdPage, pspId)

  private def onPageLoadUrl: String = routes.ConfirmationController.onPageLoad().url
  private def submitUrl: String = "testUrl2"


  override def beforeEach(): Unit = {
    super.beforeEach()
    mutableFakeDataRetrievalAction.setDataToReturn(Some(userAnswers))
    when(mockAppConfig.returnToPspDashboardUrl).thenReturn(submitUrl)
  }

  "Confirmation Controller" must {
    "return OK and the correct view for a GET" in {
      when(mockUserAnswersCacheConnector.removeAll(any(), any())).thenReturn(Future.successful(Ok))

      val req = httpGETRequest(onPageLoadUrl)
      val result = route(app, req).value

      status(result) mustEqual OK
      val view = app.injector.instanceOf[views.html.amend.ConfirmationView].apply(
        email,
        Html(s"""<p>${{ messages("confirmation.psp.id") }}</p>
                |<span class="heading-large govuk-!-font-weight-bold">$pspId</span>""".stripMargin).toString(),
        submitUrl
      )(req, messages)

      compareResultAndView(result, view)
    }

    "redirect to Session Expired page for a GET when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(app, httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
    }
  }
}
