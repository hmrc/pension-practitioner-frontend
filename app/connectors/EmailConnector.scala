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
import models.SendEmailRequest
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

sealed trait EmailStatus

case object EmailSent extends EmailStatus

case object EmailNotSent extends EmailStatus

class EmailConnector @Inject()(
    appConfig: FrontendAppConfig,
    http: HttpClient,
    crypto: ApplicationCrypto
) {
  private def callBackUrl(requestId: String, journeyType: String, pspId: String, email: String): String = {
    val encryptedPsaId = crypto.QueryParameterCrypto.encrypt(PlainText(pspId)).value
    val encryptedEmail = crypto.QueryParameterCrypto.encrypt(PlainText(email)).value

    appConfig.emailCallback(journeyType, requestId, encryptedEmail, encryptedPsaId)
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

    println( "\n>>>SNEDING:" + jsonData)

    http.POST[JsValue, HttpResponse](emailServiceUrl, jsonData).map { response =>
      response.status match {
        case ACCEPTED =>
          println( "\n>>>EMAIL SENT")
          Logger.debug(s"Email sent successfully for $journeyType")
          EmailSent
        case status =>
          println( "\n>>>EMAIL NOT SENT:" + status)
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
