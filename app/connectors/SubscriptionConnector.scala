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

import com.google.inject.Inject
import config.FrontendAppConfig
import models.{JourneyType, _}
import play.api.Logger
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, OK}
import play.api.libs.json._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import utils.HttpResponseHelper

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

class SubscriptionConnector @Inject()(http: HttpClient, config: FrontendAppConfig)
  extends HttpResponseHelper {

  private val logger = Logger(classOf[SubscriptionConnector])

  def subscribePsp(answers: UserAnswers, journeyType: JourneyType.Name)
                  (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[String] = {

    val url = config.pspSubscriptionUrl.format(journeyType.toString)
    http.POST[JsObject, HttpResponse](url, answers.data).map {
      response =>
        response.status match {
          case OK =>
            (response.json \ "pspid").validate[String] match {
              case JsSuccess(value, _) => value
              case JsError(errors) => throw JsResultException(errors)
            }
          case _ =>
            logger.warn("Unable to post psp subscription")
            handleErrorResponse("POST", url)(response)
        }
    }
  }

  def getSubscriptionDetails(pspId: String)
                            (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[JsValue] = {

    val pspIdHC = hc.withExtraHeaders("pspId" -> pspId)
    val url = config.subscriptionDetailsUrl

    http.GET[HttpResponse](url)(implicitly, pspIdHC, implicitly) map { response =>
      response.status match {
        case OK =>
          response.json
        case BAD_REQUEST if response.body.contains("INVALID_IDVALUE") =>
          throw new PspIdInvalidSubscriptionException
        case BAD_REQUEST if response.body.contains("INVALID_CORRELATIONID") =>
          throw new CorrelationIdInvalidSubscriptionException
        case NOT_FOUND =>
          throw new PspIdNotFoundSubscriptionException
        case _ =>
          handleErrorResponse("GET", config.subscriptionDetailsUrl)(response)
      }
    } andThen {
      case Failure(t: Throwable) => logger.warn("Unable to get PSP subscription details", t)
    }
  }

  def getPspApplicationDate(pspId: String)
                           (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[LocalDate] =

    getSubscriptionDetails(pspId).map { jsValue =>
      (jsValue \ "applicationDate").validate[String] match {
        case JsSuccess(value, _) =>
          value
            .split("T")
            .headOption
            .fold(throw ApplicationDateCannotBeRetrieved)(date => LocalDate.parse(date))
        case JsError(e) =>
          throw JsResultException(e)
      }
    }

}

abstract class SubscriptionException extends Exception

class PspIdInvalidSubscriptionException extends SubscriptionException

class CorrelationIdInvalidSubscriptionException extends SubscriptionException

class PspIdNotFoundSubscriptionException extends SubscriptionException

case object ApplicationDateCannotBeRetrieved extends Exception
