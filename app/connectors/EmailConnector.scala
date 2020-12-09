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

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import com.google.inject.Inject
import config.FrontendAppConfig
import models.SendEmailRequest
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

sealed trait EmailStatus

case object EmailSent extends EmailStatus

case object EmailNotSent extends EmailStatus

class EmailConnector @Inject()(
    appConfig: FrontendAppConfig,
    http: HttpClient,
    crypto: ApplicationCrypto
) {
  private def callBackUrl(requestId: String, journeyType: String, pspId: String, email: String): String = {
    val encryptedPspId = URLEncoder.encode(crypto.QueryParameterCrypto.encrypt(PlainText(pspId)).value, StandardCharsets.UTF_8.toString)
    val encryptedEmail = URLEncoder.encode(crypto.QueryParameterCrypto.encrypt(PlainText(email)).value, StandardCharsets.UTF_8.toString)

    appConfig.emailCallback(journeyType, requestId, encryptedEmail, encryptedPspId)
  }

  def sendEmail(
      requestId: String,
      pspId: String,
      journeyType: String,
      emailAddress: String,
      templateName: String,
      templateParams: Map[String, String]
  )(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[EmailStatus] = {
    val emailServiceUrl = s"${appConfig.emailApiUrl}/hmrc/email"

    val sendEmailReq = SendEmailRequest(List(emailAddress), templateName, templateParams, appConfig.emailSendForce,
      callBackUrl(requestId, journeyType, pspId, emailAddress))
    val jsonData = Json.toJson(sendEmailReq)

    http.POST[JsValue, HttpResponse](emailServiceUrl, jsonData).map { response =>
      response.status match {
        case ACCEPTED =>
          Logger.debug(s"Email sent successfully for $journeyType")
          EmailSent
        case status =>
          Logger.warn(s"Sending Email failed for $journeyType with response status $status")
          EmailNotSent
      }
    } recoverWith logExceptions
  }

  private def logExceptions: PartialFunction[Throwable, Future[EmailStatus]] = {
    case t: Throwable =>
      Logger.warn("Unable to connect to Email Service", t)
      Future.successful(EmailNotSent)
  }
}
