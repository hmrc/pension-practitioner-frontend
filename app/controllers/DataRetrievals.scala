/*
 * Copyright 2020 HM Revenue & Customs
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

import pages.WhatTypeBusinessPage
import pages.individual.IndividualDetailsPage
import models.WhatTypeBusiness.Yourselfasindividual
import pages.partnership.BusinessNamePage
import models.WhatTypeBusiness.Companyorpartnership
import models.register.BusinessType

import scala.concurrent.Future
import models.requests.DataRequest
import pages.company.CompanyEmailPage
import pages.individual.IndividualEmailPage
import pages.register.BusinessTypePage
import play.api.mvc.Results.Redirect
import play.api.mvc.AnyContent
import play.api.mvc.Result

object DataRetrievals {

  def retrievePspNameAndEmail(block: (String, String) => Future[Result])(implicit request: DataRequest[AnyContent]): Future[Result] = {
    val ua = request.userAnswers
    (ua.get(WhatTypeBusinessPage),
      ua.get(BusinessTypePage),
      ua.get(IndividualDetailsPage),
      ua.get(BusinessNamePage),
      ua.get(IndividualEmailPage),
      ua.get(CompanyEmailPage)) match {
      case (Some(Yourselfasindividual), _, Some(i), _, Some(e), _) => block(i.fullName, e)
      case (Some(Companyorpartnership), Some(BusinessType.LimitedCompany | BusinessType.UnlimitedCompany), _, Some(b), _, Some(e)) => block(b, e)
      case _ => Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
    }
  }
}
