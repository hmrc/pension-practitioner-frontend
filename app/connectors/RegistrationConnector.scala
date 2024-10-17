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

import com.google.inject.{ImplementedBy, Inject}
import config.FrontendAppConfig
import models.register._
import models.{register, _}
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException, StringContextOps}
import utils.HttpResponseHelper

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

@ImplementedBy(classOf[RegistrationConnectorImpl])
trait RegistrationConnector {
  def registerWithIdOrganisation(utr: String, organisation: Organisation, legalStatus: RegistrationLegalStatus)
                                (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[OrganisationRegistration]

  def registerWithIdIndividual(nino: Nino)
                              (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[IndividualRegistration]

  def registerWithNoIdOrganisation(name: String, address: Address, legalStatus: RegistrationLegalStatus)
                                  (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RegistrationInfo]

  def registerWithNoIdIndividual(firstName: String, lastName: String, address: Address)
                                (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RegistrationInfo]
}

@Singleton
class RegistrationConnectorImpl @Inject()(httpClientV2: HttpClientV2, config: FrontendAppConfig)
  extends RegistrationConnector with HttpResponseHelper {

  private val logger = Logger(classOf[RegistrationConnectorImpl])

  private val readsSapNumber: Reads[String] = (JsPath \ "sapNumber").read[String]

  override def registerWithIdOrganisation(utr: String,
                                          organisation: Organisation,
                                          legalStatus: RegistrationLegalStatus
                                         )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[OrganisationRegistration] = {

    val url = url"${config.registerWithIdOrganisationUrl}"
    val extraHeaders = Seq("utr" -> utr)
    val body = Json.obj(
      "organisationName" -> organisation.organisationName,
      "organisationType" -> organisation.organisationType.toString
    )

    httpClientV2.post(url)
      .withBody(body)
      .setHeader(extraHeaders: _*)
      .execute[HttpResponse] map { response =>
      response.status match {
        case OK =>
          val json = Json.parse(response.body)
          json.validate[OrganisationRegisterWithIdResponse] match {
            case JsSuccess(value, _) =>
              OrganisationRegistration(
                response = value,
                info = registrationInfo(
                  json = json,
                  legalStatus = legalStatus,
                  customerType = RegistrationCustomerType.fromAddress(value.address),
                  idType = Some(RegistrationIdType.UTR), idNumber = Some(utr),
                  noIdentifier = false
                )
              )
            case JsError(errors) => throw JsResultException(errors)
          }
        case _ =>
          handleErrorResponse("POST", url.toString)(response)
      }
    } andThen {
      case Failure(ex: NotFoundException) =>
        logger.warn("Organisation not found with registerWithIdOrganisation", ex)
        ex
      case Failure(ex) =>
        logger.error("Unable to connect to registerWithIdOrganisation", ex)
        ex
    }
  }

  override def registerWithIdIndividual(nino: Nino)
                                       (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[IndividualRegistration] = {

    val url = url"${config.registerWithIdIndividualUrl}"
    val extraHeaders = Seq("nino" -> nino.nino)

    httpClientV2.post(url)
      .withBody(Json.obj())
      .setHeader(extraHeaders: _*)
      .execute[HttpResponse] flatMap { response =>
      response.status match {
        case OK =>
          val json = Json.parse(response.body)
          json.validate[IndividualRegisterWithIdResponse] match {
            case JsSuccess(value, _) =>
              val info = registrationInfo(
                json,
                RegistrationLegalStatus.Individual,
                RegistrationCustomerType.fromAddress(value.address),
                Some(RegistrationIdType.Nino),
                Some(nino.nino),
                noIdentifier = false
              )
              Future.successful(IndividualRegistration(value, info))
            case JsError(errors) => throw JsResultException(errors)
          }
        case _ =>
          handleErrorResponse("POST", url.toString)(response)
      }
    } andThen {
      case Failure(ex: NotFoundException) =>
        logger.warn("Individual not found with registerWithIdIndividual", ex)
        ex
      case Failure(ex) =>
        logger.error("Unable to connect to registerWithIdIndividual", ex)
        ex
    }
  }

    override def registerWithNoIdOrganisation(name: String, address: Address, legalStatus: RegistrationLegalStatus)
                                             (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RegistrationInfo] = {

      val url = url"${config.registerWithNoIdOrganisationUrl}"
      val organisationRegistrant = OrganisationRegistrant(OrganisationName(name), address)

      httpClientV2.post(url)
        .withBody(Json.toJson(organisationRegistrant))
        .execute[HttpResponse] map { response =>
        response.status match {
          case OK =>
            val json = Json.parse(response.body)
            registrationInfo(
              json,
              legalStatus,
              RegistrationCustomerType.NonUK,
              None,
              None,
              noIdentifier = true
            )
          case _ =>
            handleErrorResponse("POST", url.toString)(response)
        }
      } andThen {
        case Failure(ex) =>
          logger.error("Unable to connect to registerWithNoIdOrganisation", ex)
          ex
      }
    }

    override def registerWithNoIdIndividual(firstName: String, lastName: String, address: Address)
                                           (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RegistrationInfo] = {

      val url = url"${config.registerWithNoIdIndividualUrl}"
      val registrant = RegistrationNoIdIndividualRequest(firstName, lastName, address)

      httpClientV2.post(url)
        .withBody(Json.toJson(registrant))
        .execute[HttpResponse] map { response =>
        response.status match {
          case OK =>
            val json = Json.parse(response.body)
            registrationInfo(
              json,
              RegistrationLegalStatus.Individual,
              RegistrationCustomerType.NonUK,
              None,
              None,
              noIdentifier = true
            )
          case _ =>
            handleErrorResponse("POST", url.toString)(response)
        }
      } andThen {
        case Failure(ex) =>
          logger.error("Unable to connect to registerWithNoIdIndividual", ex)
          ex
      }
    }

    private def registrationInfo(json: JsValue,
                                 legalStatus: RegistrationLegalStatus,
                                 customerType: RegistrationCustomerType,
                                 idType: Option[RegistrationIdType],
                                 idNumber: Option[String],
                                 noIdentifier: Boolean): RegistrationInfo = {

        json.validate[String](readsSapNumber) match {
          case JsSuccess(sapNumber, _) =>
            register.RegistrationInfo(legalStatus, sapNumber, noIdentifier = noIdentifier, customerType, idType, idNumber)
          case JsError(errors) => throw JsResultException(errors)
        }
      }

  }
