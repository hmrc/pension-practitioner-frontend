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

package utils

import play.api.Logging
import play.api.i18n.Messages
import play.api.mvc.Request
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases.RadioItem
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text

import scala.concurrent.Future

object TwirlMigration extends Logging {
  def duoTemplate(nunjucks: => Future[Html], twirl: => Html)(implicit request: Request[_]): Future[Html] = {
    val useTwirl = request.session.get("twirl").exists {
      case "true" => true
      case _ => false
    }
    if(useTwirl) {
      logger.warn("Using twirl template")
      Future.successful(twirl)
    } else nunjucks
  }

  def toTwirlRadios(nunjucksRadios: Seq[uk.gov.hmrc.viewmodels.Radios.Item])(implicit messages: Messages): Seq[RadioItem] = {
    nunjucksRadios.map(radio => {
      RadioItem(content = Text(radio.text.resolve), value = Some(radio.value))
    })
  }
}
