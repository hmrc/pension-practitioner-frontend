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
import connectors.SubscriptionConnector
import connectors.cache.UserAnswersCacheConnector
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
  private val service: PspDetailsService = new PspDetailsService(mockSubscriptionConnector, mockUserAnswersCacheConnector)

  override def beforeEach: Unit = {
    super.beforeEach
    reset(mockSubscriptionConnector)
    when(mockUserAnswersCacheConnector.save(any())(any(), any())).thenReturn(Future.successful(Json.obj()))
  }

  "getJson" must {
    "return appropriate json for Individual" in {
      when(mockSubscriptionConnector.getSubscriptionDetails(eqTo(pspId))(any(), any()))
        .thenReturn(Future.successful(uaIndividualUK))

      whenReady(service.getJson(pspId)) { result =>
        result mustBe expected("Individual", "Stephen Wood", nino)
      }
    }

    "return appropriate json for Company" in {
      when(mockSubscriptionConnector.getSubscriptionDetails(eqTo(pspId))(any(), any()))
        .thenReturn(Future.successful(uaCompanyUk))

      whenReady(service.getJson(pspId)) { result =>
        result mustBe expected("Company", "Test Ltd", utr)
      }
    }

    "return appropriate json for nonUk Partnership" in {
      when(mockSubscriptionConnector.getSubscriptionDetails(eqTo(pspId))(any(), any()))
        .thenReturn(Future.successful(uaPartnershipNonUK))

      whenReady(service.getJson(pspId)) { result =>
        result mustBe expected("Partnership", "Testing Ltd", Json.obj(), nonUkAddress)
      }
    }
  }


}

object PspDetailsServiceSpec {

  val utr: JsObject = Json.obj(
    "key" -> Json.obj(
      "classes" -> "govuk-!-width-one-half",
      "text" -> "Unique Taxpayer Reference"
    ),
    "value" -> Json.obj(
      "classes" -> "govuk-!-width-one-third",
      "text" -> "1234567890"
    )
  )

  val nino: JsObject = Json.obj(
    "key" -> Json.obj(
      "text" -> s"Individual’s National Insurance number",
      "classes" -> "govuk-!-width-one-half"
    ),
    "value" -> Json.obj(
      "text" -> "AB123456C",
      "classes" -> "govuk-!-width-one-third"
    )
  )

  val nonUkAddress: JsObject = Json.obj(
    "html" -> "<span class=\"govuk-!-display-block\">4 Other Place</span><span class=\"govuk-!-display-block\">Some District</span><span class=\"govuk-!-display-block\">Anytown</span><span class=\"govuk-!-display-block\">Somerset</span><span class=\"govuk-!-display-block\">France</span>",
    "classes" -> "govuk-!-width-one-third")
  val ukAddress: JsObject = Json.obj(
    "html" -> "<span class=\"govuk-!-display-block\">4 Other Place</span><span class=\"govuk-!-display-block\">Some District</span><span class=\"govuk-!-display-block\">Anytown</span><span class=\"govuk-!-display-block\">Somerset</span><span class=\"govuk-!-display-block\">ZZ1 1ZZ</span><span class=\"govuk-!-display-block\">United Kingdom</span>",
    "classes" -> "govuk-!-width-one-third")

  def listBasic1(typeText: String, name: String, address: JsObject): JsArray = Json.arr(
    Json.obj(
      "key" -> Json.obj(
        "text" -> "Practitioner ID",
        "classes" -> "govuk-!-width-one-half"
      ),
      "value" -> Json.obj(
        "text" -> "psp-id",
        "classes" -> "govuk-!-width-one-third"
      )
    ),
    Json.obj(
      "key" -> Json.obj(
        "text" -> s"$typeText name",
        "classes" -> "govuk-!-width-one-half"
      ),
      "value" -> Json.obj(
        "text" -> name,
        "classes" -> "govuk-!-width-one-third"
      )
    )
  )

  def listBasic2(typeText: String, address: JsObject): JsArray = Json.arr(
    Json.obj(
      "key" -> Json.obj(
        "text" -> s"$typeText’s contact address",
        "classes" -> "govuk-!-width-one-half"
      ),
      "value" -> address
    ),
    Json.obj(
      "key" -> Json.obj(
        "text" -> s"$typeText’s email address",
        "classes" -> "govuk-!-width-one-half"
      ),
      "value" -> Json.obj(
        "text" -> "sdd@ds.sd",
        "classes" -> "govuk-!-width-one-third"
      )
    ),
    Json.obj(
      "key" -> Json.obj(
        "text" -> s"$typeText’s phone number",
        "classes" -> "govuk-!-width-one-half"
      ),
      "value" -> Json.obj(
        "text" -> "3445",
        "classes" -> "govuk-!-width-one-third"
      )
    )
  )

  def list(typeText: String, name: String, ninoUtr: JsObject, address: JsObject): JsArray =
    if(ninoUtr != Json.obj()) {
      (listBasic1(typeText, name, address):+ninoUtr) ++ listBasic2(typeText, address)
    } else {
      listBasic1(typeText, name, address) ++ listBasic2(typeText, address)
    }

  def expected(typeText: String, name: String, ninoUtr: JsObject, address: JsObject = ukAddress): JsObject = Json.obj(
    "title" -> s"$typeText details",
    "heading" -> s"$name’s details",
    "list" -> list(typeText, name, ninoUtr, address),
    "nextPage" -> "/pension-scheme-practitioner/declare"
  )


}
