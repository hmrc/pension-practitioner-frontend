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
import pages.partnership._
import pages.register.AreYouUKCompanyPage
import uk.gov.hmrc.viewmodels.SummaryList.{Key, Value, Row, Action}
import uk.gov.hmrc.viewmodels.Text.Literal
import uk.gov.hmrc.viewmodels._

class PartnershipCYAServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach with CYAService {

  private val service: PartnershipCYAService = new PartnershipCYAService
  private val partnershipName: String = "Partnership Name"
  private val utr: String = "1234567890"
  private val address: Address = Address("line1", "line2", Some("line3"), Some("line4"), Some("zz1 1zz"), "GB")
  private val email: String = "a@b.c"
  private val phone: String = "1111111111"

  val userAnswersUK: UserAnswers = UserAnswers()
    .setOrException(AreYouUKCompanyPage, true)
    .set(BusinessNamePage, partnershipName).toOption.value
    .set(BusinessUTRPage, utr).toOption.value
    .set(PartnershipAddressPage, address).toOption.value
    .set(PartnershipEmailPage, email).toOption.value
    .set(PartnershipPhonePage, phone).toOption.value

  val userAnswersNonUK: UserAnswers = UserAnswers()
    .setOrException(AreYouUKCompanyPage, false)
    .set(BusinessNamePage, partnershipName).toOption.value
    .set(PartnershipAddressPage, address).toOption.value
    .set(PartnershipEmailPage, email).toOption.value
    .set(PartnershipPhonePage, phone).toOption.value

  val expectedRowsUK: Seq[Row] = Seq(
    Row(
      key = Key(msg"cya.partnershipName", classes = Seq("govuk-!-width-one-half")),
      value = Value(Literal(partnershipName), classes = Seq("govuk-!-width-one-third"))
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
          href = controllers.partnership.routes.PartnershipPostcodeController.onPageLoad(CheckMode).url,
          visuallyHiddenText = Some(msg"cya.change.address")
        )
      )
    ),
    Row(
      key = Key(msg"cya.email".withArgs(partnershipName), classes = Seq("govuk-!-width-one-half")),
      value = Value(Literal(email), classes = Seq("govuk-!-width-one-third")),
      actions = List(
        Action(
          content = msg"site.edit",
          href = controllers.partnership.routes.PartnershipEmailController.onPageLoad(CheckMode).url,
          visuallyHiddenText = Some(msg"cya.change.email".withArgs(partnershipName))
        )
      )
    ),
    Row(
      key = Key(msg"cya.phone".withArgs(partnershipName), classes = Seq("govuk-!-width-one-half")),
      value = Value(Literal(phone), classes = Seq("govuk-!-width-one-third")),
      actions = List(
        Action(
          content = msg"site.edit",
          href = controllers.partnership.routes.PartnershipPhoneController.onPageLoad(CheckMode).url,
          visuallyHiddenText = Some(msg"cya.change.phone".withArgs(partnershipName))
        )
      )
    )
  )

  val expectedRowsNonUK: Seq[Row] = Seq(
    Row(
      key = Key(msg"cya.partnershipName", classes = Seq("govuk-!-width-one-half")),
      value = Value(Literal(partnershipName), classes = Seq("govuk-!-width-one-third"))
    ),
    Row(
      key = Key(msg"cya.address", classes = Seq("govuk-!-width-one-half")),
      value = Value(addressAnswer(address), classes = Seq("govuk-!-width-one-third")),
      actions = List(
        Action(
          content = msg"site.edit",
          href = controllers.partnership.routes.PartnershipContactAddressController.onPageLoad(CheckMode).url,
          visuallyHiddenText = Some(msg"cya.change.address")
        )
      )
    ),
    Row(
      key = Key(msg"cya.email".withArgs(partnershipName), classes = Seq("govuk-!-width-one-half")),
      value = Value(Literal(email), classes = Seq("govuk-!-width-one-third")),
      actions = List(
        Action(
          content = msg"site.edit",
          href = controllers.partnership.routes.PartnershipEmailController.onPageLoad(CheckMode).url,
          visuallyHiddenText = Some(msg"cya.change.email".withArgs(partnershipName))
        )
      )
    ),
    Row(
      key = Key(msg"cya.phone".withArgs(partnershipName), classes = Seq("govuk-!-width-one-half")),
      value = Value(Literal(phone), classes = Seq("govuk-!-width-one-third")),
      actions = List(
        Action(
          content = msg"site.edit",
          href = controllers.partnership.routes.PartnershipPhoneController.onPageLoad(CheckMode).url,
          visuallyHiddenText = Some(msg"cya.change.phone".withArgs(partnershipName))
        )
      )
    )
  )

  "partnershipCya" must {
    "return a list of rows of partnership cya details for uk" in {
      service.partnershipCya(userAnswersUK) mustBe expectedRowsUK
    }
    "return a list of rows of partnership cya details for non uk" in {
      service.partnershipCya(userAnswersNonUK) mustBe expectedRowsNonUK
    }
  }

}
