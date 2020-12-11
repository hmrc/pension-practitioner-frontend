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
import connectors.SubscriptionConnector
import connectors.SubscriptionConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.actions.{DataRequiredActionImpl, DataRequiredAction, AuthAction, FakeAuthAction}
import controllers.base.ControllerSpecBase
import data.SampleData._
import models.{NormalMode, UserAnswers}
import navigators.CompoundNavigator
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.reset
import org.mockito.Mockito.{times, when, verify}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsNull, JsObject}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.nunjucks.NunjucksRenderer
import services.PspDetailsHelper._

import scala.concurrent.Future

class UpdateContactAddressControllerSpec extends ControllerSpecBase with MockitoSugar {
  private def onwardRoute = Call("GET", "/foo")
  private val mockSubscriptionConnector = mock[SubscriptionConnector]

  override def modules: Seq[GuiceableModule] = Seq(
    bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
    bind[AuthAction].to[FakeAuthAction],
    bind[NunjucksRenderer].toInstance(mockRenderer)
  )

  override def beforeEach: Unit = {
    super.beforeEach
    reset(mockSubscriptionConnector)
  }

  "UpdateContactAddress Controller" must {

    "return OK and the correct view for a GET on company" in {
      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))
      when(mockCompoundNavigator.nextPage(any(), any(), any())).thenReturn(onwardRoute)
      when(mockSubscriptionConnector.getSubscriptionDetails(any())(any(), any())).thenReturn(Future.successful(uaCompanyUk))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      val request = FakeRequest(GET, routes.UpdateContactAddressController.onPageLoad().url)
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, request).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual "updateContactAddress.njk"

      (jsonCaptor.getValue \ "addressUrl").asOpt[String]mustEqual
        Some(controllers.company.routes.CompanyPostcodeController.onPageLoad(NormalMode).url)

      application.stop()
    }

    "return OK and the correct view for a GET on individual" in {
      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))
      when(mockCompoundNavigator.nextPage(any(), any(), any())).thenReturn(onwardRoute)
      when(mockSubscriptionConnector.getSubscriptionDetails(any())(any(), any())).thenReturn(Future.successful(uaIndividualUK))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      val request = FakeRequest(GET, routes.UpdateContactAddressController.onPageLoad().url)
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, request).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual "updateContactAddress.njk"

      (jsonCaptor.getValue \ "addressUrl").asOpt[String]mustEqual
        Some(controllers.individual.routes.IndividualPostcodeController.onPageLoad(NormalMode).url)

      application.stop()
    }

    "return OK and the correct view for a GET on partnership" in {
      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))
      when(mockCompoundNavigator.nextPage(any(), any(), any())).thenReturn(onwardRoute)
      when(mockSubscriptionConnector.getSubscriptionDetails(any())(any(), any())).thenReturn(Future.successful(uaPartnershipNonUK))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      val request = FakeRequest(GET, routes.UpdateContactAddressController.onPageLoad().url)
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, request).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual "updateContactAddress.njk"

      (jsonCaptor.getValue \ "addressUrl").asOpt[String]mustEqual
        Some(controllers.partnership.routes.PartnershipPostcodeController.onPageLoad(NormalMode).url)


      application.stop()
    }
  }
}
