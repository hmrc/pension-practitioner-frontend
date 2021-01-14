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

import com.google.inject.{ImplementedBy, Inject}
import config.FrontendAppConfig
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json._
import uk.gov.hmrc.http.{HttpResponse, HeaderCarrier}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.HttpResponseHelper
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{Future, ExecutionContext}
import scala.util.Failure

@ImplementedBy(classOf[DeregistrationConnectorImpl])
trait DeregistrationConnector {
  def deregister(pspId: String, date: LocalDate)(implicit hc: HeaderCarrier, ec: ExecutionContext) : Future[HttpResponse]

  def canDeRegister(psaId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean]
}

class DeregistrationConnectorImpl @Inject()(http: HttpClient, config: FrontendAppConfig) extends DeregistrationConnector with HttpResponseHelper {
  override def deregister(pspId: String, date: LocalDate)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val deregisterUrl = config.pspDeregistrationUrl.format(pspId)
    val data: JsObject = Json.obj(
      "deregistrationDate"-> date.toString,
      "reason" -> "1"
    )

      http.POST[JsObject, HttpResponse](deregisterUrl, data).map { response =>
      response.status match {
        case OK => response
        case _ => handleErrorResponse("POST", deregisterUrl)(response)
      }
    } andThen {
      case Failure(t: Throwable) => Logger.warn("Unable to deregister PSP", t)
    }
  }

  override def canDeRegister(pspId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {

    val url = config.canDeregisterUrl.format(pspId)

    http.GET[HttpResponse](url).map { response =>
      response.status match {
        case OK => response.json.validate[Boolean] match {
          case JsSuccess(value, _) => value
          case JsError(errors) => throw JsResultException(errors)
        }
        case NOT_FOUND =>
          Logger.debug(s"CanDeregister call returned a NOT_FOUND response with body ${response.body}")
          true
        case _ => handleErrorResponse("GET", url)(response)
      }


    } andThen {
      case Failure(t: Throwable) => Logger.warn("Unable to get the response from can de register api", t)
    }
  }
}
