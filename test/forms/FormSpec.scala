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

package forms

import config.FrontendAppConfig
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{Assertion, OptionValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.{Form, FormError}
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.{Application, Environment}

trait FormSpec extends AnyWordSpec with OptionValues with Matchers with GuiceOneAppPerSuite {

  def checkForError(form: Form[_], data: Map[String, String], expectedErrors: Seq[FormError]): Assertion = {

    form.bind(data).fold(
      formWithErrors => {
        for (error <- expectedErrors) formWithErrors.errors should contain(FormError(error.key, error.message, error.args))
        formWithErrors.errors.size shouldBe expectedErrors.size
      },
      form => {
        fail("Expected a validation error when binding the form, but it was bound successfully.")
      }
    )
  }

  def error(key: String, value: String, args: Any*): Seq[FormError] = Seq(FormError(key, value, args))

  lazy val emptyForm: Map[String, String] = Map[String, String]()

  def fakeApp(): Application = new GuiceApplicationBuilder()
    .configure(
      //turn off metrics
      "metrics.jvm" -> false,
      "metrics.enabled" -> false
    )
    .build()

  def injector: Injector = fakeApp().injector

  def frontendAppConfig: FrontendAppConfig = injector.instanceOf[FrontendAppConfig]

  def messagesApi: MessagesApi = injector.instanceOf[MessagesApi]

  def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

  implicit def messages: Messages = messagesApi.preferred(fakeRequest)

  def environment: Environment = injector.instanceOf[Environment]
}
