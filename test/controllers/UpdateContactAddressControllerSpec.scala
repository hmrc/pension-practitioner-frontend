/*
 * Copyright 2022 HM Revenue & Customs
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

import com.kenshoo.play.metrics.Metrics
import controllers.actions.{AuthAction, FakeAuthAction}
import controllers.base.ControllerSpecBase
import data.SampleData._
import models.{CheckMode, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.JsObject
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import services.PspDetailsHelper._
import services.PspDetailsService
import uk.gov.hmrc.nunjucks.NunjucksRenderer
import utils.TestMetrics
import utils.annotations.AuthMustHaveEnrolment

import scala.concurrent.Future

class UpdateContactAddressControllerSpec extends ControllerSpecBase with MockitoSugar {
  private def onwardRoute = Call("GET", "/foo")

  private val mockPspDetailsService = mock[PspDetailsService]

  override def modules: Seq[GuiceableModule] = Seq(
    bind[Metrics].toInstance(new TestMetrics),
    bind[PspDetailsService].toInstance(mockPspDetailsService),
    bind[AuthAction].qualifiedWith(classOf[AuthMustHaveEnrolment]).to[FakeAuthAction],
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
        when(mockRenderer.render(any(), any())(any()))
          .thenReturn(Future.successful(Html("")))
        when(mockCompoundNavigator.nextPage(any(), any(), any())).thenReturn(onwardRoute)
        when(mockPspDetailsService.getUserAnswers(any(), any())(any(), any()))
          .thenReturn(Future.successful(UserAnswers(jsObject)))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
        val request = FakeRequest(GET, routes.UpdateContactAddressController.onPageLoad().url)
        val templateCaptor = ArgumentCaptor.forClass(classOf[String])
        val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

        val result = route(application, request).value

        status(result) mustEqual OK

        verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

        templateCaptor.getValue mustEqual "updateContactAddress.njk"

        (jsonCaptor.getValue \ "continueUrl").as[String] mustEqual expectedUrl
        (jsonCaptor.getValue \ "address").as[Seq[String]] mustEqual expectedAddress

        application.stop()
      }
    }
  }
}
