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

package controllers


import controllers.base.ControllerSpecBase
import data.SampleData
import models.UserAnswers
import models.register.{TolerantIndividual, RegistrationLegalStatus}
import models.requests.{DataRequest, PSPUser, UserType}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import pages.RegistrationInfoPage
import pages.company.CompanyEmailPage
import pages.individual.{IndividualDetailsPage, IndividualEmailPage}
import pages.partnership.PartnershipEmailPage
import play.api.mvc.{AnyContent, Result, Results}
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Nino

import scala.concurrent.Future

class DataRetrievalsSpec extends ControllerSpecBase with MockitoSugar with Results with ScalaFutures  {

  private val defaultPspId: String = "A0000000"
  private val defaultUserType = UserType.Organisation
  private val pspUser = PSPUser(defaultUserType, Some(Nino("AB100100A")), isExistingPSP = false, None, Some(defaultPspId))

  private def request(ua:UserAnswers):DataRequest[AnyContent] = DataRequest(FakeRequest.apply(), "id", pspUser, ua)
  private def createResult(name:String, email:String) = Ok(s"name=$name and email=$email")
  private val block: (String, String) => Future[Result] =
    (name, email) => Future.successful(createResult(name, email))
  private val email = "a@a.c"

  "retrievePspNameAndEmail" must {
    "call the function with the name and email for individuals when present in user answers" in {
      val firstName = "Bill"
      val lastName = "Bloggs"
      val individual = TolerantIndividual(Some(firstName), None, Some(lastName))
      val ua = UserAnswers()
        .setOrException(RegistrationInfoPage, SampleData.registrationInfo(RegistrationLegalStatus.Individual))
        .setOrException(IndividualDetailsPage, individual)
        .setOrException(IndividualEmailPage, email)
      val futureResult = DataRetrievals.retrievePspNameAndEmail(block)(request(ua))
      whenReady(futureResult) { result =>
        result mustBe createResult(individual.fullName, email)
      }
    }

    "call the function with the name and email for uk companies when present in user answers" in {
      val companyName = "acme ltd"
      val ua = UserAnswers()
        .setOrException(RegistrationInfoPage, SampleData.registrationInfo(RegistrationLegalStatus.LimitedCompany))
        .setOrException(pages.company.BusinessNamePage, companyName)
        .setOrException(CompanyEmailPage, email)
      val futureResult = DataRetrievals.retrievePspNameAndEmail(block)(request(ua))
      whenReady(futureResult) { result =>
        result mustBe createResult(companyName, email)
      }
    }

    "call the function with the name and email for non-uk partnerships when present in user answers" in {
      val companyName = "acme ltd"
      val ua = UserAnswers()
        .setOrException(RegistrationInfoPage, SampleData.registrationInfo(RegistrationLegalStatus.Partnership))
        .setOrException(pages.partnership.BusinessNamePage, companyName)
        .setOrException(PartnershipEmailPage, email)
      val futureResult = DataRetrievals.retrievePspNameAndEmail(block)(request(ua))
      whenReady(futureResult) { result =>
        result mustBe createResult(companyName, email)
      }
    }

    "redirect to session expired when none of requisite values present in user answers" in {
      val ua = UserAnswers()
      val futureResult = DataRetrievals.retrievePspNameAndEmail(block)(request(ua))
      whenReady(futureResult) { result =>
        result mustBe Redirect(controllers.routes.SessionExpiredController.onPageLoad())
      }
    }
  }

}
