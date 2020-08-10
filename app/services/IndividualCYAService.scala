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

package services

import models.{Address, CheckMode, UserAnswers}
import pages.individual._
import play.api.i18n.Messages
import uk.gov.hmrc.viewmodels.SummaryList.{Action, Key, Row, Value}
import uk.gov.hmrc.viewmodels.Text.Literal
import uk.gov.hmrc.viewmodels._

class IndividualCYAService extends CYAService {

  def individualCya(ua: UserAnswers)(implicit messages: Messages): Seq[Row] = {
    (
      ua.get(IndividualDetailsPage),
      ua.get(IndividualManualAddressPage),
      ua.get(IndividualEmailPage),
      ua.get(IndividualPhonePage),
      ua.get(AreYouUKResidentPage)
    ) match {
      case (Some(details), Some(address), Some(email), Some(phone), Some(areYouUKResident)) =>
        Seq(
          individualName(details.fullName),
          individualAddress(address, areYouUKResident),
          individualEmail(email),
          individualPhone(phone)
        )
      case _ => Seq.empty

    }
  }

  private def individualName(name: String): Row =
    Row(
      key = Key(msg"cya.name", classes = Seq("govuk-!-width-one-half")),
      value = Value(Literal(name), classes = Seq("govuk-!-width-one-third"))
    )

  private def individualAddress(address: Address, areYouUKResident: Boolean)(implicit messages: Messages): Row =
    Row(
      key = Key(msg"cya.address", classes = Seq("govuk-!-width-one-half")),
      value = Value(addressAnswer(address), classes = Seq("govuk-!-width-one-third")),
      actions = List(
        Action(
          content = msg"site.edit",
          href =
            if (areYouUKResident) {
              controllers.individual.routes.IndividualPostcodeController.onPageLoad(CheckMode).url
            } else {
              controllers.individual.routes.IndividualNonUKAddressController.onPageLoad(CheckMode).url
            },
          visuallyHiddenText = Some(msg"cya.change.address")
        )
      )
    )

  private def individualEmail(email: String): Row =
    Row(
      key = Key(msg"cya.individual.email", classes = Seq("govuk-!-width-one-half")),
      value = Value(Literal(email), classes = Seq("govuk-!-width-one-third")),
      actions = List(
        Action(
          content = msg"site.edit",
          href = controllers.individual.routes.IndividualEmailController.onPageLoad(CheckMode).url,
          visuallyHiddenText = Some(msg"cya.individual.change.email")
        )
      )
    )

  private def individualPhone(phone: String): Row =
    Row(
      key = Key(msg"cya.individual.phone", classes = Seq("govuk-!-width-one-half")),
      value = Value(Literal(phone), classes = Seq("govuk-!-width-one-third")),
      actions = List(
        Action(
          content = msg"site.edit",
          href = controllers.individual.routes.IndividualPhoneController.onPageLoad(CheckMode).url,
          visuallyHiddenText = Some(msg"cya.individual.change.phone")
        )
      )
    )
}
