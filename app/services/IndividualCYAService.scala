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

package services

import models.{Address, CheckMode, UserAnswers}
import pages.individual._
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{SummaryListRow, Value}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions, Key}

class IndividualCYAService extends CYAService {

  def individualCya(ua: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] = {
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

  private def individualName(name: String)(implicit messages: Messages): SummaryListRow =
    SummaryListRow(
      key = Key(Text(Messages("cya.name")), classes = "govuk-!-width-one-half"),
      value = Value(Text(name), classes = "govuk-!-width-one-third")
    )

  private def individualAddress(address: Address, areYouUKResident: Boolean)
                               (implicit messages: Messages): SummaryListRow =
    SummaryListRow(
      key = Key(Text(Messages("cya.address")), classes = "govuk-!-width-one-half"),
      value = Value(addressAnswer(address), classes = "govuk-!-width-one-third"),
      actions = Some(
        Actions(
          items = Seq(ActionItem(
            content = HtmlContent(s"""<span aria-hidden="true">${Messages("site.edit")}</span>"""),
            href =
              if (areYouUKResident) {
                controllers.individual.routes.IndividualPostcodeController.onPageLoad(CheckMode).url
              } else {
                controllers.individual.routes.IndividualContactAddressController.onPageLoad(CheckMode).url
              },
            visuallyHiddenText = Some(Messages("cya.change.address"))
          )
          )
        )
      )
    )

  private def individualEmail(email: String)
                             (implicit messages: Messages): SummaryListRow = {
    SummaryListRow(
      key = Key(Text(Messages("cya.individual.email")), classes = "govuk-!-width-one-half"),
      value = Value(Text(email), classes = "govuk-!-width-one-third"),
      actions = Some(
        Actions(
          items = Seq(ActionItem(
            content = HtmlContent(s"""<span aria-hidden="true">${Messages("site.edit")}</span>"""),
            href = controllers.individual.routes.IndividualEmailController.onPageLoad(CheckMode).url,
            visuallyHiddenText = Some(Messages("cya.individual.change.email"))
          )
          )
        )
      )
    )
  }

  private def individualPhone(phone: String)
                             (implicit messages: Messages): SummaryListRow = {
    SummaryListRow(
      key = Key(Text(Messages("cya.individual.phone")), classes = "govuk-!-width-one-half"),
      value = Value(Text(phone), classes = "govuk-!-width-one-third"),
      actions = Some(
        Actions(
          items = Seq(ActionItem(
            content = HtmlContent(s"""<span aria-hidden="true">${Messages("site.edit")}</span>"""),
            href = controllers.individual.routes.IndividualPhoneController.onPageLoad(CheckMode).url,
            visuallyHiddenText = Some(Text(Messages("cya.individual.change.phone")).value)
          )
          )
        )
      )
    )
  }
}
