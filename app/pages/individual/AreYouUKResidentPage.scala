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

package pages.individual

import models.UserAnswers
import pages.{QuestionPage, RegistrationInfoPage}
import play.api.libs.json.JsPath

import scala.util.Try

case object AreYouUKResidentPage extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "areYouUKResident"

  override def cleanup(value: Option[Boolean], userAnswers: UserAnswers): Try[UserAnswers] = {
    val result = value match {
      case Some(false) =>
        userAnswers
          .remove(IndividualDetailsPage).toOption.getOrElse(userAnswers)
          .remove(IndividualAddressPage).toOption.getOrElse(userAnswers)
          .remove(RegistrationInfoPage).toOption.getOrElse(userAnswers)
          .remove(IndividualPostcodePage).toOption.getOrElse(userAnswers)
          .remove(IndividualAddressListPage).toOption.getOrElse(userAnswers)
          .remove(IndividualManualAddressPage).toOption.getOrElse(userAnswers)
          .remove(IndividualEmailPage).toOption.getOrElse(userAnswers)
          .remove(IsThisYouPage).toOption.getOrElse(userAnswers)
          .remove(UseAddressForContactPage).toOption.getOrElse(userAnswers)
          .remove(IndividualPhonePage).toOption
      case Some(true) =>
        userAnswers
          .remove(IndividualDetailsPage).toOption.getOrElse(userAnswers)
          .remove(IndividualAddressPage).toOption.getOrElse(userAnswers)
          .remove(RegistrationInfoPage).toOption.getOrElse(userAnswers)
          .remove(IndividualManualAddressPage).toOption.getOrElse(userAnswers)
          .remove(IndividualEmailPage).toOption.getOrElse(userAnswers)
          .remove(IsThisYouPage).toOption.getOrElse(userAnswers)
          .remove(UseAddressForContactPage).toOption.getOrElse(userAnswers)
          .remove(IndividualPhonePage).toOption
      case _ => None
    }
    super.cleanup(value, result.getOrElse(userAnswers))
  }
}
