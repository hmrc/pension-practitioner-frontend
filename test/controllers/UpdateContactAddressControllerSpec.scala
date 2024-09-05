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

package controllers

import controllers.actions.{AuthAction, FakeAuthAction}
import controllers.base.ControllerSpecBase
import data.SampleData._
import models.{CheckMode, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.JsObject
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.PspDetailsHelper._
import services.PspDetailsService
import uk.gov.hmrc.nunjucks.NunjucksRenderer
import utils.annotations.AuthMustHaveEnrolmentWithNoIV
import views.html.UpdateContactAddressView

import scala.concurrent.Future

class UpdateContactAddressControllerSpec extends ControllerSpecBase {
  private def onwardRoute = Call("GET", "/foo")

  private val mockPspDetailsService = mock[PspDetailsService]

  override def fakeApplication(): Application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

  override def modules: Seq[GuiceableModule] = Seq(
    bind[PspDetailsService].toInstance(mockPspDetailsService),
    bind[AuthAction].qualifiedWith(classOf[AuthMustHaveEnrolmentWithNoIV]).to[FakeAuthAction],
    bind[NunjucksRenderer].toInstance(mockRenderer)
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockPspDetailsService)
  }

  val expectedAddressUK: Seq[String] = Seq("4 Other Place", "Some District", "Anytown", "Somerset", "ZZ1 1ZZ", "United Kingdom")
  val expectedAddressNonUK: Seq[String] = Seq("4 Other Place", "Some District", "Anytown", "Somerset", "France")

  "UpdateContactAddress Controller" must {

    behave like updateContactAddressController(
      description = "company",
      jsObject = uaCompanyUk,
      expectedUrl = controllers.company.routes.CompanyPostcodeController.onPageLoad(CheckMode).url,
      expectedAddress = expectedAddressUK
    )

    behave like updateContactAddressController(
      description = "individual",
      jsObject = uaIndividualUK,
      expectedUrl = controllers.individual.routes.IndividualPostcodeController.onPageLoad(CheckMode).url,
      expectedAddress = expectedAddressUK
    )

    behave like updateContactAddressController(
      description = "partnership",
      jsObject = uaPartnershipNonUK,
      expectedUrl = controllers.partnership.routes.PartnershipPostcodeController.onPageLoad(CheckMode).url,
      expectedAddress = expectedAddressNonUK
    )

    def updateContactAddressController(description: String, jsObject: JsObject, expectedUrl: => String, expectedAddress: => Seq[String]): Unit = {
      s"return OK and the correct view for a GET for $description" in {
        when(mockCompoundNavigator.nextPage(any(), any(), any())).thenReturn(onwardRoute)
        when(mockPspDetailsService.getUserAnswers(any(), any())(any(), any()))
          .thenReturn(Future.successful(UserAnswers(jsObject)))

        val request = FakeRequest(GET, routes.UpdateContactAddressController.onPageLoad().url)

        val result = route(app, request).value

        val view = app.injector.instanceOf[UpdateContactAddressView].apply(expectedAddress, expectedUrl)(request, messages)

        status(result) mustEqual OK
        compareResultAndView(result, view)
      }
    }
  }
}
