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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import models.{JourneyType, UserAnswers}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import play.api.http.Status
import play.api.http.Status.OK
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http._
import utils.WireMockHelper

import java.time.LocalDate

class SubscriptionConnectorSpec
  extends AsyncWordSpec
    with Matchers
    with WireMockHelper {

  private implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  override protected def portConfigKey: String = "microservice.services.pension-practitioner.port"

  private lazy val connector: SubscriptionConnector = injector.instanceOf[SubscriptionConnector]
  private val pspSubscriptionUrl = "/pension-practitioner/subscribePsp/PSPSubscription"
  private val subscriptionDetailsUrl = "/pension-practitioner/getPsp-self"
  private val pspId: String = "12345678"
  private val validResponse = Json.obj(
    "processingDate" -> LocalDate.now,
    "formBundleNumber" -> "12345678912",
    "pspid" -> pspId,
    "nino" -> "AA123000A",
    "countryCode" -> "AD"
  )
  private val getPspDetailsResponse: JsValue = Json.obj("test-key" -> "test-value")

  "subscribePsp" must {

    "return successfully when the backend has returned OK" in {
      val data = Json.obj(fields = "Id" -> "value")
      server.stubFor(
        post(urlEqualTo(pspSubscriptionUrl))
          .withRequestBody(equalTo(Json.stringify(data)))
          .willReturn(
            ok(validResponse.toString())
              .withHeader("Content-Type", "application/json")
          )
      )

      connector.subscribePsp(UserAnswers(data), JourneyType.PSP_SUBSCRIPTION) map {
        response => response mustBe pspId
      }
    }

    "return BAD REQUEST when the backend has returned BadRequestException" in {
      val data = Json.obj(fields = "Id" -> "value")
      server.stubFor(
        post(urlEqualTo(pspSubscriptionUrl))
          .withRequestBody(equalTo(Json.stringify(data)))
          .willReturn(
            badRequest()
          )
      )

      recoverToExceptionIf[BadRequestException] {
        connector.subscribePsp(UserAnswers(data), JourneyType.PSP_SUBSCRIPTION)
      } map {
        _.responseCode mustEqual Status.BAD_REQUEST
      }
    }

    "return NOT FOUND when the backend has returned NotFoundException" in {
      val data = Json.obj(fields = "Id" -> "value")
      server.stubFor(
        post(urlEqualTo(pspSubscriptionUrl))
          .withRequestBody(equalTo(Json.stringify(data)))
          .willReturn(
            notFound()
          )
      )

      recoverToExceptionIf[NotFoundException] {
        connector.subscribePsp(UserAnswers(data), JourneyType.PSP_SUBSCRIPTION)
      } map {
        _.responseCode mustEqual Status.NOT_FOUND
      }
    }

    "return UpstreamErrorResponse when the backend has returned Internal Server Error" in {
      val data = Json.obj(fields = "Id" -> "value")
      server.stubFor(
        post(urlEqualTo(pspSubscriptionUrl))
          .withRequestBody(equalTo(Json.stringify(data)))
          .willReturn(
            serverError()
          )
      )

      recoverToExceptionIf[UpstreamErrorResponse](connector.subscribePsp(UserAnswers(data), JourneyType.PSP_SUBSCRIPTION)) map {
        _.statusCode mustBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  "getPspDetails" must {
    "return 200" in {

      server.stubFor(
        get(urlEqualTo(subscriptionDetailsUrl)).withHeader("pspId", equalTo(pspId))
          .willReturn(
            aResponse()
              .withStatus(OK).withBody(getPspDetailsResponse.toString())
          )
      )

      connector.getSubscriptionDetails(pspId).map {
        result =>
          result mustBe getPspDetailsResponse
          server.findAll(getRequestedFor(urlEqualTo(subscriptionDetailsUrl))
            .withHeader("pspId", equalTo(pspId))).size() mustBe 1
      }

    }

    "throw badrequest if INVALID_IDVALUE" in {
      server.stubFor(
        get(urlEqualTo(subscriptionDetailsUrl)).withHeader("pspId", equalTo(pspId))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST).withBody("INVALID_IDVALUE")
          )
      )

      recoverToExceptionIf[PspIdInvalidSubscriptionException] {
        connector.getSubscriptionDetails(pspId)
      } map {
        _ =>
          server.findAll(getRequestedFor(urlEqualTo(subscriptionDetailsUrl))
            .withHeader("pspId", equalTo(pspId))).size() mustBe 1
      }
    }

    "throw badrequest if INVALID_CORRELATIONID" in {
      server.stubFor(
        get(urlEqualTo(subscriptionDetailsUrl)).withHeader("pspId", equalTo(pspId))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST).withBody("INVALID_CORRELATIONID")
          )
      )

      recoverToExceptionIf[CorrelationIdInvalidSubscriptionException] {
        connector.getSubscriptionDetails(pspId)
      } map {
        _ =>
          server.findAll(getRequestedFor(urlEqualTo(subscriptionDetailsUrl))
            .withHeader("pspId", equalTo(pspId))).size() mustBe 1
      }
    }

    "throw Not Found" in {
      server.stubFor(
        get(urlEqualTo(subscriptionDetailsUrl)).withHeader("pspId", equalTo(pspId))
          .willReturn(
            notFound()
          )
      )

      recoverToExceptionIf[PspIdNotFoundSubscriptionException] {
        connector.getSubscriptionDetails(pspId)
      } map {
        _ =>
          server.findAll(getRequestedFor(urlEqualTo(subscriptionDetailsUrl))
            .withHeader("pspId", equalTo(pspId))).size() mustBe 1
      }
    }

    "throw UpstreamErrorResponse for internal server error" in {
      server.stubFor(
        get(urlEqualTo(subscriptionDetailsUrl)).withHeader("pspId", equalTo(pspId))
          .willReturn(
            serverError()
          )
      )

      recoverToExceptionIf[UpstreamErrorResponse] {
        connector.getSubscriptionDetails(pspId)
      } map {
        _ =>
          server.findAll(getRequestedFor(urlEqualTo(subscriptionDetailsUrl))
            .withHeader("pspId", equalTo(pspId))).size() mustBe 1
      }
    }

    "throw Generic exception for all others" in {
      server.stubFor(
        get(urlEqualTo(subscriptionDetailsUrl)).withHeader("pspId", equalTo(pspId))
          .willReturn(
            serverError()
          )
      )

      recoverToExceptionIf[Exception] {
        connector.getSubscriptionDetails(pspId)
      } map {
        _ =>
          server.findAll(getRequestedFor(urlEqualTo(subscriptionDetailsUrl))
            .withHeader("pspId", equalTo(pspId))).size() mustBe 1
      }
    }
  }

  def errorResponse(code: String): String = {
    Json.stringify(
      Json.obj(
        "code" -> code,
        "reason" -> s"Reason for $code"
      )
    )
  }

}
