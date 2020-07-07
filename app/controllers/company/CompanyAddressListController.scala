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

package controllers.company

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.address.AddressListFormProvider
import javax.inject.Inject
import models.{Mode, TolerantAddress}
import models.requests.DataRequest
import navigators.CompoundNavigator
import pages.company.{CompanyAddressListPage, CompanyAddressPage, CompanyNamePage, CompanyPostcodePage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.{JsObject, Json, Writes}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.countryOptions.CountryOptions

import scala.concurrent.{ExecutionContext, Future}

class CompanyAddressListController @Inject()(override val messagesApi: MessagesApi,
                                             userAnswersCacheConnector: UserAnswersCacheConnector,
                                             navigator: CompoundNavigator,
                                             identify: IdentifierAction,
                                             getData: DataRetrievalAction,
                                             requireData: DataRequiredAction,
                                             formProvider: AddressListFormProvider,
                                             val controllerComponents: MessagesControllerComponents,
                                             countryOptions: CountryOptions,
                                             renderer: Renderer
                                         )(implicit ec: ExecutionContext) extends FrontendBaseController
                                          with Retrievals with I18nSupport with NunjucksSupport {


  def form(implicit messages: Messages): Form[Int] =
    formProvider(messages("addressList.error.invalid", messages("company")))

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async {
      implicit request =>
        getJson(mode, form) { json =>
          renderer.render("address/addressList.njk", json).map(Ok(_))
        }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async {
      implicit request =>

          form.bindFromRequest().fold(
            formWithErrors => {
              getJson(mode, formWithErrors) { json =>
                renderer.render("address/addressList.njk", json).map(BadRequest(_))
              }
            },
            value =>
              CompanyPostcodePage.retrieve.right.map { addresses =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(CompanyAddressPage, addresses(value).toAddress))
                  _ <- userAnswersCacheConnector.save(updatedAnswers.data)
                } yield Redirect(navigator.nextPage(CompanyAddressListPage, mode, updatedAnswers))
              }
          )

    }

  private def getJson(mode: Mode, form: Form[Int])(block: JsObject => Future[Result])
                     (implicit w: Writes[Form[Int]], messages: Messages, request: DataRequest[AnyContent]): Future[Result] =
    (CompanyNamePage and CompanyPostcodePage).retrieve.right.map {
      case companyName ~ addresses =>
        val json = Json.obj(
          "form" -> form,
          "entityType" -> messages("company"),
          "entityName" -> companyName,
          "addresses" -> transformAddressesForTemplate(addresses),
          "submitUrl" -> routes.CompanyAddressListController.onSubmit(mode).url,
          "enterManuallyUrl" -> routes.CompanyAddressController.onPageLoad(mode).url
        )
        block(json)
    }

  private def transformAddressesForTemplate(addresses:Seq[TolerantAddress]):Seq[JsObject] = {
    for ((row, i) <- addresses.zipWithIndex) yield {
      Json.obj(
        "value" -> i,
        "text" -> row.print(countryOptions)
      )
    }
  }
}
