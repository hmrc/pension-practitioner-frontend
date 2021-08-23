/*
 * Copyright 2021 HM Revenue & Customs
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

import controllers.actions.MutableFakeDataRetrievalAction
import controllers.base.ControllerSpecBase
import matchers.JsonMatchers
import models.UserAnswers
import models.WhatTypeBusiness.Companyorpartnership
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.{PspIdPage, WhatTypeBusinessPage}
import pages.company.{BusinessNamePage, CompanyEmailPage}
import play.api.Application
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.viewmodels.NunjucksSupport
import viewmodels.CommonViewModel

import scala.concurrent.Future
import play.api.mvc.Results.Ok

class ConfirmationControllerSpec extends ControllerSpecBase with MockitoSugar with NunjucksSupport
  with JsonMatchers with OptionValues with TryValues {

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val companyName: String = "Company name"
  private val application: Application =
    applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()
  private val templateToBeRendered = "register/confirmation.njk"
  private val pspId = "1234567890"
  private val email = "a@a.c"

  val userAnswers: UserAnswers = UserAnswers()
    .setOrException(WhatTypeBusinessPage, Companyorpartnership)
    .setOrException(CompanyEmailPage, email)
    .setOrException(BusinessNamePage, companyName)
    .setOrException(PspIdPage, pspId)

  private def onPageLoadUrl: String = routes.ConfirmationController.onPageLoad().url
  private def submitUrl: String = controllers.routes.SignOutController.signOut().url

  private val jsonToPassToTemplate: JsObject =
    Json.obj("viewmodel" -> CommonViewModel("company.capitalised", companyName, submitUrl),
      "email" -> email,
    "panelHtml" -> Html(s"""<p>${{ messages("confirmation.psp.id") }}</p>
                           |<span class="heading-large govuk-!-font-weight-bold">$pspId</span>""".stripMargin).toString()
    )


  override def beforeEach: Unit = {
    super.beforeEach
    mutableFakeDataRetrievalAction.setDataToReturn(Some(userAnswers))
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
  }

  "Confirmation Controller" must {
    "return OK and the correct view for a GET" in {
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])
      when(mockUserAnswersCacheConnector.removeAll(any(), any())).thenReturn(Future.successful(Ok))
      val result = route(application, httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered
      jsonCaptor.getValue must containJson(jsonToPassToTemplate)
    }

    "redirect to Session Expired page for a GET when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(application, httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
    }
  }
}
