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

package base

import com.kenshoo.play.metrics.Metrics
import config.FrontendAppConfig
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice._
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestMetrics

trait SpecBase extends PlaySpec with GuiceOneAppPerSuite {

  protected implicit val hc: HeaderCarrier = HeaderCarrier()

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      //turn off metrics
      "metrics.jvm"     -> false,
      "metrics.enabled" -> false
    )
    .overrides(bind[Metrics].toInstance(new TestMetrics))
    .build()

  protected def injector: Injector = fakeApplication().injector

  protected def frontendAppConfig: FrontendAppConfig = injector.instanceOf[FrontendAppConfig]

  protected def messagesApi: MessagesApi = injector.instanceOf[MessagesApi]

  protected def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

  protected implicit def messages: Messages = messagesApi.preferred(fakeRequest)

}
