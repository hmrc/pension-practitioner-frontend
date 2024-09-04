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

package controllers.company

import controllers.actions.MutableFakeDataRetrievalAction
import controllers.base.ControllerSpecBase
import matchers.JsonMatchers
import models.UserAnswers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.OptionValues
import org.scalatest.TryValues
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import services.CompanyCYAService
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow, Value}
import uk.gov.hmrc.viewmodels.{NunjucksSupport, SummaryList}
import uk.gov.hmrc.viewmodels.SummaryList.Row
import uk.gov.hmrc.viewmodels.Text.Literal
import views.html.CheckYourAnswersView

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends ControllerSpecBase with MockitoSugar with NunjucksSupport
  with JsonMatchers with OptionValues with TryValues {

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val companyCYAService = mock[CompanyCYAService]
  private val companyName: String = "Company name"
  private val application: Application =
    applicationBuilderMutableRetrievalAction(
      mutableFakeDataRetrievalAction,
      extraModules = Seq(bind[CompanyCYAService].toInstance(companyCYAService))).build()
  private def onPageLoadUrl: String = routes.CheckYourAnswersController.onPageLoad().url

  private val list: Seq[Row] = Seq(Row(
    key = SummaryList.Key(msg"cya.companyName", classes = Seq("govuk-!-width-one-half")),
    value = SummaryList.Value(Literal(companyName), classes = Seq("govuk-!-width-one-third"))
  ))

  override def beforeEach(): Unit = {
    super.beforeEach()
    mutableFakeDataRetrievalAction.setDataToReturn(Some(UserAnswers()))
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
  }

  "CheckYourAnswers Controller" must {
    "return OK and the correct view for a GET" in {
      val request = FakeRequest(GET, onPageLoadUrl)
      when(companyCYAService.companyCya(any())(any())).thenReturn(list)

      val result = route(application, httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual OK

      val view = application.injector.instanceOf[CheckYourAnswersView].apply(
        controllers.company.routes.DeclarationController.onPageLoad(),
        Seq(
          SummaryListRow(key = Key(Text(Messages("cya.companyName")), classes = "govuk-!-width-one-half"),
            value = Value(HtmlContent(companyName), classes = "govuk-!-width-one-third"))
        )
      )(request, messages)

      compareResultAndView(result, view)
    }

    "redirect to Session Expired page for a GET when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(application, httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
    }
  }
}
