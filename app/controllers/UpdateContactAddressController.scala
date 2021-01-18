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

package controllers

import controllers.actions.AuthAction
import controllers.actions.DataRetrievalAction
import javax.inject.Inject
import models.Address
import models.CheckMode
import models.register.RegistrationLegalStatus.Individual
import models.register.RegistrationLegalStatus.LimitedCompany
import models.register.RegistrationLegalStatus.Partnership
import models.UserAnswers
import pages.RegistrationDetailsPage
import pages.company.CompanyAddressPage
import pages.individual.IndividualManualAddressPage
import pages.partnership.PartnershipAddressPage
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Result
import renderer.Renderer
import services.PspDetailsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.AuthMustHaveEnrolment
import utils.countryOptions.CountryOptions

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class UpdateContactAddressController @Inject()(
                                                val controllerComponents: MessagesControllerComponents,
                                                renderer: Renderer,
                                                @AuthMustHaveEnrolment authenticate: AuthAction,
                                                getData: DataRetrievalAction,
                                                countryOptions: CountryOptions,
                                                pspDetailsService: PspDetailsService
                                              )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>
      pspDetailsService.getUserAnswers(request.userAnswers, request.user.pspIdOrException).flatMap {
        retrieveRequiredValues(_) match {
          case Some(Tuple2(url, address)) =>
            val json = Json.obj(
              "continueUrl" -> url,
              "address" -> address.lines(countryOptions)
            )
            renderer.render("updateContactAddress.njk", json).map(Ok(_))
          case None => Future.successful(sessionExpired)
        }
      }
  }

  private def retrieveRequiredValues(ua: UserAnswers): Option[(String, Address)] = {
    ua.get(RegistrationDetailsPage).flatMap {
      regInfo =>
        regInfo.legalStatus match {
          case LimitedCompany => Some(
            Tuple2(
              controllers.company.routes.CompanyPostcodeController.onPageLoad(CheckMode).url,
              ua.getOrException(CompanyAddressPage)
            )
          )
          case Individual => Some(
            Tuple2(
              controllers.individual.routes.IndividualPostcodeController.onPageLoad(CheckMode).url,
              ua.getOrException(IndividualManualAddressPage)
            )
          )
          case Partnership => Some(
            Tuple2(
              controllers.partnership.routes.PartnershipPostcodeController.onPageLoad(CheckMode).url,
              ua.getOrException(PartnershipAddressPage)
            )
          )
          case _ => None
        }
    }
  }

  private def sessionExpired:Result = Redirect(controllers.routes.SessionExpiredController.onPageLoad())
}
