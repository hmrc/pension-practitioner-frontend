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

package controllers.amend

import controllers.base.ControllerSpecBase
import models.UserAnswers
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{AddressChange, NameChange}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers.{route, status, _}
import play.twirl.api.Html
import services.PspDetailsService

import scala.concurrent.Future

class ViewDetailsControllerSpec extends ControllerSpecBase with MockitoSugar {

  private val pspDetailsService = mock[PspDetailsService]
  private val extraModules: Seq[GuiceableModule] = Seq(
    bind[PspDetailsService].toInstance(pspDetailsService)
  )
  private def application(userAnswers:Option[UserAnswers]): Application =
    applicationBuilder(userAnswers = userAnswers, extraModules = extraModules).build()
  private val templateToBeRendered = "amend/viewDetails.njk"


  val json: JsObject = Json.obj(
    "title" -> "title",
    "heading" -> "heading",
    "list" -> "list",
    "nextPage" -> routes.DeclarationController.onPageLoad().url
  )

  private def onPageLoadUrl: String = routes.ViewDetailsController.onPageLoad().url

  override def beforeEach: Unit = {
    super.beforeEach
    when(pspDetailsService.getJson(any(), any())(any(), any(), any())).thenReturn(Future.successful(json))
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
  }

  "ViewDetails Controller" must {
    "return OK and the correct view for a GET and don't display button where no changes" in {
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application(None), httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered
      (jsonCaptor.getValue \ "displayContinueButton").as[Boolean] mustEqual false
    }

    "return OK and the correct view for a GET and display button where name changed" in {
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val ua = UserAnswers().set(NameChange, true).toOption

      val result = route(application(ua), httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered
      (jsonCaptor.getValue \ "displayContinueButton").as[Boolean] mustEqual true
    }

    "return OK and the correct view for a GET and display button where address changed" in {
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val ua = UserAnswers().set(AddressChange, true).toOption

      val result = route(application(ua), httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered
      (jsonCaptor.getValue \ "displayContinueButton").as[Boolean] mustEqual true
    }
  }
}
