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

package services

import base.SpecBase
import models.{Address, CheckMode, UserAnswers}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.company._
import pages.register.AreYouUKCompanyPage
import uk.gov.hmrc.viewmodels._
import uk.gov.hmrc.viewmodels.SummaryList.{Key, Value, Row, Action}
import uk.gov.hmrc.viewmodels.Text.Literal

class CompanyCYAServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach with CYAService {

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

  val expectedRowsUK: Seq[Row] = Seq(
    Row(
      key = Key(msg"cya.companyName", classes = Seq("govuk-!-width-one-half")),
      value = Value(Literal(companyName), classes = Seq("govuk-!-width-one-third"))
    ),
    Row(
      key = Key(msg"cya.utr", classes = Seq("govuk-!-width-one-half")),
      value = Value(Literal(utr), classes = Seq("govuk-!-width-one-third"))
    ),
    Row(
      key = Key(msg"cya.address", classes = Seq("govuk-!-width-one-half")),
      value = Value(addressAnswer(address), classes = Seq("govuk-!-width-one-third")),
      actions = List(
        Action(
          content = msg"site.edit",
          href = controllers.company.routes.CompanyPostcodeController.onPageLoad(CheckMode).url,
          visuallyHiddenText = Some(msg"cya.change.address")
        )
      )
    ),
    Row(
      key = Key(msg"cya.email".withArgs(companyName), classes = Seq("govuk-!-width-one-half")),
      value = Value(Literal(email), classes = Seq("govuk-!-width-one-third")),
      actions = List(
        Action(
          content = msg"site.edit",
          href = controllers.company.routes.CompanyEmailController.onPageLoad(CheckMode).url,
          visuallyHiddenText = Some(msg"cya.change.email".withArgs(companyName))
        )
      )
    ),
    Row(
      key = Key(msg"cya.phone".withArgs(companyName), classes = Seq("govuk-!-width-one-half")),
      value = Value(Literal(phone), classes = Seq("govuk-!-width-one-third")),
      actions = List(
        Action(
          content = msg"site.edit",
          href = controllers.company.routes.CompanyPhoneController.onPageLoad(CheckMode).url,
          visuallyHiddenText = Some(msg"cya.change.phone".withArgs(companyName))
        )
      )
    )
  )

  val expectedRowsNonUK: Seq[Row] = Seq(
    Row(
      key = Key(msg"cya.companyName", classes = Seq("govuk-!-width-one-half")),
      value = Value(Literal(companyName), classes = Seq("govuk-!-width-one-third"))
    ),
    Row(
      key = Key(msg"cya.address", classes = Seq("govuk-!-width-one-half")),
      value = Value(addressAnswer(address), classes = Seq("govuk-!-width-one-third")),
      actions = List(
        Action(
          content = msg"site.edit",
          href = controllers.company.routes.CompanyContactAddressController.onPageLoad(CheckMode).url,
          visuallyHiddenText = Some(msg"cya.change.address")
        )
      )
    ),
    Row(
      key = Key(msg"cya.email".withArgs(companyName), classes = Seq("govuk-!-width-one-half")),
      value = Value(Literal(email), classes = Seq("govuk-!-width-one-third")),
      actions = List(
        Action(
          content = msg"site.edit",
          href = controllers.company.routes.CompanyEmailController.onPageLoad(CheckMode).url,
          visuallyHiddenText = Some(msg"cya.change.email".withArgs(companyName))
        )
      )
    ),
    Row(
      key = Key(msg"cya.phone".withArgs(companyName), classes = Seq("govuk-!-width-one-half")),
      value = Value(Literal(phone), classes = Seq("govuk-!-width-one-third")),
      actions = List(
        Action(
          content = msg"site.edit",
          href = controllers.company.routes.CompanyPhoneController.onPageLoad(CheckMode).url,
          visuallyHiddenText = Some(msg"cya.change.phone".withArgs(companyName))
        )
      )
    )
  )

  "companyCya" must {
    "return a list of rows of company cya details for UK" in {
      service.companyCya(userAnswersUK) mustBe expectedRowsUK
    }
    "return a list of rows of company cya details for non UK" in {
      service.companyCya(userAnswersNonUK) mustBe expectedRowsNonUK
    }
  }

}
