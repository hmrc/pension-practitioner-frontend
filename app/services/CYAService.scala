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
