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
import models._
import models.register._
import org.scalatest._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsResultException, Json}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import utils.{UnrecognisedHttpResponseException, WireMockHelper}

class RegistrationConnectorSpec
  extends AsyncWordSpec
    with Matchers
    with OptionValues
    with WireMockHelper {

  import RegistrationConnectorSpec._

  override protected def portConfigKey: String = "microservice.services.pension-practitioner.port"
  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  "registerWithIdOrganisation" must {

    "return the address given a valid UTR" in {

      server.stubFor(
        post(urlEqualTo(organisationPath))
          .willReturn(
            aResponse()
              .withStatus(Status.OK)
              .withHeader("Content-Type", "application/json")
              .withBody(Json.stringify(validOrganizationResponse(true)))
          )
      )

      val connector = injector.instanceOf[RegistrationConnector]
      connector.registerWithIdOrganisation(utr, organisation, legalStatus).map { registration =>
        registration.response.address mustBe expectedAddress(true)
      }

    }

    "return the registration info for a company with a UK address" in {

      val info = RegistrationInfo(
        RegistrationLegalStatus.LimitedCompany,
        sapNumber,
        noIdentifier = false,
        RegistrationCustomerType.UK,
        Some(RegistrationIdType.UTR),
        Some(utr)
      )

      server.stubFor(
        post(urlEqualTo(organisationPath))
          .willReturn(
            aResponse()
              .withStatus(Status.OK)
              .withHeader("Content-Type", "application/json")
              .withBody(Json.stringify(validOrganizationResponse(true)))
          )
      )

      val connector = injector.instanceOf[RegistrationConnector]
      connector.registerWithIdOrganisation(utr, organisation, legalStatus).map { registration =>
        registration.info mustBe info
      }

    }

    "return the registration info for a company with a non-UK address" in {

      val info = RegistrationInfo(
        RegistrationLegalStatus.LimitedCompany,
        sapNumber,
        noIdentifier = false,
        RegistrationCustomerType.NonUK,
        Some(RegistrationIdType.UTR),
        Some(utr)
      )

      server.stubFor(
        post(urlEqualTo(organisationPath))
          .willReturn(
            aResponse()
              .withStatus(Status.OK)
              .withHeader("Content-Type", "application/json")
              .withBody(Json.stringify(validOrganizationResponse(false)))
          )
      )

      val connector = injector.instanceOf[RegistrationConnector]
      connector.registerWithIdOrganisation(utr, organisation, legalStatus).map { registration =>
        registration.info mustBe info
      }

    }

    "propagate exceptions from HttpClientV2" in {

      server.stubFor(
        post(urlEqualTo(organisationPath))
          .willReturn(
            aResponse()
              .withStatus(Status.NOT_FOUND)
          )
      )

      val connector = injector.instanceOf[RegistrationConnector]
      recoverToSucceededIf[NotFoundException] {
        connector.registerWithIdOrganisation(utr, organisation, legalStatus)
      }

    }

    "identify JSON parse errors" in {

      server.stubFor(
        post(urlEqualTo(organisationPath))
          .willReturn(
            aResponse()
              .withStatus(Status.OK)
              .withHeader("Content-Type", "application/json")
              .withBody(Json.stringify(invalidResponse))
          )
      )

      val connector = injector.instanceOf[RegistrationConnector]
      recoverToSucceededIf[JsResultException] {
        connector.registerWithIdOrganisation(utr, organisation, legalStatus)
      }

    }

    "forward HTTP headers" in {

      val headerName = "test-header-name"
      val headerValue = "test-header-value"

      server.stubFor(
        post(urlEqualTo(organisationPath))
          .withHeader(headerName, equalTo(headerValue))
          .willReturn(
            aResponse()
              .withStatus(Status.OK)
              .withHeader("Content-Type", "application/json")
              .withBody(Json.stringify(validOrganizationResponse(true)))
          )
      )

      val connector = injector.instanceOf[RegistrationConnector]

      val hc: HeaderCarrier = HeaderCarrier(extraHeaders = Seq((headerName, headerValue)))

      connector.registerWithIdOrganisation(utr, organisation, legalStatus)(hc,
        scala.concurrent.ExecutionContext.global).map { _ =>
        succeed
      }

    }
  }

  "registerWithIdIndividual" must {
    "return the individual and address given a valid NINO" in {
      server.stubFor(
        post(urlEqualTo(individualPath))
          .withHeader("nino", equalTo("AB100100A"))
          .willReturn(
            aResponse()
              .withStatus(Status.OK)
              .withHeader("Content-Type", "application/json")
              .withBody(Json.stringify(validIndividualResponse(true)))
          )
      )

      val connector = injector.instanceOf[RegistrationConnector]
      connector.registerWithIdIndividual(nino).map { registration =>
        registration.response.individual mustBe expectedIndividual
        registration.response.address mustBe expectedAddress(true)
      }

    }

    "return the individual and address given a valid NINO when manual Iv is disabled" in {
      lazy val appWithIvDisabled: Application = new GuiceApplicationBuilder()
        .configure(
          portConfigKey -> server.port().toString,
          "auditing.enabled" -> false,
          "metrics.enabled" -> false,
          "metrics.jvm" -> false
        )
        .build()

      val injector = appWithIvDisabled.injector
      server.stubFor(
        post(urlEqualTo(individualPath))
          .willReturn(
            aResponse()
              .withStatus(Status.OK)
              .withHeader("Content-Type", "application/json")
              .withBody(Json.stringify(validIndividualResponse(true)))
          )
      )

      val connector = injector.instanceOf[RegistrationConnector]
      connector.registerWithIdIndividual(nino).map { registration =>
        registration.response.individual mustBe expectedIndividual
        registration.response.address mustBe expectedAddress(true)
      }
    }

    "return the registration info for an individual with a UK address" in {

      val info = RegistrationInfo(
        RegistrationLegalStatus.Individual,
        sapNumber,
        noIdentifier = false,
        RegistrationCustomerType.UK,
        Some(RegistrationIdType.Nino),
        Some(nino.nino)
      )

      server.stubFor(
        post(urlEqualTo(individualPath))
          .willReturn(
            aResponse()
              .withStatus(Status.OK)
              .withHeader("Content-Type", "application/json")
              .withBody(Json.stringify(validIndividualResponse(true)))
          )
      )

      val connector = injector.instanceOf[RegistrationConnector]
      connector.registerWithIdIndividual(nino).map { registration =>
        registration.info mustBe info
      }

    }

    "return the registration info for an individual with a Non-UK address" in {

      val info = RegistrationInfo(
        RegistrationLegalStatus.Individual,
        sapNumber,
        noIdentifier = false,
        RegistrationCustomerType.NonUK,
        Some(RegistrationIdType.Nino),
        Some(nino.nino)
      )

      server.stubFor(
        post(urlEqualTo(individualPath))
          .willReturn(
            aResponse()
              .withStatus(Status.OK)
              .withHeader("Content-Type", "application/json")
              .withBody(Json.stringify(validIndividualResponse(false)))
          )
      )

      val connector = injector.instanceOf[RegistrationConnector]
      connector.registerWithIdIndividual(nino).map { registration =>
        registration.info mustBe info
      }

    }

    "only accept responses with status 200 OK" in {

      server.stubFor(
        post(urlEqualTo(individualPath))
          .willReturn(
            aResponse()
              .withStatus(Status.ACCEPTED)
              .withHeader("Content-Type", "application/json")
              .withBody(Json.stringify(validIndividualResponse(true)))
          )
      )

      val connector = injector.instanceOf[RegistrationConnector]
      recoverToSucceededIf[UnrecognisedHttpResponseException] {
        connector.registerWithIdIndividual(nino)
      }

    }

    "propagate exceptions from HttpClientV2" in {

      server.stubFor(
        post(urlEqualTo(individualPath))
          .willReturn(
            aResponse()
              .withStatus(Status.NOT_FOUND)
          )
      )

      val connector = injector.instanceOf[RegistrationConnector]
      recoverToSucceededIf[NotFoundException] {
        connector.registerWithIdIndividual(nino)
      }

    }

    "identify JSON parse errors" in {

      server.stubFor(
        post(urlEqualTo(individualPath))
          .willReturn(
            aResponse()
              .withStatus(Status.OK)
              .withHeader("Content-Type", "application/json")
              .withBody(Json.stringify(invalidResponse))
          )
      )

      val connector = injector.instanceOf[RegistrationConnector]
      recoverToSucceededIf[JsResultException] {
        connector.registerWithIdIndividual(nino)
      }

    }

    "forward HTTP headers" in {

      val headerName = "test-header-name"
      val headerValue = "test-header-value"

      server.stubFor(
        post(urlEqualTo(individualPath))
          .withHeader(headerName, equalTo(headerValue))
          .willReturn(
            aResponse()
              .withStatus(Status.OK)
              .withHeader("Content-Type", "application/json")
              .withBody(Json.stringify(validIndividualResponse(true)))
          )
      )

      val connector = injector.instanceOf[RegistrationConnector]

      val hc: HeaderCarrier = HeaderCarrier(extraHeaders = Seq((headerName, headerValue)))

      connector.registerWithIdIndividual(nino)(hc, scala.concurrent.ExecutionContext.global).map { _ =>
        succeed
      }
    }

  }

  "registerWithNoIdOrganisation" must {
    "return successfully given a valid name and address" in {

      server.stubFor(
        post(urlEqualTo(noIdOrganisationPath))
          .willReturn(
            aResponse()
              .withStatus(Status.OK)
              .withHeader("Content-Type", "application/json")
              .withBody(Json.stringify(validNonUkResponse))
          )
      )

      val connector = injector.instanceOf[RegistrationConnector]
      connector.registerWithNoIdOrganisation(organisation.organisationName, expectedAddress(uk = false).toPrepopAddress, legalStatus).map { registration =>
        registration.sapNumber mustBe sapNumber
      }
    }

    "return successfully with noIdentifier set to true" in {

      server.stubFor(
        post(urlEqualTo(noIdOrganisationPath))
          .willReturn(
            aResponse()
              .withStatus(Status.OK)
              .withHeader("Content-Type", "application/json")
              .withBody(Json.stringify(validNonUkResponse))
          )
      )

      val connector = injector.instanceOf[RegistrationConnector]
      connector.registerWithNoIdOrganisation(organisation.organisationName, expectedAddress(uk = false).toPrepopAddress, legalStatus).map { registration =>
        registration.noIdentifier mustBe true
      }
    }


    "only accept responses with status 200 OK" in {

      server.stubFor(
        post(urlEqualTo(noIdOrganisationPath))
          .willReturn(
            aResponse()
              .withStatus(Status.ACCEPTED)
              .withHeader("Content-Type", "application/json")
          )
      )

      val connector = injector.instanceOf[RegistrationConnector]
      recoverToSucceededIf[UnrecognisedHttpResponseException] {
        connector.registerWithNoIdOrganisation(organisation.organisationName, expectedAddress(uk = false).toPrepopAddress, legalStatus)
      }

    }

    "propagate exceptions from HttpClientV2" in {

      server.stubFor(
        post(urlEqualTo(noIdOrganisationPath))
          .willReturn(
            aResponse()
              .withStatus(Status.NOT_FOUND)
          )
      )

      val connector = injector.instanceOf[RegistrationConnector]
      recoverToSucceededIf[NotFoundException] {
        connector.registerWithNoIdOrganisation(organisation.organisationName, expectedAddress(uk = false).toPrepopAddress, legalStatus)
      }
    }

  }

  "registerWithNoIdIndividual" must {
    "return successfully given a valid name, dob and address" in {

      server.stubFor(
        post(urlEqualTo(noIdIndividualPath))
          .withRequestBody(equalToJson(Json.stringify(registerWithoutIdIndividualRequest)))
          .willReturn(
            aResponse()
              .withStatus(Status.OK)
              .withHeader("Content-Type", "application/json")
              .withBody(Json.stringify(validNonUkResponse))
          )
      )

      val connector = injector.instanceOf[RegistrationConnector]
      connector.registerWithNoIdIndividual(firstName, lastName, expectedAddress(uk = false).toPrepopAddress).map { registration =>
        registration.sapNumber mustBe sapNumber
      }
    }

    "return successfully with noIdentifier equal to true" in {

      server.stubFor(
        post(urlEqualTo(noIdIndividualPath))
          .withRequestBody(equalToJson(Json.stringify(registerWithoutIdIndividualRequest)))
          .willReturn(
            aResponse()
              .withStatus(Status.OK)
              .withHeader("Content-Type", "application/json")
              .withBody(Json.stringify(validNonUkResponse))
          )
      )

      val connector = injector.instanceOf[RegistrationConnector]
      connector.registerWithNoIdIndividual(firstName, lastName, expectedAddress(uk = false).toPrepopAddress).map { registration =>
        registration.noIdentifier mustBe true
      }
    }

    "only accept responses with status 200 OK" in {

      server.stubFor(
        post(urlEqualTo(noIdIndividualPath))
          .willReturn(
            aResponse()
              .withStatus(Status.ACCEPTED)
              .withHeader("Content-Type", "application/json")
          )
      )

      val connector = injector.instanceOf[RegistrationConnector]
      recoverToSucceededIf[UnrecognisedHttpResponseException] {
        connector.registerWithNoIdIndividual(firstName, lastName, expectedAddress(uk = false).toPrepopAddress)
      }

    }


    "propagate exceptions from HttpClientV2" in {

      server.stubFor(
        post(urlEqualTo(noIdIndividualPath))
          .willReturn(
            aResponse()
              .withStatus(Status.NOT_FOUND)
          )
      )

      val connector = injector.instanceOf[RegistrationConnector]
      recoverToSucceededIf[NotFoundException] {
        connector.registerWithNoIdIndividual(firstName, lastName, expectedAddress(uk = false).toPrepopAddress)
      }

    }
  }

}

object RegistrationConnectorSpec extends OptionValues {
  private val utr = "test-utr"
  private val nino = Nino("AB100100A")
  private val sapNumber = "test-sap-number"

  private val organisationPath = "/pension-practitioner/register-with-id/organisation"
  private val noIdOrganisationPath = "/pension-practitioner/register-with-no-id/organisation"
  private val noIdIndividualPath = "/pension-practitioner/register-with-no-id/individual"
  private val individualPath = "/pension-practitioner/register-with-id/individual"

  private val organisation = Organisation("Test Ltd", OrganisationTypeEnum.CorporateBody)
  private val firstName = "John"
  private val lastName = "Doe"
  private val legalStatus = RegistrationLegalStatus.LimitedCompany
  private val registerWithoutIdIndividualRequest = Json.toJson(
    RegistrationNoIdIndividualRequest(firstName, lastName, expectedAddress(uk = false).toPrepopAddress))

  private val expectedIndividual = TolerantIndividual(
    Some("John"),
    Some("T"),
    Some("Doe")
  )

  private def expectedAddress(uk: Boolean) = TolerantAddress(
    Some("Building Name"),
    Some("1 Main Street"),
    Some("Some Village"),
    Some("Some Town"),
    Some("ZZ1 1ZZ"),
    if (uk) Some("GB") else Some("XX")
  )

  private def expectedAddressJson(address: TolerantAddress) = Json.obj(
    "addressLine1" -> address.addressLine1.value,
    "addressLine2" -> address.addressLine2.value,
    "addressLine3" -> address.addressLine3.value,
    "addressLine4" -> address.addressLine4.value,
    "countryCode" -> address.countryOpt.value,
    "postalCode" -> address.postcode.value
  )

  private def validNonUkResponse = Json.obj(
    "safeId" -> "",
    "sapNumber" -> sapNumber
  )

  private def validOrganizationResponse(uk: Boolean) = Json.obj(
    "safeId" -> "",
    "sapNumber" -> sapNumber,
    "isEditable" -> false,
    "isAnAgent" -> false,
    "isAnIndividual" -> false,
    "organisation" -> Json.obj(
      "organisationName" -> organisation.organisationName,
      "organisationType" -> organisation.organisationType.toString
    ),
    "address" -> expectedAddressJson(expectedAddress(uk)),
    "contactDetails" -> Json.obj()
  )

  private def validIndividualResponse(uk: Boolean) = Json.obj(
    "safeId" -> "",
    "sapNumber" -> sapNumber,
    "isEditable" -> false,
    "isAnAgent" -> false,
    "isAnIndividual" -> true,
    "individual" -> Json.obj(
      "firstName" -> expectedIndividual.firstName.value,
      "middleName" -> expectedIndividual.middleName.value,
      "lastName" -> expectedIndividual.lastName.value,
      "dateOfBirth" -> ""
    ),
    "address" -> expectedAddressJson(expectedAddress(uk)),
    "contactDetails" -> Json.obj()
  )

  private val invalidResponse = Json.obj(
    "invalid-element" -> "Meh!"
  )

}
