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

package controllers

import config.FrontendAppConfig
import connectors.cache.UserAnswersCacheConnector
import controllers.actions.{DataRequiredAction, DataRequiredActionImpl, FakeIdentifierAction, IdentifierAction}
import controllers.base.ControllerSpecBase
import data.SampleData._
import forms.WhatTypeBusinessFormProvider
import matchers.JsonMatchers
import models.{UserAnswers, WhatTypeBusiness}
import navigators.CompoundNavigator
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.WhatTypeBusinessPage
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.nunjucks.NunjucksRenderer
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.annotations.AuthWithNoIV

import scala.concurrent.Future

class WhatTypeBusinessControllerSpec extends ControllerSpecBase with MockitoSugar with NunjucksSupport with JsonMatchers with OptionValues with TryValues {

  private def onwardRoute = Call("GET", "/foo")

  private def whatTypeBusinessRoute: String = routes.WhatTypeBusinessController.onPageLoad().url
  private def whatTypeBusinessSubmitRoute: String = routes.WhatTypeBusinessController.onSubmit().url

  private val formProvider = new WhatTypeBusinessFormProvider()
  private val form = formProvider()

  override def modules: Seq[GuiceableModule] = Seq(
    bind[DataRequiredAction].to[DataRequiredActionImpl],
    bind[IdentifierAction].qualifiedWith(classOf[AuthWithNoIV]).to[FakeIdentifierAction],
    bind[NunjucksRenderer].toInstance(mockRenderer),
    bind[FrontendAppConfig].toInstance(mockAppConfig),
    bind[UserAnswersCacheConnector].toInstance(mockUserAnswersCacheConnector),
    bind[CompoundNavigator].toInstance(mockCompoundNavigator)
  )

  private val answers: UserAnswers = userAnswersWithCompanyName.set(WhatTypeBusinessPage, WhatTypeBusiness.values.head).success.value

  "WhatTypeBusiness Controller" must {
    "return OK and the correct view for a GET" in {
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCompanyName)).overrides().build()
      val request = FakeRequest(GET, whatTypeBusinessRoute)
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, request).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      val expectedJson = Json.obj("form" -> form, "submitUrl" -> whatTypeBusinessSubmitRoute, "radios" -> WhatTypeBusiness.radios(form))

      templateCaptor.getValue mustEqual "whatTypeBusiness.njk"

      jsonCaptor.getValue must containJson(expectedJson)

      application.stop()
    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      val request = FakeRequest(GET, whatTypeBusinessRoute)
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, request).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      val filledForm = form.bind(Map("value" -> WhatTypeBusiness.values.head.toString))

      val expectedJson = Json.obj("form" -> filledForm, "submitUrl" -> whatTypeBusinessSubmitRoute, "radios" -> WhatTypeBusiness.radios(filledForm))

      templateCaptor.getValue mustEqual "whatTypeBusiness.njk"

      jsonCaptor.getValue must containJson(expectedJson)

      application.stop()
    }

    "redirect to the next page when valid data is submitted" in {
      when(mockUserAnswersCacheConnector.save(any())(any(), any())) thenReturn Future.successful(Json.obj())
      when(mockCompoundNavigator.nextPage(any(), any(), any())).thenReturn(onwardRoute)

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCompanyName)).overrides().build()

      val request = FakeRequest(POST, whatTypeBusinessRoute).withFormUrlEncodedBody(("value", WhatTypeBusiness.values.head.toString))

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual onwardRoute.url

      application.stop()
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCompanyName)).overrides().build()
      val request = FakeRequest(POST, whatTypeBusinessRoute).withFormUrlEncodedBody(("value", "invalid value"))
      val boundForm = form.bind(Map("value" -> "invalid value"))
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, request).value

      status(result) mustEqual BAD_REQUEST

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      val expectedJson = Json.obj("form" -> boundForm, "submitUrl" -> whatTypeBusinessSubmitRoute, "radios" -> WhatTypeBusiness.radios(boundForm))

      templateCaptor.getValue mustEqual "whatTypeBusiness.njk"
      jsonCaptor.getValue must containJson(expectedJson)

      application.stop()
    }
  }
}
