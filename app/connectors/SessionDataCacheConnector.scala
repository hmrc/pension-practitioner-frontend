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

import com.google.inject.Inject
import config.FrontendAppConfig
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.{ExecutionContext, Future}

class SessionDataCacheConnector  @Inject()(
  config: FrontendAppConfig,
  httpClientV2: HttpClientV2
) {

  private def url() =
    url"${config.pensionAdministratorUrl}/pension-administrator/journey-cache/session-data-self"

  def fetch()(implicit ec: ExecutionContext,
                        headerCarrier: HeaderCarrier): Future[Option[JsValue]] = {
    val headers: Seq[(String, String)] = Seq(("Content-Type", "application/json"))

    httpClientV2.get(url())
      .setHeader(headers: _*)
      .execute[HttpResponse] map { response =>
      response.status match {
        case NOT_FOUND =>
          None
        case OK =>
          Some(Json.parse(response.body))
        case _ =>
          throw new HttpException(response.body, response.status)
      }
    } recoverWith mapExceptionsToStatus
  }

  def removeAll()(implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): Future[Result] = {
    val headers: Seq[(String, String)] = Seq(("Content-Type", "application/json"))

    httpClientV2.delete(url())
      .setHeader(headers: _*)
      .execute[HttpResponse] map { _ =>
          Ok
      }
  }

  private def mapExceptionsToStatus: PartialFunction[Throwable, Future[Option[JsValue]]] = {
    case _: NotFoundException =>
      Future.successful(None)
  }

}
