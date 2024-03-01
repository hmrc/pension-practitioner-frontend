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

package audit

import com.kenshoo.play.metrics.Metrics
import config.FrontendAppConfig
import models.requests.UserType
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.DataEvent
import utils.TestMetrics

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuditServiceSpec extends AnyWordSpec with Matchers with MockitoSugar with Inside {

  private val mockAuditConnector = mock[AuditConnector]

  private def app: Application = new GuiceApplicationBuilder()
    .configure(
      //turn off metrics
      "metrics.jvm" -> false,
      "metrics.enabled" -> false
    )
    .overrides(
      bind[AuditConnector].toInstance(mockAuditConnector),
      bind[Metrics].toInstance(new TestMetrics)
    )
    .build()

  private val auditService = app.injector.instanceOf[AuditService]

  private def config: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  "AuditService" must {
    "sent audit event" in {
      val aftAuditEvent = PSPStartEvent(UserType.Organisation, existingUser = false)
      val templateCaptor = ArgumentCaptor.forClass(classOf[DataEvent])

      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

      when(mockAuditConnector.sendEvent(any())(any(), any())).thenReturn(Future(Success))

      auditService.sendEvent(aftAuditEvent)

      verify(mockAuditConnector, times(1)).sendEvent(templateCaptor.capture())
      inside(templateCaptor.getValue) {
        case DataEvent(auditSource, auditType, _, _, detail, _, _, _) =>
          auditSource mustBe config.appName
          auditType mustBe "PSPStartNew"
          detail mustBe Map("userType" -> "Organisation", "existingUser" -> "false")
      }
      app.stop()
    }
  }
}
