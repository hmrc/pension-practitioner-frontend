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

import connectors.SessionDataCacheConnector
import controllers.base.ControllerSpecBase
import data.SampleData._
import matchers.JsonMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.Future

class SignOutControllerSpec extends ControllerSpecBase with MockitoSugar with JsonMatchers with OptionValues with TryValues {

  private def signOutRoute: String = routes.SignOutController.signOut().url

  private val signoutUrl = "/foo"
  private val mockSessionDataCacheConnector: SessionDataCacheConnector = mock[SessionDataCacheConnector]
  private val mockAuthConnector: AuthConnector = mock[AuthConnector]
  private val extraModules: Seq[GuiceableModule] = Seq(
    bind[SessionDataCacheConnector].toInstance(mockSessionDataCacheConnector),
    bind[AuthConnector].toInstance(mockAuthConnector)
  )

  override def fakeApplication(): Application =
    applicationBuilder(Some(userAnswersWithCompanyName),
      extraModules = extraModules).build()

  override def beforeEach(): Unit = {
    reset(mockSessionDataCacheConnector)
    reset(mockAuthConnector)
    super.beforeEach()
  }

  "SignOut Controller" must {
    "redirect and remove all items from mongo caches" in {
      when(mockUserAnswersCacheConnector.removeAll(any(), any())).thenReturn(Future.successful(Ok))
      when(mockSessionDataCacheConnector.removeAll()(any(), any())).thenReturn(Future.successful(Ok))
      when(mockAuthConnector.authorise[Option[String]](any(), any())(any(), any())).thenReturn(Future.successful(Some("id")))
      when(mockAppConfig.signOutUrl).thenReturn(signoutUrl)

      val request = FakeRequest(GET, signOutRoute)

      val result = route(app, request).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(signoutUrl)
    }
  }
}
