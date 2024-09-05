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

package controllers.deregister.company

import audit.{AuditService, PSPDeregistration, PSPDeregistrationEmail}
import connectors._
import controllers.actions.MutableFakeDataRetrievalAction
import controllers.base.ControllerSpecBase
import forms.deregister.DeregistrationDateFormProvider
import matchers.JsonMatchers
import models.{JourneyType, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.deregister.DeregistrationDateCompanyPage
import pages.{PspEmailPage, PspNamePage}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.viewmodels.DateInput
import views.html.deregister.company.DeregistrationDateView

import java.time.LocalDate
import scala.concurrent.Future

class DeregistrationDateControllerSpec extends ControllerSpecBase with MockitoSugar
  with JsonMatchers with OptionValues with TryValues {

  private val minDate: LocalDate = LocalDate.of(2020, 2, 1)
  private val form = new DeregistrationDateFormProvider()("company", minDate)
  private val dummyCall: Call = Call("GET", "/foo")
  private val mockDeregistrationConnector = mock[DeregistrationConnector]
  private val mockEnrolmentConnector = mock[EnrolmentConnector]
  private val mockEmailConnector: EmailConnector = mock[EmailConnector]
  private val mockAuditService = mock[AuditService]
  private val mockSubscriptionConnector: SubscriptionConnector = mock[SubscriptionConnector]

  private val email = "a@a.c"
  private val companyName = "acme"
  private val userAnswers: UserAnswers = UserAnswers().set(PspNamePage, companyName).toOption.value

  private def onPageLoadUrl: String = routes.DeregistrationDateController.onPageLoad().url

  private def submitUrl: String = routes.DeregistrationDateController.onSubmit().url

  private val ua: UserAnswers = UserAnswers()
    .setOrException(PspNamePage, companyName)
    .setOrException(PspEmailPage, email)

  private val valuesValid: Map[String, Seq[String]] = Map(
    "deregistrationDate.day" -> Seq("3"),
    "deregistrationDate.month" -> Seq("4"),
    "deregistrationDate.year" -> Seq("2020"),
    "amountTaxDue" -> Seq("33.44")
  )

  private val valuesInvalid: Map[String, Seq[String]] = Map(
    "deregistrationDate.day" -> Seq("32"),
    "deregistrationDate.month" -> Seq("13"),
    "deregistrationDate.year" -> Seq("2003"),
    "amountTaxDue" -> Seq("33.44")
  )

  def extraModules: Seq[GuiceableModule] = Seq(
    bind[DeregistrationConnector].toInstance(mockDeregistrationConnector),
    bind[EnrolmentConnector].toInstance(mockEnrolmentConnector),
    bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
    bind[EmailConnector].toInstance(mockEmailConnector),
    bind[AuditService].toInstance(mockAuditService)
  )

  private lazy val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()

  override def fakeApplication(): Application =
    applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction, extraModules).build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuditService)
    mutableFakeDataRetrievalAction.setDataToReturn(Some(userAnswers))
    when(mockEnrolmentConnector.deEnrol(any(), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(HttpResponse(OK, "")))
    when(mockSubscriptionConnector.getPspApplicationDate(any())(any(), any()))
      .thenReturn(Future.successful(LocalDate.parse("2020-02-01")))
    when(mockDeregistrationConnector.deregister(any(), any())(any(), any()))
      .thenReturn(Future.successful(HttpResponse(OK, "")))
    when(mockUserAnswersCacheConnector.save(any())(any(), any())).thenReturn(Future.successful(Json.obj()))
    doNothing.when(mockAuditService).sendEvent(any())(any(), any())
  }

  "DeregistrationDate Controller" must {
    "return OK and the correct view for a GET" in {
      val request = FakeRequest(GET, onPageLoadUrl)

      val result = route(app, httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual OK

      val view = app.injector.instanceOf[DeregistrationDateView]
      val expectedView = view(
        routes.DeregistrationDateController.onSubmit(),
        companyName,
        form,
        mockAppConfig.returnToPspDashboardUrl,
        "1 February 2020",
        DateInput.localDate(form("deregistrationDate"))
      )(request, messages).toString

      contentAsString(result).removeAllNonces() mustEqual expectedView
    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      val prepopUA: UserAnswers = userAnswers.set(DeregistrationDateCompanyPage, LocalDate.now).toOption.value
      mutableFakeDataRetrievalAction.setDataToReturn(Some(prepopUA))
      val request = FakeRequest(GET, onPageLoadUrl)

      val result = route(app, httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual OK

      val view = app.injector.instanceOf[DeregistrationDateView]
      val expectedView = view(
        routes.DeregistrationDateController.onSubmit(),
        companyName,
        form.fill(LocalDate.now()),
        mockAppConfig.returnToPspDashboardUrl,
        "1 February 2020",
        DateInput.localDate(form("deregistrationDate"))
      )(request, messages).toString

      contentAsString(result).removeAllNonces() mustEqual expectedView
    }

    "redirect to Session Expired page for a GET when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(app, httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
    }

    "Save data to user answers, redirect to next page when valid data is submitted and send email, audit event and email audit event" in {
      val templateId = "dummyTemplateId"
      val pspId = "test psp id"

      val expectedJson = Json.obj(
        PspNamePage.toString -> companyName,
        DeregistrationDateCompanyPage.toString -> "2020-04-03",
        PspEmailPage.toString -> email
      )
      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))
      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(DeregistrationDateCompanyPage), any(), any())).thenReturn(dummyCall)
      when(mockEmailConnector.sendEmail(any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(EmailSent))
      when(mockAppConfig.emailPspDeregistrationTemplateId).thenReturn(templateId)

      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])
      val result = route(app, httpPOSTRequest(submitUrl, valuesValid)).value

      status(result) mustEqual SEE_OTHER
      verify(mockUserAnswersCacheConnector, times(1)).save(jsonCaptor.capture)(any(), any())
      verify(mockEmailConnector, times(1))
        .sendEmail(any(), ArgumentMatchers.eq(pspId), ArgumentMatchers.eq(JourneyType.PSP_DEREGISTRATION), ArgumentMatchers.eq(email), ArgumentMatchers.eq(templateId), any())(any(), any())
      jsonCaptor.getValue must containJson(expectedJson)
      redirectLocation(result) mustBe Some(dummyCall.url)
      verify(mockAuditService, times(1))
        .sendEvent(ArgumentMatchers.eq(PSPDeregistrationEmail(pspId, email)))(any(), any())
      verify(mockAuditService, times(1))
        .sendEvent(ArgumentMatchers.eq(PSPDeregistration(pspId)))(any(), any())
    }

    "return a BAD REQUEST when invalid data is submitted" in {
      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))
      val result = route(app, httpPOSTRequest(submitUrl, valuesInvalid)).value

      status(result) mustEqual BAD_REQUEST

      verify(mockUserAnswersCacheConnector, times(0)).save(any())(any(), any())
    }

    "redirect to Session Expired page for a POST when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(app, httpPOSTRequest(submitUrl, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
    }
  }
}