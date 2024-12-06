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

import connectors.{EmailConnector, EmailSent, SubscriptionConnector}
import controllers.actions.MutableFakeDataRetrievalAction
import controllers.base.ControllerSpecBase
import data.SampleData
import matchers.JsonMatchers
import models.register.RegistrationLegalStatus
import models.{JourneyType, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import pages.company.{BusinessNamePage, CompanyEmailPage}
import pages.{PspIdPage, RegistrationInfoPage}
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import services.PspDetailsService

import scala.concurrent.Future

class DeclarationControllerSpec
  extends ControllerSpecBase with JsonMatchers {

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val mockSubscriptionConnector: SubscriptionConnector = mock[SubscriptionConnector]
  private val mockEmailConnector: EmailConnector = mock[EmailConnector]
  private val mockPspDetailsService: PspDetailsService = mock[PspDetailsService]
  private val partnershipName = "Acme Ltd"
  override def fakeApplication(): Application =
    applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction,
      Seq(
        bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
        bind[EmailConnector].toInstance(mockEmailConnector),
        bind[PspDetailsService].to(mockPspDetailsService)
      )).build()
  private val valuesValid: Map[String, Seq[String]] = Map("value" -> Seq("true"))
  private val email = "a@a.c"

  private def onPageLoadUrl: String = routes.DeclarationController.onPageLoad().url

  private def submitUrl: String = routes.DeclarationController.onSubmit().url

  override def beforeEach(): Unit = {
    super.beforeEach()
    mutableFakeDataRetrievalAction.setDataToReturn(Some(UserAnswers()))
  }

  "Declaration Controller" must {

    "return OK and the correct view for a GET" in {
      when(mockPspDetailsService.amendmentsExist(any())).thenReturn(true)

      val req = httpGETRequest(onPageLoadUrl)

      val result = route(app, req).value

      status(result) mustEqual OK

      val view = app.injector.instanceOf[views.html.amend.DeclarationView].apply(controllers.amend.routes.DeclarationController.onSubmit())(req, messages)

      compareResultAndView(result, view)
    }

    "redirect to ViewDetails page if no amendments have been made" in {
      when(mockPspDetailsService.amendmentsExist(any())).thenReturn(false)

      val result = route(app, httpGETRequest(onPageLoadUrl)).value
      status(result) mustEqual SEE_OTHER
    }

    "redirect to next page when valid data is submitted and send email" in {
      val pspId = "psp-id"
      val templateId = "dummyTemplateId"
      when(mockEmailConnector.sendEmail(
        requestId = any(),
        pspId = ArgumentMatchers.eq(pspId),
        journeyType = ArgumentMatchers.eq(JourneyType.PSP_AMENDMENT),
        emailAddress = ArgumentMatchers.eq(email),
        templateName = ArgumentMatchers.eq(templateId), templateParams = any()
      )(
        hc = any(),
        executionContext = any()
      )).thenReturn(Future.successful(EmailSent))

      when(mockAppConfig.emailPspAmendmentTemplateId).thenReturn(templateId)

      val ua = UserAnswers()
        .setOrException(RegistrationInfoPage, SampleData.registrationInfo(RegistrationLegalStatus.Partnership))
        .setOrException(BusinessNamePage, partnershipName)
        .setOrException(CompanyEmailPage, email)

      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val expectedJson = Json.obj(PspIdPage.toString -> pspId)
      val uaCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])

      when(mockSubscriptionConnector.getSubscriptionDetails(any(), any())).thenReturn(Future.successful(Json.obj()))
      when(mockSubscriptionConnector.subscribePsp(uaCaptor.capture(), any())(any(), any())).thenReturn(Future.successful(pspId))
      when(mockUserAnswersCacheConnector.save(any())(any(), any())).thenReturn(Future.successful(Json.obj()))

      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])
      val result = route(app, httpPOSTRequest(submitUrl, valuesValid)).value

      status(result) mustEqual SEE_OTHER
      verify(mockUserAnswersCacheConnector, times(1)).save(jsonCaptor.capture)(any(), any())

      jsonCaptor.getValue must containJson(expectedJson)
      redirectLocation(result) mustBe Some(routes.ConfirmationController.onPageLoad().url)
    }
  }
}
