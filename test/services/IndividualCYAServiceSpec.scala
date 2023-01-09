/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.viewmodels.SummaryList.{Action, Key, Row, Value}
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


  private def addressRow(href: String) = Row(
    key = Key(msg"cya.address", classes = Seq("govuk-!-width-one-half")),
    value = Value(addressAnswer(address), classes = Seq("govuk-!-width-one-third")),
    actions = List(
      Action(
        content = Html(s"""<span aria-hidden="true">${msg"site.edit".resolve}</span>"""),
        href = href,
        visuallyHiddenText = Some(msg"cya.change.address")
      )
    )
  )

  def expectedRows(addressRow: Row): Seq[Row] = Seq(
    Row(
      key = Key(msg"cya.name", classes = Seq("govuk-!-width-one-half")),
      value = Value(Literal(individualDetails.fullName), classes = Seq("govuk-!-width-one-third"))
    ),
    addressRow,
    Row(
      key = Key(msg"cya.individual.email", classes = Seq("govuk-!-width-one-half")),
      value = Value(Literal(email), classes = Seq("govuk-!-width-one-third")),
      actions = List(
        Action(
          content = Html(s"""<span aria-hidden="true">${msg"site.edit".resolve}</span>"""),
          href = controllers.individual.routes.IndividualEmailController.onPageLoad(CheckMode).url,
          visuallyHiddenText = Some(msg"cya.individual.change.email")
        )
      )
    ),
    Row(
      key = Key(msg"cya.individual.phone", classes = Seq("govuk-!-width-one-half")),
      value = Value(Literal(phone), classes = Seq("govuk-!-width-one-third")),
      actions = List(
        Action(
          content = Html(s"""<span aria-hidden="true">${msg"site.edit".resolve}</span>"""),
          href = controllers.individual.routes.IndividualPhoneController.onPageLoad(CheckMode).url,
          visuallyHiddenText = Some(msg"cya.individual.change.phone")
        )
      )
    )
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
