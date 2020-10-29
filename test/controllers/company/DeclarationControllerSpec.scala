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

package controllers.company

import connectors.{EmailConnector, EmailSent, EnrolmentConnector, SubscriptionConnector}
import controllers.actions.MutableFakeDataRetrievalAction
import controllers.base.ControllerSpecBase
import matchers.JsonMatchers
import models.WhatTypeBusiness.Companyorpartnership
import models.register.BusinessType
import models.{ExistingPSP, KnownFact, KnownFacts, UserAnswers}
import org.mockito.{ArgumentCaptor, Matchers}
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.{PspIdPage, WhatTypeBusinessPage}
import pages.company.{BusinessNamePage, CompanyEmailPage, DeclarationPage}
import pages.register.{AreYouUKCompanyPage, BusinessTypePage, ExistingPSPPage}
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Call
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.KnownFactsRetrieval

import scala.concurrent.Future

class DeclarationControllerSpec extends ControllerSpecBase with MockitoSugar with NunjucksSupport
  with JsonMatchers with OptionValues with TryValues {

  import DeclarationControllerSpec._

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val mockSubscriptionConnector: SubscriptionConnector = mock[SubscriptionConnector]
  private val mockEmailConnector: EmailConnector = mock[EmailConnector]
  private val mockEnrolmentConnector: EnrolmentConnector = mock[EnrolmentConnector]
  private val knownFactsRetrieval: KnownFactsRetrieval = mock[KnownFactsRetrieval]

  private val application: Application =
    applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction,
      Seq(
        bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
        bind[EmailConnector].toInstance(mockEmailConnector),
        bind[EnrolmentConnector].toInstance(mockEnrolmentConnector),
        bind[KnownFactsRetrieval].toInstance(knownFactsRetrieval)
      )).build()

  private val templateToBeRendered = "register/declaration.njk"
  private val dummyCall: Call = Call("GET", "/foo")
  private val valuesValid: Map[String, Seq[String]] = Map("value" -> Seq("true"))
  private val knownFacts = Some(KnownFacts(
    Set(KnownFact("PSPID", "test-psa")),
    Set(KnownFact("NINO", "test-nino")
    )))

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
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered
    }

    "redirect to Session Expired page for a GET when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(application, httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
    }

    "redirect to next page when valid data is submitted and send email" in {
      val templateId = "dummyTemplateId"
      val pspId = "psp-id"
      when(mockEmailConnector
        .sendEmail(any(),
          Matchers.eq(pspId),
          Matchers.eq("PSPSubscription"),
          Matchers.eq(email),
          Matchers.eq(templateId),any())(any(),any()))
        .thenReturn(Future.successful(EmailSent))
      when(mockEnrolmentConnector.enrol(any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(NO_CONTENT, "")))
      when(knownFactsRetrieval.retrieve(any())(any())).thenReturn(knownFacts)
      when(mockAppConfig.emailPspSubscriptionTemplateId).thenReturn(templateId)

      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val expectedJson = Json.obj(PspIdPage.toString -> pspId)

      val uaCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])

      when(mockCompoundNavigator.nextPage(Matchers.eq(DeclarationPage), any(), any())).thenReturn(dummyCall)
      when(mockSubscriptionConnector.subscribePsp(uaCaptor.capture())(any(), any())).thenReturn(Future.successful(pspId))
      when(mockUserAnswersCacheConnector.save(any())(any(), any())).thenReturn(Future.successful(Json.obj()))

      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])
      val result = route(application, httpPOSTRequest(submitUrl, valuesValid)).value

      status(result) mustEqual SEE_OTHER
      verify(mockUserAnswersCacheConnector, times(1)).save(jsonCaptor.capture)(any(), any())

      jsonCaptor.getValue must containJson(expectedJson)
      uaCaptor.getValue.getOrException(ExistingPSPPage) mustBe ExistingPSP(isExistingPSP = false, existingPSPId = None)
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

object DeclarationControllerSpec {

  private val partnershipName = "Acme Ltd"
  private val email = "a@a.c"

  val ua: UserAnswers = UserAnswers()
    .setOrException(WhatTypeBusinessPage, Companyorpartnership)
    .setOrException(AreYouUKCompanyPage, true)
    .setOrException(BusinessTypePage, BusinessType.BusinessPartnership)
    .setOrException(BusinessNamePage, partnershipName)
    .setOrException(CompanyEmailPage, email)
}
