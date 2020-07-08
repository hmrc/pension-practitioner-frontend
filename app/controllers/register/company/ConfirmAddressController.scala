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

package controllers.register.company

import config.FrontendAppConfig
import connectors.RegistrationConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.DataRetrievals
import controllers.actions._
import forms.register.company.ConfirmAddressFormProvider
import javax.inject.Inject
import models.NormalMode
import models.TolerantAddress
import models.register.BusinessType
import models.register.Organisation
import models.register.OrganisationRegisterWithIdResponse
import models.register.OrganisationRegistration
import models.register.RegistrationCustomerType
import models.register.RegistrationInfo
import models.register.RegistrationLegalStatus
import models.register.RegistrationLegalStatus.LimitedCompany
import models.requests.DataRequest
import navigators.CompoundNavigator
import pages.register.BusinessTypePage
import pages.register.company.CompanyNamePage
import pages.register.company.RegistrationInfoPage
import pages.register.company.BusinessUTRPage
import pages.register.company.ConfirmAddressPage
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Result
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport
import uk.gov.hmrc.viewmodels.Radios
import utils.countryOptions.CountryOptions

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class ConfirmAddressController @Inject()(override val messagesApi: MessagesApi,
                                      userAnswersCacheConnector: UserAnswersCacheConnector,
                                      navigator: CompoundNavigator,
                                      identify: IdentifierAction,
                                      getData: DataRetrievalAction,
                                      registrationConnector:RegistrationConnector,
                                      requireData: DataRequiredAction,
                                      formProvider: ConfirmAddressFormProvider,
                                      val controllerComponents: MessagesControllerComponents,
                                      countryOptions: CountryOptions,
                                      config: FrontendAppConfig,
                                      renderer: Renderer
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with NunjucksSupport {

  private val form = formProvider()

  private def retrieveDataForRegistration(block: (String, String, BusinessType) => Future[Result])(implicit
    request: DataRequest[AnyContent]): Future[Result] = {
    (request.userAnswers.get(CompanyNamePage),
      request.userAnswers.get(BusinessUTRPage),
      request.userAnswers.get(BusinessTypePage) ) match {
      case (Some(pspName), Some(utr), Some(businessType)) =>
          block (pspName, utr, businessType)
      case _  => Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
    }
  }

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      retrieveDataForRegistration { (pspName, utr, businessType) =>
        val organisation = Organisation(pspName,businessType)
       registrationConnector.registerWithIdOrganisation(utr, organisation, LimitedCompany).flatMap { reg =>
       // val or = OrganisationRegistration(
       //   OrganisationRegisterWithIdResponse(
       //     organisation,
       //     TolerantAddress(Some("addr1"), Some("addr2"), None, None, Some(""), Some(""))
       //   ),
       //   RegistrationInfo(RegistrationLegalStatus.LimitedCompany, "", false, RegistrationCustomerType.UK, None, None)
       // )co
       //
       // Future.successful(or).flatMap{ reg =>

          val ua = request.userAnswers
            .setOrException(ConfirmAddressPage, reg.response.address)
            .setOrException(CompanyNamePage, reg.response.organisation.organisationName)
            .setOrException(RegistrationInfoPage, reg.info)

         val formattedAddress = Json.obj(
           "addr1" -> reg.response.address.addressLine1.getOrElse[String](""),
           "addr2" -> reg.response.address.addressLine2.getOrElse[String](""),
           "addr3" -> reg.response.address.addressLine3.getOrElse[String](""),
           "addr4" -> reg.response.address.addressLine4.getOrElse[String](""),
           "postcode" -> reg.response.address.postcode.getOrElse[String](""),
           "country" -> countryOptions.getCountryNameFromCode(reg.response.address.country.getOrElse[String](""))
         )

          userAnswersCacheConnector.save(ua.data).flatMap{ _ =>
            val json = Json.obj(
              "form" -> form, "pspName" -> pspName,
              "address" -> formattedAddress,
              "submitUrl" -> routes.ConfirmAddressController.onSubmit().url,
              "radios" -> Radios.yesNo(form("value")))

            renderer.render("register/company/confirmAddress.njk", json).map(Ok(_))
          }
        }
      }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      DataRetrievals.retrieveCompanyName { pspName =>
        form.bindFromRequest().fold(
          formWithErrors => {

            val json = Json.obj(
              "form"   -> formWithErrors,
              "pspName" -> pspName,
              "submitUrl"   -> routes.ConfirmAddressController.onSubmit().url,
              "radios" -> Radios.yesNo(formWithErrors("value"))
            )

            renderer.render("register/company/confirmAddress.njk", json).map(BadRequest(_))
          },
          {
            case true =>
              Future.successful(Redirect(navigator.nextPage(ConfirmAddressPage, NormalMode, request.userAnswers)))
            case false =>
              val updatedAnswers = request.userAnswers
                .removeOrException(ConfirmAddressPage)
                .removeOrException(RegistrationInfoPage)
                userAnswersCacheConnector.save(updatedAnswers.data).map { _ =>
                  Redirect(controllers.routes.SessionExpiredController.onPageLoad())
                }
          }
        )
      }
  }
}
