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
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import pages.company._
import pages.register.AreYouUKCompanyPage
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import uk.gov.hmrc.govukfrontend.views.Aliases.{Key, SummaryListRow, Value}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions}

class CompanyCYAServiceSpec extends AnyWordSpec with MockitoSugar with BeforeAndAfterEach with CYAService with Matchers {

  private implicit val messages: Messages = stubMessages()

  val service: CompanyCYAService = new CompanyCYAService
  private val companyName: String = "Company Name"
  private val utr: String = "1234567890"
  private val address: Address = Address("line1", "line2", Some("line3"), Some("line4"), Some("zz1 1zz"), "GB")
  private val email: String = "a@b.c"
  private val phone: String = "1111111111"

  val userAnswersUK: UserAnswers = UserAnswers()
    .setOrException(AreYouUKCompanyPage, true)
    .setOrException(BusinessNamePage, companyName)
    .setOrException(BusinessUTRPage, utr)
    .setOrException(CompanyAddressPage, address)
    .setOrException(CompanyEmailPage, email)
    .setOrException(CompanyPhonePage, phone)

  val userAnswersNonUK: UserAnswers = UserAnswers()
    .setOrException(AreYouUKCompanyPage, false)
    .setOrException(BusinessNamePage, companyName)
    .setOrException(CompanyAddressPage, address)
    .setOrException(CompanyEmailPage, email)
    .setOrException(CompanyPhonePage, phone)

  val expectedRowsUK: Seq[SummaryListRow] = Seq(
    SummaryListRow(
      key = Key(Text(Messages("cya.companyName")), classes = "govuk-!-width-one-half"),
      value = Value(Text(companyName), classes = "govuk-!-width-one-third")
    ),
    SummaryListRow(
      key = Key(Text(Messages("cya.utr")), classes = "govuk-!-width-one-half"),
      value = Value(Text(utr), classes = "govuk-!-width-one-third")
    ),
    SummaryListRow(
      key = Key(Text(Messages("cya.address")), classes = "govuk-!-width-one-half"),
      value = Value(addressAnswer(address), classes = "govuk-!-width-one-third"),
      actions = Some(
        Actions(
          items = Seq(ActionItem(
          content = HtmlContent(s"""<span aria-hidden="true">${Messages("site.edit")}</span>"""),
          href = controllers.company.routes.CompanyPostcodeController.onPageLoad(CheckMode).url,
          visuallyHiddenText = Some(Messages("cya.change.address"))
        )
      )
    ))),
    SummaryListRow(
      key = Key(Text(Messages("cya.email",companyName)), classes = "govuk-!-width-one-half"),
      value = Value(Text(email), classes = "govuk-!-width-one-third"),
      actions = Some(
        Actions(
          items = Seq(ActionItem(
          content = HtmlContent(s"""<span aria-hidden="true">${Messages("site.edit")}</span>"""),
          href = controllers.company.routes.CompanyEmailController.onPageLoad(CheckMode).url,
          visuallyHiddenText = Some(Messages("cya.change.email",companyName)))
        )
      )
    )),
    SummaryListRow(
      key = Key(Text(Messages("cya.phone",companyName)), classes = "govuk-!-width-one-half"),
      value = Value(Text(phone), classes = "govuk-!-width-one-third"),
      actions = Some(
        Actions(
          items = Seq(ActionItem(
          content = HtmlContent(s"""<span aria-hidden="true">${Messages("site.edit")}</span>"""),
          href = controllers.company.routes.CompanyPhoneController.onPageLoad(CheckMode).url,
          visuallyHiddenText = Some(Messages("cya.change.phone",companyName)))
        )
      )
    )
  ))

  val expectedRowsNonUK: Seq[SummaryListRow] = Seq(
    SummaryListRow(
      key = Key(Text(Messages("cya.companyName")), classes = "govuk-!-width-one-half"),
      value = Value(Text(companyName), classes = "govuk-!-width-one-third")
    ),
    SummaryListRow(
      key = Key(Text(Messages("cya.address")), classes = "govuk-!-width-one-half"),
      value = Value(addressAnswer(address), classes = "govuk-!-width-one-third"),
      actions = Some(
        Actions(
          items = Seq(ActionItem(
          content = HtmlContent(s"""<span aria-hidden="true">${Messages("site.edit")}</span>"""),
          href = controllers.company.routes.CompanyContactAddressController.onPageLoad(CheckMode).url,
          visuallyHiddenText = Some(Messages("cya.change.address"))
        )
      )
    ))),
    SummaryListRow(
      key = Key(Text(Messages("cya.email",companyName)), classes = "govuk-!-width-one-half"),
      value = Value(Text(email), classes = "govuk-!-width-one-third"),
      actions = Some(
        Actions(
          items = Seq(ActionItem(
          content = HtmlContent(s"""<span aria-hidden="true">${Messages("site.edit")}</span>"""),
          href = controllers.company.routes.CompanyEmailController.onPageLoad(CheckMode).url,
          visuallyHiddenText = Some(Messages("cya.change.email",companyName))
        )
      )
    ))),
    SummaryListRow(
      key = Key(Text(Messages("cya.phone",companyName)), classes = "govuk-!-width-one-half"),
      value = Value(Text(phone), classes = "govuk-!-width-one-third"),
      actions = Some(
        Actions(
          items = Seq(ActionItem(
          content = HtmlContent(s"""<span aria-hidden="true">${Messages("site.edit")}</span>"""),
          href = controllers.company.routes.CompanyPhoneController.onPageLoad(CheckMode).url,
          visuallyHiddenText = Some(Messages("cya.change.phone",companyName))
        )
      )
    )
  )))

  "companyCya" must {
    "return a list of rows of company cya details for UK" in {
      service.companyCya(userAnswersUK) mustBe expectedRowsUK
    }
    "return a list of rows of company cya details for non UK" in {
      service.companyCya(userAnswersNonUK) mustBe expectedRowsNonUK
    }
  }

}
