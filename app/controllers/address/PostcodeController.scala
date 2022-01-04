/*
 * Copyright 2022 HM Revenue & Customs
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
import connectors.AddressLookupConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import models.requests.DataRequest
import models.{Mode, TolerantAddress}
import navigators.CompoundNavigator
import pages.QuestionPage
import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json.{JsArray, Json, JsObject}
import play.api.mvc.{Result, AnyContent}
import renderer.Renderer
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import forms.FormsHelper.formWithError

import scala.concurrent.{Future, ExecutionContext}

trait PostcodeController extends FrontendBaseController with Retrievals {

  protected def renderer: Renderer
  protected def userAnswersCacheConnector: UserAnswersCacheConnector
  protected def navigator: CompoundNavigator
  protected def form(implicit messages: Messages): Form[String]
  protected def addressLookupConnector: AddressLookupConnector
  protected def viewTemplate = "address/postcode.njk"

  def get(json: Form[String] => JsObject)
         (implicit request: DataRequest[AnyContent], ec: ExecutionContext, messages: Messages): Future[Result] = {

    renderer.render(viewTemplate, json(form)).map(Ok(_))
  }

  def post(mode: Mode, formToJson: Form[String] => JsObject, postcodePage: QuestionPage[Seq[TolerantAddress]], errorMessage: String)
          (implicit request: DataRequest[AnyContent], ec: ExecutionContext, hc: HeaderCarrier, messages: Messages): Future[Result] = {
    form.bindFromRequest().fold(
      formWithErrors =>
        renderer.render(viewTemplate, formToJson(formWithErrors)).map(BadRequest(_)),
      value =>
          addressLookupConnector.addressLookupByPostCode(value).flatMap {
            case Nil =>
              val json = formToJson(formWithError(form, errorMessage))
                renderer.render(viewTemplate, json).map(BadRequest(_))

            case addresses =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(postcodePage, addresses))
                _ <- userAnswersCacheConnector.save(updatedAnswers.data)
              } yield Redirect(navigator.nextPage(postcodePage, mode, updatedAnswers))
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

  def jsonCountries(countrySelected: Option[String], config: FrontendAppConfig)(implicit messages: Messages): JsArray =
    config.validCountryCodes
      .map(countryCode => (countryCode, messages(s"country.$countryCode")))
      .sortWith(_._2 < _._2)
      .foldLeft(JsArray(Seq(Json.obj("value" -> "", "text" -> "")))) { (acc, nextCountryTuple) =>
        acc ++ countryJsonElement(nextCountryTuple, countrySelected.contains(nextCountryTuple._1))
      }

}

