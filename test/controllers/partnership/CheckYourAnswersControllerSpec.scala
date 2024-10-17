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

package controllers.partnership

import controllers.actions.MutableFakeDataRetrievalAction
import controllers.base.ControllerSpecBase
import matchers.JsonMatchers
import models.UserAnswers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import services.PartnershipCYAService
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryListRow
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, Value}
import views.html.CheckYourAnswersView

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends ControllerSpecBase with MockitoSugar
  with JsonMatchers with OptionValues with TryValues {

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val partnershipCYAService = mock[PartnershipCYAService]
  private val partnershipName: String = "Partnership name"
  override def fakeApplication(): Application =
    applicationBuilderMutableRetrievalAction(
      mutableFakeDataRetrievalAction,
      extraModules = Seq(bind[PartnershipCYAService].toInstance(partnershipCYAService))).build()

  private def onPageLoadUrl: String = routes.CheckYourAnswersController.onPageLoad().url

  private val list = Seq(SummaryListRow(
    key = Key(Text(Messages("cya.partnershipName")), classes = "govuk-!-width-one-half"),
    value = Value(Text(partnershipName), classes = "govuk-!-width-one-third")
  ))

  override def beforeEach(): Unit = {
    super.beforeEach()
    mutableFakeDataRetrievalAction.setDataToReturn(Some(UserAnswers()))
   }

  "CheckYourAnswers Controller" must {
    "return OK and the correct view for a GET" in {

      val request = FakeRequest(GET, onPageLoadUrl)
      when(partnershipCYAService.partnershipCya(any())(any())).thenReturn(list)

      val result = route(app, httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual OK

      val view = app.injector.instanceOf[CheckYourAnswersView].apply(
        controllers.partnership.routes.DeclarationController.onPageLoad(),
        Seq(
          SummaryListRow(key = Key(Text(Messages("cya.partnershipName")), classes = "govuk-!-width-one-half"),
            value = Value(HtmlContent(partnershipName), classes = "govuk-!-width-one-third"))
        )
      )(request, messages)

      compareResultAndView(result, view)
    }

    "redirect to Session Expired page for a GET when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(app, httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
    }
  }
}
