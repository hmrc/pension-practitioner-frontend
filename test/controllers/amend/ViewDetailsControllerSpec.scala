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

package controllers.amend

import controllers.base.ControllerSpecBase
import models.PspDetailsData
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.test.Helpers.{route, status, _}
import play.twirl.api.Html
import services.PspDetailsService

import scala.concurrent.Future

class ViewDetailsControllerSpec extends ControllerSpecBase {

  private val pspDetailsService = mock[PspDetailsService]
  private val extraModules: Seq[GuiceableModule] = Seq(
    bind[PspDetailsService].toInstance(pspDetailsService)
  )
  override def fakeApplication(): Application =
    applicationBuilder(userAnswers = None, extraModules = extraModules).build()

  private def onPageLoadUrl: String = routes.ViewDetailsController.onPageLoad().url

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(pspDetailsService.getData(any(), any())(any(), any(), any())).thenReturn(Future.successful
    (PspDetailsData("title", "heading", Seq(), None, false, routes.DeclarationController.onPageLoad().url)))
   }

  "ViewDetails Controller" must {
    "return OK and the correct view for a GET" in {

      val req = httpGETRequest(onPageLoadUrl)

      val result = route(app, req).value

      status(result) mustEqual OK
      val view = app.injector.instanceOf[views.html.amend.ViewDetailsView].apply(
        "title",
        "heading",
        Seq(),
        false,
        routes.DeclarationController.onPageLoad().url,
        None)(req, messages)

      compareResultAndView(result, view)
    }
  }
}
