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

import models.UserAnswers
import pages.WhatTypeBusinessPage
import pages.individual.IndividualDetailsPage
import models.WhatTypeBusiness.Yourselfasindividual
import models.WhatTypeBusiness.Companyorpartnership
import models.register.BusinessRegistrationType
import models.register.BusinessType

import scala.concurrent.Future
import models.requests.DataRequest
import pages.individual.IndividualEmailPage
import pages.register.AreYouUKCompanyPage
import pages.register.BusinessRegistrationTypePage
import pages.register.BusinessTypePage
import play.api.mvc.Results.Redirect
import play.api.mvc.AnyContent
import play.api.mvc.Result

object DataRetrievals {

  private def futureSessionExpired:Future[Result] = Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))

  def retrievePspNameAndEmail(block: (String, String) => Future[Result])(implicit request: DataRequest[AnyContent]): Future[Result] = {
    val ua = request.userAnswers

    val retrievedValues = ua.get(WhatTypeBusinessPage) match {
      case Some(Yourselfasindividual) =>
        retrieveIndividualNameAndEmail(ua)
      case Some(Companyorpartnership) =>
        retrieveCompanyOrPartnershipNameAndEmail(ua)
      case _ => None
    }

    retrievedValues match {
      case Some(t) => block(t._1, t._2)
      case _ => futureSessionExpired
    }
  }

  private def retrieveCompanyOrPartnershipNameAndEmail(ua:UserAnswers):Option[(String,String)] = {
      (ua.get(AreYouUKCompanyPage), ua.get(BusinessTypePage), ua.get(BusinessRegistrationTypePage)) match {
        case (Some(true), Some(BusinessType.LimitedCompany | BusinessType.UnlimitedCompany), _) => companyNameAndEmail(ua)
        case (Some(true), Some(_), _) => partnershipNameAndEmail(ua)
        case (Some(false), _, Some(BusinessRegistrationType.Company)) => companyNameAndEmail(ua)
        case (Some(false), _, Some(_)) => partnershipNameAndEmail(ua)
        case _ => None
      }
  }

  private def companyNameAndEmail(ua:UserAnswers):Option[(String,String)] = {
    (ua.get(pages.company.BusinessNamePage), ua.get(pages.company.CompanyEmailPage)) match {
      case (Some(n), Some(e)) => Some(Tuple2(n, e))
      case _ => None
    }
  }

  private def partnershipNameAndEmail(ua:UserAnswers):Option[(String,String)] = {
    (ua.get(pages.partnership.BusinessNamePage), ua.get(pages.partnership.PartnershipEmailPage)) match {
      case (Some(n), Some(e)) => Some(Tuple2(n, e))
      case _ => None
    }
  }

  private def retrieveIndividualNameAndEmail(ua:UserAnswers):Option[(String,String)] = {
    (ua.get(IndividualDetailsPage), ua.get(IndividualEmailPage)) match {
      case (Some(i), Some(e)) => Some(Tuple2(i.fullName, e))
      case _ => None
    }
  }
}
