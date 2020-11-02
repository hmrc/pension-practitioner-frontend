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

package services

import base.SpecBase
import config.FrontendAppConfig
import connectors.SubscriptionConnector
import connectors.cache.UserAnswersCacheConnector
import models.{CheckMode, UserAnswers}
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsArray, JsObject, Json}
import services.PsaDetailsHelper._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PspDetailsServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach with ScalaFutures {

  import PspDetailsServiceSpec._

  private val pspId: String = "psp-id"
  private val mockSubscriptionConnector: SubscriptionConnector = mock[SubscriptionConnector]
  private val mockUserAnswersCacheConnector: UserAnswersCacheConnector = mock[UserAnswersCacheConnector]
  private val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]
  private val service: PspDetailsService = new PspDetailsService(mockAppConfig, mockSubscriptionConnector, mockUserAnswersCacheConnector)

  override def beforeEach: Unit = {
    super.beforeEach
    reset(mockSubscriptionConnector, mockUserAnswersCacheConnector, mockAppConfig)
    when(mockUserAnswersCacheConnector.save(any())(any(), any())).thenReturn(Future.successful(Json.obj()))
    when(mockAppConfig.returnToPspDashboardUrl).thenReturn(frontendAppConfig.returnToPspDashboardUrl)
  }

  "getJson" must {
    "return appropriate json for Individual" in {
      when(mockSubscriptionConnector.getSubscriptionDetails(eqTo(pspId))(any(), any()))
        .thenReturn(Future.successful(uaIndividualUK))

      whenReady(service.getJson(None, pspId)) { result =>
        result mustBe expected("Individual", "Stephen Wood", nino)
      }
    }

    "return appropriate json for Company" in {

      whenReady(service.getJson(Some(UserAnswers(uaCompanyUk)), pspId)) { result =>
        result mustBe expected("Company", "Test Ltd", utr)
      }
    }

    "return appropriate json for nonUk Partnership" in {
      when(mockSubscriptionConnector.getSubscriptionDetails(eqTo(pspId))(any(), any()))
        .thenReturn(Future.successful(uaPartnershipNonUK))

      whenReady(service.getJson(None, pspId)) { result =>
        result mustBe expected("Partnership", "Testing Ltd", Json.obj(), nonUkAddress)
      }
    }
  }


}

object PspDetailsServiceSpec {

  val halfWidth: String = "govuk-!-width-one-half"
  val thirdWidth: String = "govuk-!-width-one-third"

  val utr: JsObject = Json.obj(
    "key" -> Json.obj(
      "classes" -> halfWidth,
      "text" -> "Unique Taxpayer Reference"
    ),
    "value" -> Json.obj(
      "classes" -> thirdWidth,
      "text" -> "1234567890"
    )
  )

  val nino: JsObject = Json.obj(
    "key" -> Json.obj(
      "text" -> s"Individual’s National Insurance number",
      "classes" -> halfWidth
    ),
    "value" -> Json.obj(
      "text" -> "AB123456C",
      "classes" -> thirdWidth
    )
  )

  val pspIdRow: JsObject = Json.obj(
    "key" -> Json.obj(
      "text" -> "Practitioner ID",
      "classes" -> halfWidth
    ),
    "value" -> Json.obj(
      "text" -> "psp-id",
      "classes" -> thirdWidth
    )
  )

  def addressRow(typeText: String, address: JsObject, href: String): JsObject = Json.obj(
    "key" -> Json.obj(
      "text" -> s"$typeText’s contact address",
      "classes" -> halfWidth
    ),
    "value" -> address,
    "actions" -> Json.obj(
      "items" -> Json.arr(
        Json.obj(
          "href" -> href,
          "text" -> "Change",
          "visuallyHiddenText" -> "Contact address"
        )
      )
    )
  )

  def emailRow(typeText: String, name: String, href: String): JsObject = Json.obj(
    "key" -> Json.obj(
      "text" -> s"$typeText’s email address",
      "classes" -> halfWidth
    ),
    "value" -> Json.obj(
      "text" -> "sdd@ds.sd",
      "classes" -> thirdWidth
    ),
    "actions" -> Json.obj(
      "items" -> Json.arr(
        Json.obj(
          "href" -> href,
          "text" -> "Change",
          "visuallyHiddenText" -> s"$name’s email address?"
        )
      )
    )
  )

  def phoneRow(typeText: String, name: String, href: String): JsObject = Json.obj(
    "key" -> Json.obj(
      "text" -> s"$typeText’s phone number",
      "classes" -> halfWidth
    ),
    "value" -> Json.obj(
      "text" -> "3445",
      "classes" -> thirdWidth
    ),
    "actions" -> Json.obj(
      "items" -> Json.arr(
        Json.obj(
          "href" -> href,
          "text" -> "Change",
          "visuallyHiddenText" -> s"$name’s phone number?"
        )
      )
    )
  )

  val nonUkAddress: JsObject = Json.obj(
    "html" -> "<span class=\"govuk-!-display-block\">4 Other Place</span><span class=\"govuk-!-display-block\">Some District</span><span class=\"govuk-!-display-block\">Anytown</span><span class=\"govuk-!-display-block\">Somerset</span><span class=\"govuk-!-display-block\">France</span>",
    "classes" -> thirdWidth)
  val ukAddress: JsObject = Json.obj(
    "html" -> "<span class=\"govuk-!-display-block\">4 Other Place</span><span class=\"govuk-!-display-block\">Some District</span><span class=\"govuk-!-display-block\">Anytown</span><span class=\"govuk-!-display-block\">Somerset</span><span class=\"govuk-!-display-block\">ZZ1 1ZZ</span><span class=\"govuk-!-display-block\">United Kingdom</span>",
    "classes" -> thirdWidth)

  def nameRow(typeText: String, name: String): JsObject = Json.obj(
      "key" -> Json.obj(
        "text" -> s"$typeText’s name",
        "classes" -> halfWidth
      ),
      "value" -> Json.obj(
        "text" -> name,
        "classes" -> thirdWidth
      )
  )

  def nonUkNameRow(typeText: String, name: String, href: String): JsObject = Json.obj(
    "key" -> Json.obj(
      "text" -> s"$typeText’s name",
      "classes" -> halfWidth
    ),
    "value" -> Json.obj(
      "text" -> name,
      "classes" -> thirdWidth
    ),
    "actions" -> Json.obj(
      "items" -> Json.arr(
        Json.obj(
          "href" -> href,
          "text" -> "Change",
          "visuallyHiddenText" -> s"$name’s name"
        )
      )
    )
  )

  def individualList(typeText: String, name: String, address: JsObject): JsArray = Json.arr(
    pspIdRow,
    nameRow(typeText, name),
    nino,
    addressRow(typeText, address, controllers.individual.routes.IndividualPostcodeController.onPageLoad(CheckMode).url),
    emailRow(typeText, name, controllers.individual.routes.IndividualEmailController.onPageLoad(CheckMode).url),
    phoneRow(typeText, name, controllers.individual.routes.IndividualPhoneController.onPageLoad(CheckMode).url)
  )

  def companyList(typeText: String, name: String, address: JsObject): JsArray = Json.arr(
    pspIdRow,
    utr,
    nameRow(typeText, name),
    addressRow(typeText, address, controllers.company.routes.CompanyPostcodeController.onPageLoad(CheckMode).url),
    emailRow(typeText, name, controllers.company.routes.CompanyEmailController.onPageLoad(CheckMode).url),
    phoneRow(typeText, name, controllers.company.routes.CompanyPhoneController.onPageLoad(CheckMode).url)
  )

  def partnershipList(typeText: String, name: String, address: JsObject): JsArray = Json.arr(
    pspIdRow,
    nonUkNameRow(typeText, name, controllers.partnership.routes.PartnershipNameController.onPageLoad(CheckMode).url),
    addressRow(typeText, address, controllers.partnership.routes.PartnershipPostcodeController.onPageLoad(CheckMode).url),
    emailRow(typeText, name, controllers.partnership.routes.PartnershipEmailController.onPageLoad(CheckMode).url),
    phoneRow(typeText, name, controllers.partnership.routes.PartnershipPhoneController.onPageLoad(CheckMode).url)
  )

  def list(typeText: String, name: String, address: JsObject): JsArray =
    typeText match {
      case "Individual" => individualList(typeText, name, address)
      case "Company" => companyList(typeText, name, address)
      case _ => partnershipList(typeText, name, address)
    }

  def expected(typeText: String, name: String, ninoUtr: JsObject, address: JsObject = ukAddress): JsObject = Json.obj(
    "pageTitle" -> s"$typeText details",
    "heading" -> s"$name’s details",
    "list" -> list(typeText, name, address),
    "nextPage" -> "/pension-scheme-practitioner/declare",
    "returnLink" -> s"Return to $name",
    "returnUrl" -> "http://localhost:8204/manage-pension-schemes/dashboard"
  )


}
