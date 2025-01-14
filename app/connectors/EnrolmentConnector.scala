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

import audit.{AuditService, PSPDeenrolment, PSPEnrolmentFailure, PSPEnrolmentSuccess}
import com.google.inject.{ImplementedBy, Singleton}
import config.FrontendAppConfig
import models.KnownFacts
import models.requests.DataRequest
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{AnyContent, RequestHeader}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import utils.{HttpResponseHelper, RetryHelper}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

@ImplementedBy(classOf[EnrolmentConnectorImpl])
trait EnrolmentConnector {
  def enrol(enrolmentKey: String, knownFacts: KnownFacts)
           (implicit w: Writes[KnownFacts], hc: HeaderCarrier, executionContext: ExecutionContext, request: DataRequest[AnyContent]): Future[HttpResponse]

  def deEnrol(groupId: String, pspId: String, userId: String)
             (implicit hc: HeaderCarrier, ec: ExecutionContext, rh: RequestHeader): Future[HttpResponse]

}

@Singleton
class EnrolmentConnectorImpl @Inject()(
  val httpClientV2: HttpClientV2,
  config: FrontendAppConfig,
  auditService: AuditService
) extends EnrolmentConnector with RetryHelper with HttpResponseHelper {

  private val logger = Logger(classOf[EnrolmentConnectorImpl])

  override def enrol(enrolmentKey: String, knownFacts: KnownFacts
                    )(implicit w: Writes[KnownFacts],
                      hc: HeaderCarrier,
                      executionContext: ExecutionContext,
                      request: DataRequest[AnyContent]): Future[HttpResponse] =
    retryOnFailure(() => enrolmentRequest(enrolmentKey, knownFacts), config) andThen
      logExceptions(knownFacts)

  private def enrolmentRequest(enrolmentKey: String, knownFacts: KnownFacts
                              )(implicit hc: HeaderCarrier,
                                executionContext: ExecutionContext,
                                request: DataRequest[AnyContent]): Future[HttpResponse] = {
    val url = url"""${config.taxEnrolmentsUrl.format("HMRC-PODSPP-ORG")}"""

    httpClientV2.put(url)
      .withBody(knownFacts)
      .execute[HttpResponse] flatMap { response =>
      response.status match {
        case NO_CONTENT =>
          auditService.sendEvent(PSPEnrolmentSuccess(request.externalId, enrolmentKey))
          Future.successful(response)
        case statusCode =>
          auditService.sendEvent(PSPEnrolmentFailure(request.externalId, enrolmentKey, statusCode))
          if (response.body.contains("INVALID_JSON")) logger.warn(s"INVALID_JSON returned from call to $url")
          handleErrorResponse("PUT", url.toString)(response)
      }
    }
  }

  private def logExceptions(knownFacts: KnownFacts): PartialFunction[Try[HttpResponse], Unit] = {
    case Failure(t: Throwable) =>
      logger.error("Unable to connect to Tax Enrolments", t)
      logger.debug(s"Known Facts: ${Json.toJson(knownFacts)}")
  }

  override def deEnrol(groupId: String, pspId: String, userId: String
                      )(implicit hc: HeaderCarrier, ec: ExecutionContext, rh: RequestHeader): Future[HttpResponse] = {
    retryOnFailure(
      f = () => deEnrolmentRequest(groupId, pspId, userId),
      config = config
    )
  } andThen {
    logDeEnrolmentExceptions
  }

  private def deEnrolmentRequest(groupId: String, pspId: String, userId: String
                                )(implicit hc: HeaderCarrier, ec: ExecutionContext, rh: RequestHeader): Future[HttpResponse] = {

    val enrolmentKey = s"HMRC-PODSPP-ORG~PSPID~$pspId"
    val deEnrolmentUrl = url"${config.taxDeEnrolmentUrl.format(groupId, enrolmentKey)}"

    httpClientV2.delete(deEnrolmentUrl)
    .execute[HttpResponse] flatMap {
      case response if response.status equals NO_CONTENT =>
        auditService.sendEvent(PSPDeenrolment(userId, pspId))
        Future.successful(response)
      case response =>
        Future.failed(new HttpException(response.body, response.status))
    }
  }

  private def logDeEnrolmentExceptions: PartialFunction[Try[HttpResponse], Unit] = {
    case Failure(t: Throwable) =>
      logger.error("Unable to connect to Tax Enrolments to de enrol the PSA", t)
  }
}
