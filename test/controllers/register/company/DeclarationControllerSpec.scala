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

import controllers.actions.MutableFakeDataRetrievalAction
import controllers.base.ControllerSpecBase
import matchers.JsonMatchers
import models.UserAnswers
import org.mockito.Matchers.any
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import org.mockito.ArgumentCaptor
import org.mockito.Matchers
import org.scalatest.OptionValues
import org.scalatest.TryValues
import org.scalatestplus.mockito.MockitoSugar
import pages.register.company.DeclarationPage
import play.api.Application
import play.api.mvc.Call
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.viewmodels.NunjucksSupport

import scala.concurrent.Future

class DeclarationControllerSpec extends ControllerSpecBase with MockitoSugar with NunjucksSupport
  with JsonMatchers with OptionValues with TryValues {

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()

  private val application: Application =
    applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()
  private val templateToBeRendered = "register/company/declaration.njk"
  private val dummyCall: Call = Call("GET", "/foo")
  private val valuesValid: Map[String, Seq[String]] = Map("value" -> Seq("true"))

  private def onPageLoadUrl: String = routes.DeclarationController.onPageLoad().url
  private def submitUrl: String = routes.DeclarationController.onSubmit().url

  override def beforeEach: Unit = {
    super.beforeEach
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
    mutableFakeDataRetrievalAction.setDataToReturn(Some(UserAnswers()))
  }

  "Declaration Controller" must {
    "return OK and the correct view for a GET" in {
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])

      val result = route(application, httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), any())(any())

      templateCaptor.getValue mustEqual templateToBeRendered
    }

    "redirect to Session Expired page for a GET when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(application, httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
    }

    "redirect to next page when valid data is submitted" in {

      when(mockCompoundNavigator.nextPage(Matchers.eq(DeclarationPage), any(), any())).thenReturn(dummyCall)

      val result = route(application, httpPOSTRequest(submitUrl, valuesValid)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(dummyCall.url)

    }

    "redirect to Session Expired page for a POST when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(application, httpPOSTRequest(submitUrl, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
    }
  }
}
