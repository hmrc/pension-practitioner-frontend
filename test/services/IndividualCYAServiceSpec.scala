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

import models.register.TolerantIndividual
import models.{Address, CheckMode, UserAnswers}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.individual._
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import uk.gov.hmrc.govukfrontend.views.Aliases.{Key, SummaryListRow, Value}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions}
import uk.gov.hmrc.viewmodels.Text.Literal
import uk.gov.hmrc.viewmodels._

class IndividualCYAServiceSpec
  extends AnyWordSpec with MockitoSugar with BeforeAndAfterEach with CYAService with Matchers with OptionValues {

  private implicit val messages: Messages = stubMessages()

  private val service: IndividualCYAService = new IndividualCYAService
  private val address: Address = Address("line1", "line2", Some("line3"), Some("line4"), Some("zz1 1zz"), "GB")
  private val email: String = "a@b.c"
  private val phone: String = "1111111111"
  private val individualDetails = TolerantIndividual(Some("first"), None, Some("last"))

  private def userAnswers(isUK: Boolean): UserAnswers = UserAnswers()
    .set(AreYouUKResidentPage, isUK).toOption.value
    .set(IndividualDetailsPage, individualDetails).toOption.value
    .set(IndividualManualAddressPage, address).toOption.value
    .set(IndividualEmailPage, email).toOption.value
    .set(IndividualPhonePage, phone).toOption.value


  private def addressRow(href: String) = SummaryListRow(
    key = Key(Text(Messages("cya.address")), classes = "govuk-!-width-one-half"),
    value = Value(addressAnswer(address), classes = "govuk-!-width-one-third"),
    actions = Some(
      Actions(
        items = Seq(ActionItem(
          content = HtmlContent(s"""<span aria-hidden="true">${msg"site.edit".resolve}</span>"""),
          href = href,
          visuallyHiddenText = Some(Messages("cya.change.address"))))))
  )

  def expectedRows(addressRow: SummaryListRow): Seq[SummaryListRow] = Seq(
    SummaryListRow(
      key = Key(Text(Messages("cya.name")), classes = "govuk-!-width-one-half"),
      value = Value(Text(individualDetails.fullName), classes = "govuk-!-width-one-third")
    ),
    addressRow,
    SummaryListRow(
      key = Key(Text(Messages("cya.individual.email")), classes = "govuk-!-width-one-half"),
      value = Value(Text(email), classes = "govuk-!-width-one-third"),
      actions = Some(
        Actions(
          items = Seq(ActionItem(
            content = HtmlContent(s"""<span aria-hidden="true">${msg"site.edit".resolve}</span>"""),
            href = controllers.individual.routes.IndividualEmailController.onPageLoad(CheckMode).url,
            visuallyHiddenText = Some(Messages("cya.individual.change.email"))
          )
          )))
    ),
    SummaryListRow(
      key = Key(Text(Messages("cya.individual.phone")), classes = "govuk-!-width-one-half"),
      value = Value(Text(phone), classes = "govuk-!-width-one-third"),
      actions = Some(
        Actions(
          items = Seq(ActionItem(
            content = HtmlContent(s"""<span aria-hidden="true">${msg"site.edit".resolve}</span>"""),
            href = controllers.individual.routes.IndividualPhoneController.onPageLoad(CheckMode).url,
            visuallyHiddenText = Some(Messages("cya.individual.change.phone"))
          )
          )
        )))
  )

  "individualCya" must {
    "return a list of rows of individual uk cya details" in {

      service.individualCya(userAnswers(isUK = true)) mustBe expectedRows(
        addressRow(controllers.individual.routes.IndividualPostcodeController.onPageLoad(CheckMode).url))

    }

    "return a list of rows of individual non uk cya details" in {
      service.individualCya(userAnswers(isUK = false)) mustBe expectedRows(addressRow(
        controllers.individual.routes.IndividualContactAddressController.onPageLoad(CheckMode).url))

    }
  }

}
