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
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import utils.HttpResponseHelper

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

@ImplementedBy(classOf[DeregistrationConnectorImpl])
trait DeregistrationConnector {
  def deregister(date: LocalDate
                )(implicit hc: HeaderCarrier, ec: ExecutionContext) : Future[HttpResponse]

  def canDeRegister(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean]
}

class DeregistrationConnectorImpl @Inject()(httpClientV2: HttpClientV2, config: FrontendAppConfig)
  extends DeregistrationConnector
    with HttpResponseHelper {

  private val logger = Logger(classOf[DeregistrationConnectorImpl])

  override def deregister(date: LocalDate)
                         (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val deregisterUrl = url"${config.pspDeregistrationUrl}"
    val data: JsObject = Json.obj(
      "deregistrationDate"-> date.toString,
      "reason" -> "1"
    )

    httpClientV2.post(deregisterUrl)
      .withBody(data)
      .execute[HttpResponse] map { response =>
      response.status match {
        case OK => response
        case _ => handleErrorResponse("POST", deregisterUrl.toString)(response)
      }
    } andThen {
        case Failure(t: Throwable) => logger.warn("Unable to deregister PSP", t)
      }

 }

  override def canDeRegister
                            (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {

    val url = url"${config.canDeregisterUrl}"

    httpClientV2.get(url)
      .execute[HttpResponse] map { response =>
      response.status match {
        case OK => response.json.validate[Boolean] match {
          case JsSuccess(value, _) => value
          case JsError(errors) => throw JsResultException(errors)
        }
        case NOT_FOUND =>
          logger.debug(s"CanDeregister call returned a NOT_FOUND response with body ${response.body}")
          true
        case _ => handleErrorResponse("GET", url.toString)(response)
      }
   } andThen {
      case Failure(t: Throwable) => logger.warn("Unable to get the response from can de register api", t)
    }
  }

}
