/*
 * Copyright 2022 HM Revenue & Customs
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
import org.scalatest._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import utils.WireMockHelper

class PersonalDetailsValidationConnectorSpec extends AsyncWordSpec with Matchers with WireMockHelper with OptionValues {

  import PersonalDetailsValidationConnectorSpec._

  override protected def portConfigKey: String = "microservice.services.personal-details-validation.port"

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private lazy val connector = injector.instanceOf[PersonalDetailsValidationConnector]

  private val url: String = s"/personal-details-validation/$journeyId"

  ".retrieveNino" must {

    "return a Nino" when {
      "personal-details-validation returned successfully with a nino" in {
        server.stubFor(
          get(urlEqualTo(url)
          ).willReturn(
            ok(expectedResponse)
          )
        )

        connector.retrieveNino(journeyId).map {
          result =>
            result.value mustBe Nino("AB000003D")
        }
      }
    }

    "return None" when {
      "no nino returned from personal-details-validation" in {
        server.stubFor(
          get(urlEqualTo(url)
          ).willReturn(
            ok(responseWithoutNino)
          )
        )

        connector.retrieveNino(journeyId).map {
          result => {
            result mustBe None
          }
        }
      }

      "nino is malformed" in {
        server.stubFor(
          get(urlEqualTo(url)
          ).willReturn(
            ok(responseWithMalformedNino)
          )
        )

        connector.retrieveNino(journeyId).map {
          result =>
            result mustBe None
        }
      }

      "no results are found for the requested journey Id" in {
        server.stubFor(
          get(urlEqualTo(url)
          ).willReturn(
            notFound()
          )
        )

        connector.retrieveNino(journeyId).map {
          result =>
            result mustBe None
        }
      }
    }
  }
}

object PersonalDetailsValidationConnectorSpec {

  private val journeyId = "1234"
  private val expectedResponse =
    """{
      |  "id": "502f90f7-13ab-44c4-a4fa-474da0f0fe03",
      |  "validationStatus": "success",
      |  "personalDetails": {
      |    "firstName": "Jim",
      |    "lastName": "Ferguson",
      |    "nino": "AB000003D",
      |    "dateOfBirth": "1948-04-23"
      |  }
      |}""".stripMargin

  private val responseWithMalformedNino =
    """{
      |  "id": "502f90f7-13ab-44c4-a4fa-474da0f0fe03",
      |  "validationStatus": "success",
      |  "personalDetails": {
      |    "firstName": "Jim",
      |    "lastName": "Ferguson",
      |    "nino": "blah",
      |    "dateOfBirth": "1948-04-23"
      |  }
      |}""".stripMargin

  private val responseWithoutNino =
    """{
      |  "id": "502f90f7-13ab-44c4-a4fa-474da0f0fe03",
      |  "validationStatus": "success",
      |  "personalDetails": {
      |    "firstName": "Jim",
      |    "lastName": "Ferguson",
      |    "dateOfBirth": "1948-04-23"
      |  }
      |}""".stripMargin
}