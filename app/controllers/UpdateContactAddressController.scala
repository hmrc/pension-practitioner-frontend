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

import connectors.SubscriptionConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.actions.AuthAction
import controllers.actions.DataRequiredAction
import controllers.actions.DataRetrievalAction
import javax.inject.Inject
import models.Address
import models.CheckMode
import models.register.RegistrationLegalStatus.Individual
import models.register.RegistrationLegalStatus.LimitedCompany
import models.register.RegistrationLegalStatus.Partnership
import models.NormalMode
import models.UserAnswers
import pages.RegistrationDetailsPage
import pages.company.CompanyAddressPage
import pages.individual.IndividualAddressPage
import pages.individual.IndividualManualAddressPage
import pages.partnership.PartnershipAddressPage
import play.api.i18n.I18nSupport
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import renderer.Renderer
import services.PspDetailsService
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
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
      request.user.alreadyEnrolledPspId.map { pspId =>
          pspDetailsService.extractUserAnswers(request.userAnswers, pspId).flatMap { ua =>
          val json = retrieveRequiredValues(ua) match {
            case Some(Tuple2(url, address)) =>
              Json.obj(
                "continueUrl" -> url,
                "address" -> address.lines(countryOptions)
              )
            case None => Json.obj()
          }
          renderer.render("updateContactAddress.njk", json).map(Ok(_))
        }
      }.getOrElse(
        Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
      )
  }

  private def retrieveRequiredValues(ua: UserAnswers): Option[(String, Address)] = {
    ua.get(RegistrationDetailsPage).flatMap {
      regInfo =>
        regInfo.legalStatus match {
          case LimitedCompany => Some(
            controllers.company.routes.CompanyPostcodeController.onPageLoad(CheckMode).url,
            ua.getOrException(CompanyAddressPage)
          )
          case Individual => Some(
            controllers.individual.routes.IndividualPostcodeController.onPageLoad(CheckMode).url,
            ua.getOrException(IndividualManualAddressPage)
          )
          case Partnership => Some(
            controllers.partnership.routes.PartnershipPostcodeController.onPageLoad(CheckMode).url,
            ua.getOrException(PartnershipAddressPage)
          )
          case _ => None
        }
    }
  }
}
