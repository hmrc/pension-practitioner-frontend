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

package services

import models.Address
import play.api.i18n.Messages
import uk.gov.hmrc.viewmodels.Html

trait CYAService {

  def addressAnswer(addr: Address)(implicit messages: Messages): Html = {
    def addrLineToHtml(l: String): String = s"""<span class="govuk-!-display-block">$l</span>"""

    Html(
      addrLineToHtml(addr.addressLine1) +
        addrLineToHtml(addr.addressLine2) +
        addr.addressLine3.fold("")(addrLineToHtml) +
        addr.addressLine4.fold("")(addrLineToHtml) +
        addr.postcode.fold("")(addrLineToHtml) +
        addrLineToHtml(messages("country." + addr.country))
    )
  }

}
