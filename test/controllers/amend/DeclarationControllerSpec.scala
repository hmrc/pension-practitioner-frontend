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

package controllers.amend

import connectors.EmailConnector
import connectors.EmailSent
import connectors.SubscriptionConnector
import controllers.actions.MutableFakeDataRetrievalAction
import controllers.base.ControllerSpecBase
import matchers.JsonMatchers
import models.WhatTypeBusiness.Companyorpartnership
import models.register.BusinessType
import models.UserAnswers
import org.mockito.ArgumentCaptor
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import org.scalatest.OptionValues
import org.scalatest.TryValues
import org.scalatestplus.mockito.MockitoSugar
import pages.PspIdPage
import pages.WhatTypeBusinessPage
import pages.company.BusinessNamePage
import pages.company.CompanyEmailPage
import pages.register.AreYouUKCompanyPage
import pages.register.BusinessTypePage
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.viewmodels.NunjucksSupport

import scala.concurrent.Future

class DeclarationControllerSpec extends ControllerSpecBase with MockitoSugar with NunjucksSupport
  with JsonMatchers with OptionValues with TryValues {

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val mockSubscriptionConnector: SubscriptionConnector = mock[SubscriptionConnector]
  private val mockEmailConnector: EmailConnector = mock[EmailConnector]
  private val partnershipName = "Acme Ltd"
  private val application: Application =
    applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction,
      Seq(
        bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
        bind[EmailConnector].toInstance(mockEmailConnector)
      )).build()
  private val templateToBeRendered = "amend/declaration.njk"
  private val valuesValid: Map[String, Seq[String]] = Map("value" -> Seq("true"))
  private val email = "a@a.c"

  private def onPageLoadUrl: String = routes.DeclarationController.onPageLoad().url
  private def submitUrl: String = routes.DeclarationController.onSubmit().url

  override def beforeEach: Unit = {
    super.beforeEach
    mutableFakeDataRetrievalAction.setDataToReturn(Some(UserAnswers()))
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
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

    "redirect to next page when valid data is submitted and send email" in {
      val pspId = "psp-id"
      val templateId = "dummyTemplateId"
      when(mockEmailConnector
        .sendEmail(any(),
          Matchers.eq(pspId),
          Matchers.eq("PSPAmendment"),
          Matchers.eq(email),
          Matchers.eq(templateId),any())(any(),any()))
        .thenReturn(Future.successful(EmailSent))
      when(mockAppConfig.emailPspAmendmentTemplateId).thenReturn(templateId)

      val ua = UserAnswers()
        .setOrException(WhatTypeBusinessPage, Companyorpartnership)
        .setOrException(AreYouUKCompanyPage, true)
        .setOrException(BusinessTypePage, BusinessType.BusinessPartnership)
        .setOrException(BusinessNamePage, partnershipName)
        .setOrException(CompanyEmailPage, email)
      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val expectedJson = Json.obj(PspIdPage.toString -> pspId)
      val uaCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])

      when(mockSubscriptionConnector.subscribePsp(uaCaptor.capture())(any(), any())).thenReturn(Future.successful(pspId))
      when(mockUserAnswersCacheConnector.save(any())(any(), any())).thenReturn(Future.successful(Json.obj()))

      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])
      val result = route(application, httpPOSTRequest(submitUrl, valuesValid)).value

      status(result) mustEqual SEE_OTHER
      verify(mockUserAnswersCacheConnector, times(1)).save(jsonCaptor.capture)(any(), any())

      jsonCaptor.getValue must containJson(expectedJson)
      redirectLocation(result) mustBe Some(routes.ConfirmationController.onPageLoad().url)
    }
  }
}
