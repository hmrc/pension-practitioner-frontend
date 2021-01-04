/*
 * Copyright 2021 HM Revenue & Customs
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

package connectors

import audit.{AuditService, PSPEnrolment, StubSuccessfulAuditService}
import com.github.tomakehurst.wiremock.client.WireMock._
import models.requests.{DataRequest, PSPUser, UserType}
import models.{KnownFact, KnownFacts, UserAnswers}
import org.scalatest.{AsyncWordSpec, MustMatchers, RecoverMethods}
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, UpstreamErrorResponse}
import utils.WireMockHelper

import scala.concurrent.ExecutionContext.Implicits.global

class EnrolmentConnectorSpec extends AsyncWordSpec with MustMatchers with WireMockHelper with RecoverMethods {

  override protected def portConfigKey: String = "microservice.services.tax-enrolments.port"

  private val testPspId = "test-psp-id"
  private val testUserId = "test"

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val dataRequest: DataRequest[AnyContent] = DataRequest(FakeRequest("", ""), testUserId,
    PSPUser(UserType.Organisation, None, isExistingPSP = false, None, None), UserAnswers())

  private def url: String = s"/tax-enrolments/service/HMRC-PODSPP-ORG/enrolment"

  private lazy val connector = injector.instanceOf[EnrolmentConnector]
  private val fakeAuditService = new StubSuccessfulAuditService()

  override def beforeEach(): Unit = {
    fakeAuditService.reset()
    super.beforeEach()
  }

  override protected def bindings: Seq[GuiceableModule] =
    Seq(bind[AuditService].toInstance(fakeAuditService))

  ".enrol" must {

    "return a successful response" when {
      "enrolments returns code NO_CONTENT" which {
        "means the enrolment was updated or created successfully" in {

          val knownFacts = KnownFacts(Set(KnownFact("PSPID", testPspId)), Set(KnownFact("NINO", "JJ123456P")))
          val expectedAuditEvent = PSPEnrolment(testUserId, testPspId)

          server.stubFor(
            put(urlEqualTo(url))
              .willReturn(
                noContent
              )
          )

          connector.enrol(testPspId, knownFacts) map {
            result =>
              result.status mustEqual NO_CONTENT
              fakeAuditService.verifySent(expectedAuditEvent) mustBe true
          }
        }
      }
    }
    "return a failure" when {
      "enrolments returns BAD_REQUEST" which {
        "means the POST body wasn't as expected" in {

          val knownFacts = KnownFacts(Set(KnownFact("PSPID", testPspId)), Set.empty[KnownFact])

          server.stubFor(
            put(urlEqualTo(url))
              .willReturn(
                badRequest
              )
          )

          recoverToSucceededIf[BadRequestException] {
            connector.enrol(testPspId, knownFacts)
          }
        }
      }
      "enrolments returns UNAUTHORISED" which {
        "means missing or incorrect MDTP bearer token" in {

          val knownFacts = KnownFacts(Set(KnownFact("PSPID", testPspId)), Set(KnownFact("NINO", "JJ123456P")))

          server.stubFor(
            put(urlEqualTo(url))
              .willReturn(
                unauthorized
              )
          )

          recoverToSucceededIf[UpstreamErrorResponse] {
            connector.enrol(testPspId, knownFacts)
          }
        }
      }
    }
  }
}
