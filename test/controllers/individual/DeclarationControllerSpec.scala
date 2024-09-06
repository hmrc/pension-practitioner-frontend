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

package controllers.individual

import connectors.{EmailConnector, EmailSent, EnrolmentConnector, SubscriptionConnector}
import controllers.actions.MutableFakeDataRetrievalAction
import controllers.base.ControllerSpecBase
import controllers.individual.DeclarationControllerSpec.{email, ua}
import data.SampleData
import matchers.JsonMatchers
import models.register.RegistrationLegalStatus
import models.{ExistingPSP, JourneyType, KnownFact, KnownFacts, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.individual.{DeclarationPage, IndividualDetailsPage, IndividualEmailPage}
import pages.register.ExistingPSPPage
import pages.{PspIdPage, RegistrationInfoPage}
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Call
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.KnownFactsRetrieval
import views.html.individual.DeclarationView

import scala.concurrent.Future

class DeclarationControllerSpec extends ControllerSpecBase with MockitoSugar with NunjucksSupport
  with JsonMatchers with OptionValues with TryValues {

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val mockSubscriptionConnector: SubscriptionConnector = mock[SubscriptionConnector]
  private val mockEmailConnector: EmailConnector = mock[EmailConnector]
  private val mockEnrolmentConnector: EnrolmentConnector = mock[EnrolmentConnector]
  private val knownFactsRetrieval: KnownFactsRetrieval = mock[KnownFactsRetrieval]

  override def fakeApplication(): Application =
    applicationBuilderMutableRetrievalAction(
      mutableFakeDataRetrievalAction,
      Seq(
        bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
        bind[EmailConnector].toInstance(mockEmailConnector),
        bind[EnrolmentConnector].toInstance(mockEnrolmentConnector),
        bind[KnownFactsRetrieval].toInstance(knownFactsRetrieval)
      )
    ).build()

  private val knownFacts = Some(KnownFacts(
    Set(KnownFact("PSPID", "test-psa")),
    Set(KnownFact("NINO", "test-nino")
    )))

  private val dummyCall: Call = Call("GET", "/foo")
  private val valuesValid: Map[String, Seq[String]] = Map()

  private def onPageLoadUrl: String = routes.DeclarationController.onPageLoad().url
  private def submitUrl: String = routes.DeclarationController.onSubmit().url

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
    mutableFakeDataRetrievalAction.setDataToReturn(Some(UserAnswers()))
  }

  "Declaration Controller" must {
    "return OK and the correct view for a GET" in {
      val request = httpGETRequest(onPageLoadUrl)

      val view = app.injector.instanceOf[DeclarationView].apply(routes.DeclarationController.onSubmit())(request, messages)

      val result = route(app, request).value

      status(result) mustEqual OK
      compareResultAndView(result, view)
    }

    "redirect to Session Expired page for a GET when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(app, httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
    }

    "redirect to next page when valid data is submitted and send email" in {
      val templateId = "dummyTemplateId"
      val pspId = "psp-id"
      when(mockEmailConnector
        .sendEmail(any(),
          ArgumentMatchers.eq(pspId),
          ArgumentMatchers.eq(JourneyType.PSP_SUBSCRIPTION),
          ArgumentMatchers.eq(email),
          ArgumentMatchers.eq(templateId),any())(any(),any()))
        .thenReturn(Future.successful(EmailSent))
      when(mockAppConfig.emailPspSubscriptionTemplateId).thenReturn(templateId)

      when(mockEnrolmentConnector.enrol(any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(NO_CONTENT, "")))
      when(knownFactsRetrieval.retrieve(any())(any())).thenReturn(knownFacts)
      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val expectedJson = Json.obj(PspIdPage.toString -> pspId)
      val uaCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])
      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(DeclarationPage), any(), any())).thenReturn(dummyCall)
      when(mockSubscriptionConnector.subscribePsp(uaCaptor.capture(), any())(any(), any())).thenReturn(Future.successful(pspId))
      when(mockUserAnswersCacheConnector.save(any())(any(), any())).thenReturn(Future.successful(Json.obj()))

      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])
      val result = route(app, httpPOSTRequest(submitUrl, valuesValid)).value

      status(result) mustEqual SEE_OTHER
      verify(mockUserAnswersCacheConnector, times(1)).save(jsonCaptor.capture)(any(), any())
      jsonCaptor.getValue must containJson(expectedJson)
      uaCaptor.getValue.getOrException(ExistingPSPPage) mustBe ExistingPSP(isExistingPSP = false, existingPSPId = None)
      redirectLocation(result) mustBe Some(dummyCall.url)
    }

    "redirect to Session Expired page for a POST when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(app, httpPOSTRequest(submitUrl, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
    }

    "redirect to Cannot register Practitioner page for a POST when there is active Psp exists" in {
      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))
      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(DeclarationPage), any(), any())).thenReturn(dummyCall)

      when(mockSubscriptionConnector.subscribePsp(any(), any())(any(), any())).thenReturn(Future.failed(UpstreamErrorResponse(
        message = "ACTIVE_PSPID_ALREADY_EXISTS",
        statusCode = FORBIDDEN,
        reportAs = FORBIDDEN
      )))

      val result = route(app, httpPOSTRequest(submitUrl, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.routes.CannotRegisterPractitionerController.onPageLoad().url

    }
  }
}

object DeclarationControllerSpec {

  private val email = "a@a.c"

  val ua = UserAnswers()
    .setOrException(RegistrationInfoPage, SampleData.registrationInfo(RegistrationLegalStatus.Individual))
    .setOrException(IndividualDetailsPage, SampleData.tolerantIndividual)
    .setOrException(IndividualEmailPage, email)
}