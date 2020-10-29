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

package connectors

import audit.{AuditService, PSPEnrolment}
import com.google.inject.{ImplementedBy, Singleton}
import config.FrontendAppConfig
import javax.inject.Inject
import models.KnownFacts
import models.requests.DataRequest
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{Json, Writes}
import play.api.mvc.AnyContent
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.{HttpResponseHelper, RetryHelper}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

@ImplementedBy(classOf[EnrolmentConnectorImpl])
trait EnrolmentConnector {
  def enrol(enrolmentKey: String, knownFacts: KnownFacts)
           (implicit w: Writes[KnownFacts], hc: HeaderCarrier, executionContext: ExecutionContext, request: DataRequest[AnyContent]): Future[HttpResponse]

}

@Singleton
class EnrolmentConnectorImpl @Inject()(val http: HttpClient,
                                           config: FrontendAppConfig,
                                           auditService: AuditService)
  extends EnrolmentConnector
    with RetryHelper
    with HttpResponseHelper {

  def url: String = config.taxEnrolmentsUrl.format("HMRC-PODSPP-ORG")

  override def enrol(enrolmentKey: String, knownFacts: KnownFacts)
                    (implicit w: Writes[KnownFacts],
                     hc: HeaderCarrier,
                     executionContext: ExecutionContext,
                     request: DataRequest[AnyContent]): Future[HttpResponse] = {
    retryOnFailure(() => enrolmentRequest(enrolmentKey, knownFacts), config)
  } andThen {
    logExceptions(knownFacts)
  }

  private def enrolmentRequest(enrolmentKey: String, knownFacts: KnownFacts)
                              (implicit w: Writes[KnownFacts], hc: HeaderCarrier, executionContext: ExecutionContext,
                               request: DataRequest[AnyContent]): Future[HttpResponse] = {

    http.PUT[KnownFacts, HttpResponse](url, knownFacts) flatMap {
      response =>
        response.status match {
          case NO_CONTENT =>
            auditService.sendEvent(PSPEnrolment(request.externalId, enrolmentKey))
            Future.successful(response)
          case _ =>
            if (response.body.contains("INVALID_JSON")) Logger.warn(s"INVALID_JSON returned from call to $url")
            handleErrorResponse("PUT", url)(response)
        }
    }
  }


  private def logExceptions(knownFacts: KnownFacts): PartialFunction[Try[HttpResponse], Unit] = {
    case Failure(t: Throwable) =>
      Logger.error("Unable to connect to Tax Enrolments", t)
      Logger.debug(s"Known Facts: ${Json.toJson(knownFacts)}")
  }
}
