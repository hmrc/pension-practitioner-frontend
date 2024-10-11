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
import pages.partnership._
import pages.register.AreYouUKCompanyPage
import play.api.i18n.Messages
import play.api.mvc.Call
import uk.gov.hmrc.govukfrontend.views.Aliases.{SummaryListRow, Value}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions, Key}

class PartnershipCYAService extends CYAService {

  def partnershipCya(ua: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] = {
    ua.get(AreYouUKCompanyPage) match {
      case Some(true) =>
        (
          ua.get(BusinessNamePage),
          ua.get(BusinessUTRPage),
          ua.get(PartnershipAddressPage),
          ua.get(PartnershipEmailPage),
          ua.get(PartnershipPhonePage)
        ) match {
          case (Some(name), Some(utr), Some(address), Some(email), Some(phone)) =>
              Seq(
                partnershipName(name),
                partnershipUtr(utr),
                partnershipAddress(address,
                  controllers.partnership.routes.PartnershipPostcodeController.onPageLoad(CheckMode)),
                partnershipEmail(name, email),
                partnershipPhone(name, phone)
              )
          case _ => Seq.empty
        }
      case Some(false) =>
        (
          ua.get(BusinessNamePage),
          ua.get(PartnershipAddressPage),
          ua.get(PartnershipEmailPage),
          ua.get(PartnershipPhonePage)
        ) match {
          case (Some(name), Some(address), Some(email), Some(phone)) =>
            Seq(
              partnershipName(name),
              partnershipAddress(address,
                controllers.partnership.routes.PartnershipContactAddressController.onPageLoad(CheckMode)),
              partnershipEmail(name, email),
              partnershipPhone(name, phone)
            )
          case _ => Seq.empty
        }
      case _ => Seq.empty
    }
  }

  private def partnershipName(name: String)(implicit messages: Messages): SummaryListRow = {
    SummaryListRow(key = Key(content = Text(Messages("cya.partnershipName")),
      classes = "govuk-!-width-one-half"),
      value = Value(Text(Messages(name)), classes = "govuk-!-width-one-third"))
  }

  private def partnershipUtr(utr: String)(implicit messages: Messages): SummaryListRow =
    SummaryListRow(
      key = Key(Text(Messages("cya.utr")), classes = "govuk-!-width-one-half"),
      value = Value(Text(Messages(utr)), classes = "govuk-!-width-one-third")
    )

  private def partnershipAddress(address: Address, href: Call)
                                (implicit messages: Messages): SummaryListRow =
    SummaryListRow(
      key = Key(Text(Messages("cya.address")), classes = "govuk-!-width-one-half"),
      value = Value(addressAnswer(address), classes = "govuk-!-width-one-third"),
      actions = Some(
        Actions(
          items = Seq(ActionItem(
            content = HtmlContent(s"""<span aria-hidden="true">${Messages("site.edit")}</span>"""),
            href = href.url,
            visuallyHiddenText = Some(Text(Messages("cya.change.address")).value)
          )
          )
        )
      )
    )

  private def partnershipEmail(partnershipName: String, email: String)
                              (implicit messages: Messages): SummaryListRow =
    SummaryListRow(
      key = Key(Text(Messages("cya.email", partnershipName)), classes = "govuk-!-width-one-half"),
      value = Value(Text(email), classes = "govuk-!-width-one-third"),
      actions = Some(
        Actions(
          items = Seq(ActionItem(
            content = HtmlContent(s"""<span aria-hidden="true">${Messages("site.edit")}</span>"""),
            href = controllers.partnership.routes.PartnershipEmailController.onPageLoad(CheckMode).url,
            visuallyHiddenText = Some(Text(Messages("cya.change.email", partnershipName)).value
            )
          )
          )
        )
      )
    )

  private def partnershipPhone(partnershipName: String, phone: String)
                              (implicit messages: Messages): SummaryListRow = {
    SummaryListRow(
      key = Key(Text(Messages("cya.phone", partnershipName)), classes = "govuk-!-width-one-half"),
      value = Value(Text(phone), classes = "govuk-!-width-one-third"),
      actions = Some(
        Actions(
          items = Seq(ActionItem(
            content = HtmlContent(s"""<span aria-hidden="true">${Messages("site.edit")}</span>"""),
            href = controllers.partnership.routes.PartnershipPhoneController.onPageLoad(CheckMode).url,
            visuallyHiddenText = Some(Text(Messages("cya.change.phone", partnershipName)).value)
          )
          )
        )
      )
    )
  }
}
