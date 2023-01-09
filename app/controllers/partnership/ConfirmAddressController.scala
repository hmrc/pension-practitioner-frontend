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

package controllers.partnership

import config.FrontendAppConfig
import connectors.RegistrationConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.ConfirmAddressFormProvider
import javax.inject.Inject
import models.register.RegistrationLegalStatus.Partnership
import models.register.{BusinessType, Organisation}
import models.requests.DataRequest
import models.{NormalMode, TolerantAddress, UserAnswers}
import navigators.CompoundNavigator
import pages.RegistrationInfoPage
import pages.partnership.{ConfirmAddressPage, BusinessUTRPage, BusinessNamePage}
import pages.register.BusinessTypePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{Json, JsObject}
import play.api.mvc.{Result, AnyContent, MessagesControllerComponents, Action}
import renderer.Renderer
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.{NunjucksSupport, Radios}
import utils.countryOptions.CountryOptions

import scala.concurrent.{Future, ExecutionContext}

class ConfirmAddressController @Inject()(override val messagesApi: MessagesApi,
                                         userAnswersCacheConnector: UserAnswersCacheConnector,
                                         navigator: CompoundNavigator,
                                         authenticate: AuthAction,
                                         getData: DataRetrievalAction,
                                         registrationConnector:RegistrationConnector,
                                         requireData: DataRequiredAction,
                                         formProvider: ConfirmAddressFormProvider,
                                         val controllerComponents: MessagesControllerComponents,
                                         countryOptions: CountryOptions,
                                         config: FrontendAppConfig,
                                         renderer: Renderer
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with NunjucksSupport with Retrievals {

  private val form = formProvider("confirmAddress.partnership.error.required")

  private def retrieveDataForRegistration(block: (String, String, BusinessType) => Future[Result])(implicit
    request: DataRequest[AnyContent]): Future[Result] = {
    (request.userAnswers.get(BusinessNamePage),
      request.userAnswers.get(BusinessUTRPage),
      request.userAnswers.get(BusinessTypePage) ) match {
      case (Some(pspName), Some(utr), Some(businessType)) =>
          block (pspName, utr, businessType)
      case _  => Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
    }
  }

  private def formattedAddress(tolerantAddress:TolerantAddress) =
    Json.toJson(tolerantAddress.lines(countryOptions))

  def onPageLoad(): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      retrieveDataForRegistration { (pspName, utr, businessType) =>
        val organisation = Organisation(pspName, businessType)
       registrationConnector.registerWithIdOrganisation(utr, organisation, Partnership).flatMap { reg =>

          val ua = request.userAnswers
            .setOrException(ConfirmAddressPage, reg.response.address)
            .setOrException(BusinessNamePage, reg.response.organisation.organisationName)
            .setOrException(RegistrationInfoPage, reg.info)

          userAnswersCacheConnector.save(ua.data).flatMap{ _ =>
            val json = Json.obj(
              "form" -> form,
              "pspName" -> pspName,
              "entityName" -> "partnership",
              "address" -> formattedAddress(reg.response.address),
              "submitUrl" -> routes.ConfirmAddressController.onSubmit().url,
              "radios" -> Radios.yesNo(form("value")))

            renderer.render("confirmAddress.njk", json).map(Ok(_))
          }
        } recoverWith {
         case _: NotFoundException =>
           Future.successful(Redirect(controllers.register.routes.BusinessDetailsNotFoundController.onPageLoad()))
       }
      }
  }

  def onSubmit(): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      BusinessNamePage.retrieve.map { pspName =>
        form.bindFromRequest().fold(
          formWithErrors => {

            request.userAnswers.get(ConfirmAddressPage) match {
              case Some(addr) =>
                val json = Json.obj(
                  "form"   -> formWithErrors,
                  "entityName" -> "partnership",
                  "pspName" -> pspName,
                  "address" -> formattedAddress(addr),
                  "submitUrl"   -> routes.ConfirmAddressController.onSubmit().url,
                  "radios" -> Radios.yesNo(formWithErrors("value"))
                )

                renderer.render("confirmAddress.njk", json).map(BadRequest(_))

              case _ => Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
            }

          },
          {
            case true =>
              Future.successful(Redirect(navigator.nextPage(ConfirmAddressPage, NormalMode, request.userAnswers)))
            case false =>
              val updatedAnswers = request.userAnswers
                .removeOrException(ConfirmAddressPage)
                .removeOrException(RegistrationInfoPage)
                userAnswersCacheConnector.save(updatedAnswers.data).map { jsValue =>
                  Redirect(navigator.nextPage(ConfirmAddressPage, NormalMode, UserAnswers(jsValue.as[JsObject])))
                }
          }
        )
      }
  }
}
