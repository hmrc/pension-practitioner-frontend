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

package controllers.address

import connectors.cache.UserAnswersCacheConnector
import controllers.{Retrievals, Variation}
import models.requests.DataRequest
import models.{Address, Mode, TolerantAddress}
import navigators.CompoundNavigator
import pages.{AddressChange, QuestionPage}
import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AnyContent, Result}
import renderer.Renderer
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import utils.countryOptions.CountryOptions

import scala.concurrent.{ExecutionContext, Future}

trait AddressListController extends FrontendBaseController with Retrievals with Variation {

  protected def renderer: Renderer
  protected def userAnswersCacheConnector: UserAnswersCacheConnector
  protected def navigator: CompoundNavigator
  protected def form(implicit messages: Messages): Form[Int]
  protected def viewTemplate = "address/addressList.njk"

  def get(json: Form[Int] => JsObject)(implicit request: DataRequest[AnyContent], ec: ExecutionContext, messages: Messages): Future[Result] =
          renderer.render(viewTemplate, json(form)).map(Ok(_))

  def post(mode: Mode, json: Form[Int] => JsObject, pages: AddressPages)
          (implicit request: DataRequest[AnyContent], ec: ExecutionContext, hc: HeaderCarrier, messages: Messages): Future[Result] = {
        form.bindFromRequest().fold(
          formWithErrors =>
            renderer.render(viewTemplate, json(formWithErrors)).map(BadRequest(_)),
          value =>
            pages.postcodePage.retrieve.right.map { addresses =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(pages.addressPage,
                  addresses(value).copy(country = Some("GB")).toAddress))
                answersWithChangeFlag <- Future.fromTry(setChangeFlag(updatedAnswers, AddressChange))
                _ <- userAnswersCacheConnector.save(answersWithChangeFlag.data)
              } yield Redirect(navigator.nextPage(pages.addressListPage, mode, answersWithChangeFlag))
            }
        )
  }

  def transformAddressesForTemplate(addresses:Seq[TolerantAddress], countryOptions: CountryOptions):Seq[JsObject] = {
    for ((row, i) <- addresses.zipWithIndex) yield {
      Json.obj(
        "value" -> i,
        "text" -> row.print(countryOptions)
      )
    }
  }

}

case class AddressPages(postcodePage: QuestionPage[Seq[TolerantAddress]],
                        addressListPage: QuestionPage[Int],
                        addressPage: QuestionPage[Address])
