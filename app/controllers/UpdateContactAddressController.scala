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
import controllers.actions.AuthAction
import controllers.actions.DataRequiredAction
import controllers.actions.DataRetrievalAction
import javax.inject.Inject
import models.Address
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
                                                requireData: DataRequiredAction,
                                                countryOptions: CountryOptions,
                                                subscriptionConnector: SubscriptionConnector
                                              )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      request.user.alreadyEnrolledPspId.map { pspId =>
        subscriptionConnector.getSubscriptionDetails(pspId).flatMap { uaJson =>
          val ua = UserAnswers(uaJson.as[JsObject])
          val json = retrieveRequiredValues(ua) match {
            case Some(Tuple2(url, address)) =>
              Json.obj(
                "addressUrl" -> url,
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
            controllers.company.routes.CompanyPostcodeController.onPageLoad(NormalMode).url,
            ua.getOrException(CompanyAddressPage)
          )
          case Individual => Some(
            controllers.individual.routes.IndividualPostcodeController.onPageLoad(NormalMode).url,
            ua.getOrException(IndividualManualAddressPage)
          )
          case Partnership => Some(
            controllers.partnership.routes.PartnershipPostcodeController.onPageLoad(NormalMode).url,
            ua.getOrException(PartnershipAddressPage)
          )
          case _ => None
        }
    }
  }
    //    ua.get(RegistrationDetailsPage).map { regInfo =>
    //
    //      regInfo.legalStatus match {
    //        case Individual =>
    //          val title: String = individualMessage("viewDetails.title").resolve
    //          Json.obj(
    //            "pageTitle" -> title,
    //            "heading" -> ua.get(IndividualDetailsPage).fold(title)(name => heading(name.fullName)),
    //            "list" -> individualDetails(ua, pspId),
    //            "nextPage" -> nextPage,
    //            "returnUrl" -> appConfig.returnToPspDashboardUrl,
    //            "returnLink" -> ua.get(IndividualDetailsPage)
    //              .fold(messages("site.return_to_dashboard"))(name => messages("site.return_to", name.fullName))
    //          )
    //        case LimitedCompany =>
    //          val title: String = companyMessage("viewDetails.title").resolve
    //          Json.obj(
    //            "pageTitle" -> title,
    //            "heading" -> ua.get(comp.BusinessNamePage).fold(title)(name => heading(name)),
    //            "list" -> companyDetails(ua, pspId),
    //            "nextPage" -> nextPage,
    //            "returnUrl" -> appConfig.returnToPspDashboardUrl,
    //            "returnLink" -> ua.get(comp.BusinessNamePage)
    //              .fold(messages("site.return_to_dashboard"))(name => messages("site.return_to", name))
    //          )
    //        case Partnership =>
    //          val title: String = partnershipMessage("viewDetails.title").resolve
    //          Json.obj(
    //            "pageTitle" -> title,
    //            "heading" -> ua.get(BusinessNamePage).fold(title)(name => heading(name)),
    //            "list" -> partnershipDetails(ua, pspId),
    //            "nextPage" -> nextPage,
    //            "returnUrl" -> appConfig.returnToPspDashboardUrl,
    //            "returnLink" -> ua.get(BusinessNamePage)
    //              .fold(messages("site.return_to_dashboard"))(name => messages("site.return_to", name))
    //          )
    //      }
    //    }.getOrElse(Json.obj())



}
