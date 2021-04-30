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

import java.time.LocalDate
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest._
import play.api.http.Status
import play.api.http.Status.OK
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http._
import utils.WireMockHelper

class DeregistrationConnectorSpec
  extends AsyncWordSpec
    with MustMatchers
    with WireMockHelper {

  private implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  override protected def portConfigKey: String = "microservice.services.pension-practitioner.port"

  private lazy val connector: DeregistrationConnector = injector.instanceOf[DeregistrationConnector]
  private val pspId: String = "12345678"
  private val deregistrationUrl = s"/pension-practitioner/deregisterPsp/$pspId"
  private val canDeregisterUrl = s"/pension-practitioner/can-deregister/$pspId"
  private val date = LocalDate.parse("2020-01-01")
  private val validResponse = Json.obj(
    "processingDate" -> LocalDate.now,
    "formBundleNumber" -> "12345678912",
    "pspid" -> pspId,
    "nino" -> "AA123000A",
    "countryCode" -> "AD"
  )
  private val requestBody: JsValue = Json.obj(
    "deregistrationDate"-> date.toString,
    "reason" -> "1"
  )

  "deregisterPsp" must {

    "return successfully when the backend has returned OK" in {

      server.stubFor(
        post(urlEqualTo(deregistrationUrl))
          .withRequestBody(equalTo(Json.stringify(requestBody)))
          .willReturn(
            ok(validResponse.toString())
              .withHeader("Content-Type", "application/json")
          )
      )

      connector.deregister(pspId, date) map {
        response => response.status mustBe OK
      }
    }

    "return BAD REQUEST when the backend has returned BadRequestException" in {

      server.stubFor(
        post(urlEqualTo(deregistrationUrl))
          .withRequestBody(equalTo(Json.stringify(requestBody)))
          .willReturn(
            badRequest()
          )
      )

      recoverToExceptionIf[BadRequestException] {
        connector.deregister(pspId, date)
      } map {
        _.responseCode mustEqual Status.BAD_REQUEST
      }
    }

    "return NOT FOUND when the backend has returned NotFoundException" in {

      server.stubFor(
        post(urlEqualTo(deregistrationUrl))
          .withRequestBody(equalTo(Json.stringify(requestBody)))
          .willReturn(
            notFound()
          )
      )

      recoverToExceptionIf[NotFoundException] {
        connector.deregister(pspId, date)
      } map {
        _.responseCode mustEqual Status.NOT_FOUND
      }
    }

    "return UpstreamErrorResponse when the backend has returned Internal Server Error" in {

      server.stubFor(
        post(urlEqualTo(deregistrationUrl))
          .withRequestBody(equalTo(Json.stringify(requestBody)))
          .willReturn(
            serverError()
          )
      )

      recoverToExceptionIf[UpstreamErrorResponse](connector.deregister(pspId, date)) map {
        _.statusCode mustBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  "canDeregister" must {
    "return 200" in {

      server.stubFor(
        get(urlEqualTo(canDeregisterUrl))
          .willReturn(
            aResponse()
              .withStatus(OK).withBody("true")
          )
      )

      connector.canDeRegister(pspId).map {
        result =>
          result mustBe true
          server.findAll(getRequestedFor(urlEqualTo(canDeregisterUrl))).size() mustBe 1
      }

    }

    "throw badrequest if INVALID_IDVALUE" in {
      server.stubFor(
        get(urlEqualTo(canDeregisterUrl))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST).withBody("INVALID_IDVALUE")
          )
      )

      recoverToExceptionIf[BadRequestException] {
        connector.canDeRegister(pspId)
      } map {
        _ =>
          server.findAll(getRequestedFor(urlEqualTo(canDeregisterUrl))).size() mustBe 1
      }
    }

    "throw badrequest if INVALID_CORRELATIONID" in {
      server.stubFor(
        get(urlEqualTo(canDeregisterUrl))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST).withBody("INVALID_CORRELATIONID")
          )
      )

      recoverToExceptionIf[BadRequestException] {
        connector.canDeRegister(pspId)
      } map {
        _ =>
          server.findAll(getRequestedFor(urlEqualTo(canDeregisterUrl))).size() mustBe 1
      }
    }

    "return true if Not Found is returned" in {
      server.stubFor(
        get(urlEqualTo(canDeregisterUrl))
          .willReturn(
            notFound()
          )
      )

        connector.canDeRegister(pspId).map {
        result =>
          result mustBe true
          server.findAll(getRequestedFor(urlEqualTo(canDeregisterUrl))).size() mustBe 1
      }
    }

    "throw UpstreamErrorResponse for internal server error" in {
      server.stubFor(
        get(urlEqualTo(canDeregisterUrl))
          .willReturn(
            serverError()
          )
      )

      recoverToExceptionIf[UpstreamErrorResponse] {
        connector.canDeRegister(pspId)
      } map {
        _ =>
          server.findAll(getRequestedFor(urlEqualTo(canDeregisterUrl))).size() mustBe 1
      }
    }

    "throw Generic exception for all others" in {
      server.stubFor(
        get(urlEqualTo(canDeregisterUrl))
          .willReturn(
            serverError()
          )
      )

      recoverToExceptionIf[Exception] {
        connector.canDeRegister(pspId)
      } map {
        _ =>
          server.findAll(getRequestedFor(urlEqualTo(canDeregisterUrl))).size() mustBe 1
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
