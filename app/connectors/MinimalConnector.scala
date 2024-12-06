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
import models.MinimalPSP
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{JsError, JsResultException, JsSuccess, Json}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import utils.HttpResponseHelper

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

@ImplementedBy(classOf[MinimalConnectorImpl])
trait MinimalConnector {

  def getMinimalPspDetails()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[MinimalPSP]

}

class MinimalConnectorImpl @Inject()(httpClientV2: HttpClientV2, config: FrontendAppConfig)
  extends MinimalConnector
    with HttpResponseHelper {

  private val logger = Logger(classOf[MinimalConnectorImpl])

  override def getMinimalPspDetails()
                                   (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[MinimalPSP] = {

    val url = url"${config.minimalDetailsUrl}"
    val headers: Seq[(String, String)] = Seq(("loggedInAsPsp", "true"))

    httpClientV2.get(url)
      .setHeader(headers: _*)
      .execute[HttpResponse].map { response =>
        response.status match {
          case OK =>
            Json.parse(response.body).validate[MinimalPSP] match {
              case JsSuccess(value, _) => value
              case JsError(errors) =>
                logger.error(s"JSON validation failed: $errors, response body: ${response.body}")
                throw JsResultException(errors)
            }

          case _ => handleErrorResponse("GET", config.minimalDetailsUrl)(response)
        }
      }  andThen {
           case Failure(t: Throwable) => logger.warn("Unable get minimal details", t)
         }
  }

}
