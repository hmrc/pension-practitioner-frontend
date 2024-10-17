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

import audit.AuditService
import controllers.base.ControllerSpecBase
import data.SampleData._
import forms.WhatTypeBusinessFormProvider
import models.{UserAnswers, WhatTypeBusiness}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.TryValues
import pages.{PspIdPage, WhatTypeBusinessPage}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.WhatTypeBusinessView

import scala.concurrent.Future

class WhatTypeBusinessControllerSpec extends ControllerSpecBase with TryValues {

  private def onwardRoute = Call("GET", "/foo")

  private def whatTypeBusinessRoute: Call = routes.WhatTypeBusinessController.onPageLoad()
  private def whatTypeBusinessSubmitRoute: Call = routes.WhatTypeBusinessController.onSubmit()

  private val formProvider = new WhatTypeBusinessFormProvider()
  private val form = formProvider()

  private val mockAuditService = mock[AuditService]

  private val extraModules: Seq[GuiceableModule] = Seq(
    bind[AuditService].toInstance(mockAuditService)
  )

  private def buildApp(userAnswers:Option[UserAnswers]): Application =
    applicationBuilder(userAnswers,
      extraModules = extraModules).build()

  override def beforeEach(): Unit = {
    reset(mockAuditService)
    doNothing().when(mockAuditService).sendEvent(any())(any(), any())
    super.beforeEach()
  }

  val answers: UserAnswers = userAnswersWithCompanyName.set(WhatTypeBusinessPage, WhatTypeBusiness.values.head).success.value

  "WhatTypeBusiness Controller" must {
    "return OK and the correct view for a GET" in {
      val application = buildApp(userAnswers = Some(userAnswersWithCompanyName))
      val request = FakeRequest(GET, whatTypeBusinessRoute.url)

      val result = route(application, request).value

      status(result) mustEqual OK

      val view = application.injector.instanceOf[WhatTypeBusinessView].apply(whatTypeBusinessSubmitRoute,
        form,
        WhatTypeBusiness.radios(form))(request, messages)

      compareResultAndView(result, view)

      application.stop()
    }

    "populate the view correctly on a GET when the question has previously been answered" in {

      val application = buildApp(userAnswers = Some(answers))

      val request = FakeRequest(GET, whatTypeBusinessRoute.url)

      val result = route(application, request).value

      status(result) mustEqual OK

      val filledForm = form.bind(Map("value" -> WhatTypeBusiness.values.head.toString))

      val view = application.injector.instanceOf[WhatTypeBusinessView].apply(whatTypeBusinessSubmitRoute,
        filledForm,
        WhatTypeBusiness.radios(filledForm))(request, messages)

      compareResultAndView(result, view)

      application.stop()
    }

    "redirect to the next page when valid data is submitted" in {
      when(mockUserAnswersCacheConnector.save(any())(any(), any())) thenReturn Future.successful(Json.obj())
      when(mockCompoundNavigator.nextPage(any(), any(), any())).thenReturn(onwardRoute)

      val pspId = "test-id"

      val application = buildApp(userAnswers = Some(userAnswersWithCompanyName.setOrException(PspIdPage, pspId)))

      val request = FakeRequest(POST, whatTypeBusinessRoute.url).withFormUrlEncodedBody(("value", WhatTypeBusiness.values.head.toString))

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual onwardRoute.url

      application.stop()
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val application = buildApp(userAnswers = Some(userAnswersWithCompanyName))
      val request = FakeRequest(POST, whatTypeBusinessRoute.url).withFormUrlEncodedBody(("value", "invalid value"))
      val boundForm = form.bind(Map("value" -> "invalid value"))

      val result = route(application, request).value

      status(result) mustEqual BAD_REQUEST

      val view = application.injector.instanceOf[WhatTypeBusinessView].apply(whatTypeBusinessSubmitRoute,
        boundForm,
        WhatTypeBusiness.radios(boundForm))(request, messages)

      compareResultAndView(result, view)

      application.stop()
    }
  }
}
