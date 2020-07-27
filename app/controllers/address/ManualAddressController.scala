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

import config.FrontendAppConfig
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import models.requests.DataRequest
import models.{Address, Mode}
import navigators.CompoundNavigator
import pages.QuestionPage
import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.mvc.{AnyContent, Result}
import renderer.Renderer
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

trait ManualAddressController extends FrontendBaseController with Retrievals {

  protected def renderer: Renderer

  protected def userAnswersCacheConnector: UserAnswersCacheConnector

  protected def navigator: CompoundNavigator

  protected def form(implicit messages: Messages): Form[Address]

  protected def viewTemplate = "address/manualAddress.njk"

  def get(json: Form[Address] => JsObject)
         (implicit request: DataRequest[AnyContent], ec: ExecutionContext, messages: Messages): Future[Result] = {
    renderer.render(viewTemplate, json(form)).map(Ok(_))
  }

  def post(mode: Mode, json: Form[Address] => JsObject, addressPage: QuestionPage[Address])
          (implicit request: DataRequest[AnyContent], ec: ExecutionContext, hc: HeaderCarrier, messages: Messages): Future[Result] = {
    form.bindFromRequest().fold(
      formWithErrors =>
        renderer.render(viewTemplate, json(formWithErrors)).map(BadRequest(_)),
      value =>
        for {
          updatedAnswers <- Future.fromTry(request.userAnswers.set(addressPage, value))
          _ <- userAnswersCacheConnector.save(updatedAnswers.data)
        } yield {
          Redirect(navigator.nextPage(addressPage, mode, updatedAnswers))
        }

    )
  }

  private def countryJsonElement(tuple: (String, String), isSelected: Boolean): JsArray = Json.arr(
    if (isSelected) {
      Json.obj(
        "value" -> tuple._1,
        "text" -> tuple._2,
        "selected" -> true
      )
    } else {
      Json.obj(
        "value" -> tuple._1,
        "text" -> tuple._2
      )
    }
  )

  def jsonCountries(countrySelected: Option[String], config: FrontendAppConfig)(implicit messages: Messages): JsArray = {
    config.validCountryCodes
      .map(countryCode => (countryCode, messages(s"country.$countryCode")))
      .sortWith(_._2 < _._2)
      .foldLeft(JsArray(Seq(Json.obj("value" -> "", "text" -> "")))) { (acc, nextCountryTuple) =>
        acc ++ countryJsonElement(nextCountryTuple, countrySelected.contains(nextCountryTuple._1))
      }
  }

}

