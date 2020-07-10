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

package navigators

import data.SampleData
import models.{CheckMode, NormalMode, UserAnswers}
import org.scalatest.prop.TableFor3
import pages._
import pages.company._
import play.api.mvc.Call
import controllers.company.routes._
import pages.register.DeclarationPage

class CompanyNavigatorSpec extends NavigatorBehaviour {
  private val uaConfirmAddressYes = SampleData
    .emptyUserAnswers.setOrException(ConfirmAddressPage, SampleData.addressUK)

  private def uaConfirmName(v:Boolean) = SampleData
    .emptyUserAnswers.setOrException(ConfirmNamePage, v)

  private def uaUseSameAddress(v:Boolean) = SampleData
    .emptyUserAnswers.setOrException(CompanyUseSameAddressPage, v)


  private val navigator: CompoundNavigator = injector.instanceOf[CompoundNavigator]

  "NormalMode" must {
    def normalModeRoutes: TableFor3[Page, UserAnswers, Call] =
      Table(
        ("Id", "UserAnswers", "Next Page"),
        row(BusinessUTRPage)(CompanyNameController.onPageLoad()),
        row(CompanyNamePage)(ConfirmNameController.onPageLoad()),
        row(ConfirmNamePage)(ConfirmAddressController.onPageLoad(), Some(uaConfirmName(true))),
        row(ConfirmNamePage)(TellHMRCController.onPageLoad(),Some(uaConfirmName(false))),
        row(ConfirmAddressPage)(TellHMRCController.onPageLoad()),
        row(ConfirmAddressPage)(CompanyUseSameAddressController.onPageLoad(), Some(uaConfirmAddressYes)),
        row(CompanyUseSameAddressPage)(CompanyEmailController.onPageLoad(NormalMode), Some(uaUseSameAddress(true))),
        row(CompanyUseSameAddressPage)(CompanyPostcodeController.onPageLoad(NormalMode), Some(uaUseSameAddress(false))),
        row(CompanyPostcodePage)(CompanyAddressListController.onPageLoad(NormalMode)),
        row(CompanyAddressListPage)(CompanyEmailController.onPageLoad(NormalMode)),
        row(CompanyAddressPage)(CompanyEmailController.onPageLoad(NormalMode)),
        row(CompanyEmailPage)(CompanyPhoneController.onPageLoad(NormalMode)),
        row(CompanyPhonePage)(CheckYourAnswersController.onPageLoad()),
        row(DeclarationPage)(controllers.register.routes.ConfirmationController.onPageLoad())
      )

    behave like navigatorWithRoutesForMode(NormalMode)(navigator, normalModeRoutes)
  }


}
