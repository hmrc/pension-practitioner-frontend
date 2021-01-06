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

package utils

import base.SpecBase
import models.register.{RegistrationCustomerType, RegistrationIdType, RegistrationInfo, RegistrationLegalStatus}
import models.requests.{DataRequest, PSPUser, UserType}
import models.{Address, KnownFact, KnownFacts, TolerantAddress, UserAnswers}
import pages.RegistrationInfoPage
import pages.company.CompanyRegisteredAddressPage
import play.api.libs.json.Json
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Nino

class KnownFactsRetrievalSpec extends SpecBase {

  private val psp = "psp-id"
  private val utr = "test-utr"
  private val nino = Nino("AB123456C")
  private val sapNumber = "test-sap-number"
  private val nonUk = "test-non-uk"

  private lazy val generator = app.injector.instanceOf[KnownFactsRetrieval]

  "retrieve" must {

    "return set of known facts" when {

      "user is individual" which {

        "comprise of NINO" in {

          val registration = RegistrationInfo(
            RegistrationLegalStatus.Individual,
            sapNumber,
            noIdentifier = false,
            RegistrationCustomerType.UK,
            Some(RegistrationIdType.Nino),
            Some(nino.nino)
          )

          implicit val request: DataRequest[AnyContent] = DataRequest(
            FakeRequest(), "id",
            PSPUser(UserType.Individual, Some(nino), isExistingPSP = false, None, None),
            UserAnswers(Json.obj(
              CompanyRegisteredAddressPage.toString -> Address("1 Street", "Somewhere", None, None, Some("ZZ1 1ZZ"), "GB"),
              RegistrationInfoPage.toString -> registration
            ))
          )

          generator.retrieve(psp) mustBe Some(KnownFacts(
                      Set(KnownFact("PSPID", psp)),
                      Set(KnownFact("NINO", nino.nino)
                      )))

        }

      }

      "user is partnership" which {

        "comprise of SA UTR" when {

          "company is UK" in {

            val registration = RegistrationInfo(
              RegistrationLegalStatus.Partnership,
              sapNumber,
              noIdentifier = false,
              RegistrationCustomerType.UK,
              Some(RegistrationIdType.UTR),
              Some(utr)
            )

            implicit val request: DataRequest[AnyContent] = DataRequest(
              FakeRequest(), "id",
              PSPUser(UserType.Organisation, None, isExistingPSP = false, None, None),
              UserAnswers(Json.obj(
                CompanyRegisteredAddressPage.toString -> Address("1 Street", "Somewhere", None, None, Some("ZZ1 1ZZ"), "GB"),
                RegistrationInfoPage.toString -> registration
              ))
            )

            generator.retrieve(psp) mustBe Some(KnownFacts(
              Set(KnownFact("PSPID", psp)),
              Set(KnownFact("SAUTR", utr)
              )))
          }

        }

        "comprise of PSP ID and Country Code" when {

          "company is Non-UK" in {

            val registration = RegistrationInfo(
              RegistrationLegalStatus.Partnership,
              sapNumber,
              noIdentifier = false,
              RegistrationCustomerType.NonUK,
              None,
              None
            )

            implicit val request: DataRequest[AnyContent] = DataRequest(
              FakeRequest(), "id",
              PSPUser(UserType.Organisation, None, isExistingPSP = false, None, None),
              UserAnswers(Json.obj(
                CompanyRegisteredAddressPage.toString -> Address(
                  "1 Street",
                  "Somewhere",
                  None, None, None,
                  nonUk
                ),
                RegistrationInfoPage.toString -> registration
              ))
            )

            generator.retrieve(psp) mustBe Some(KnownFacts(
              Set(KnownFact("PSPID", psp)),
              Set(KnownFact("CountryCode", nonUk)
              )))
          }
        }
      }

      "user is company" which {

        "comprise of CTR UTR" when {

          "company is UK" in {

            val registration = RegistrationInfo(
              RegistrationLegalStatus.LimitedCompany,
              sapNumber,
              noIdentifier = false,
              RegistrationCustomerType.UK,
              Some(RegistrationIdType.UTR),
              Some(utr)
            )

            implicit val request: DataRequest[AnyContent] = DataRequest(
              FakeRequest(), "id",
              PSPUser(UserType.Organisation, None, isExistingPSP = false, None, None),
              UserAnswers(Json.obj(
                CompanyRegisteredAddressPage.toString -> Address("1 Street", "Somewhere", None, None, Some("ZZ1 1ZZ"), "GB"),
                RegistrationInfoPage.toString -> registration
              ))
            )

            generator.retrieve(psp) mustBe Some(KnownFacts(
              Set(KnownFact("PSPID", psp)),
              Set(KnownFact("CTUTR", utr)
              )))
          }

        }

        "comprise of PSP ID and Country Code" when {

          "company is Non-UK" in {

            val registration = RegistrationInfo(
              RegistrationLegalStatus.LimitedCompany,
              sapNumber,
              noIdentifier = false,
              RegistrationCustomerType.NonUK,
              None,
              None
            )

            implicit val request: DataRequest[AnyContent] = DataRequest(
              FakeRequest(), "id",
              PSPUser(UserType.Organisation, None, isExistingPSP = false, None, None),
              UserAnswers(Json.obj(
                CompanyRegisteredAddressPage.toString -> Address("1 Street", "Somewhere", None, None, None, nonUk),
                RegistrationInfoPage.toString -> registration
              ))
            )

            generator.retrieve(psp) mustBe Some(KnownFacts(
              Set(KnownFact("PSPID", psp)),
              Set(KnownFact("CountryCode", nonUk)
              )))
          }

        }

      }

    }

  }

}
