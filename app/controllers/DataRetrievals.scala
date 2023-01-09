/*
 * Copyright 2023 HM Revenue & Customs
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
import models.register.RegistrationLegalStatus
import models.requests.DataRequest
import pages.RegistrationDetailsPage
import pages.individual.{IndividualDetailsPage, IndividualEmailPage}
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Result}

import scala.concurrent.Future

object DataRetrievals {

  def retrievePspNameAndEmail(block: (String, String) => Future[Result])(implicit request: DataRequest[AnyContent]): Future[Result] = {
    val ua = request.userAnswers
    val detailsOption = ua.get(RegistrationDetailsPage).flatMap { regInfo =>
      if(regInfo.legalStatus == RegistrationLegalStatus.Individual) {
        individualNameAndEmail(ua)
      } else {
        organisationNameAndEmail(ua)
      }
    }

    detailsOption.map(x => block(x._1, x._2))
      .getOrElse(Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad())))
  }

  private def organisationNameAndEmail(ua:UserAnswers):Option[(String,String)] = {
    (ua.get(pages.company.BusinessNamePage), ua.get(pages.company.CompanyEmailPage)) match {
      case (Some(n), Some(e)) => Some(Tuple2(n, e))
      case _ => None
    }
  }

  private def individualNameAndEmail(ua:UserAnswers):Option[(String,String)] = {
    (ua.get(IndividualDetailsPage), ua.get(IndividualEmailPage)) match {
      case (Some(i), Some(e)) => Some(Tuple2(i.fullName, e))
      case _ => None
    }
  }
}
