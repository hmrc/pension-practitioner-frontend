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

import com.google.inject.Inject
import config.FrontendAppConfig
import models._
import play.Logger
import play.api.http.Status.OK
import play.api.libs.json._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.HttpResponseHelper

import scala.concurrent.{ExecutionContext, Future}

class SubscriptionConnector @Inject()(http: HttpClient,
                                      config: FrontendAppConfig) extends HttpResponseHelper {

  def subscribePsp(answers: UserAnswers)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[String] = {
      val url = config.pspSubscriptionUrl
    http.POST[JsObject, HttpResponse](url, answers.data).map {
      response =>
        response.status match {
          case OK =>
            (response.json \ "pspid").validate[String] match {
                case JsSuccess(value, _) => value
                case JsError(errors) => throw JsResultException(errors)
          }
          case _ =>
            Logger.warn("Unable to post psp subscription")
            handleErrorResponse("POST", url)(response)
        }
    }
  }


}
