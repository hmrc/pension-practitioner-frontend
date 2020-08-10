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
import models.register.InternationalRegion.{EuEea, RestOfTheWorld}
import models.register.RegistrationInfo
import models.requests.DataRequest
import models.{Address, Mode, TolerantAddress}
import navigators.CompoundNavigator
import pages.{QuestionPage, RegistrationInfoPage}
import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.mvc.{AnyContent, Result}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import utils.countryOptions.CountryOptions

import scala.concurrent.{ExecutionContext, Future}

trait NonUKManualAddressController extends FrontendBaseController with Retrievals {

  protected def renderer: Renderer
  protected def countryOptions: CountryOptions
  protected def userAnswersCacheConnector: UserAnswersCacheConnector

  protected def navigator: CompoundNavigator

  protected def form(implicit messages: Messages): Form[Address]

  protected def viewTemplate = "address/nonUKAddress.njk"

  def get(json: Form[Address] => JsObject, addressPage: QuestionPage[TolerantAddress])
         (implicit request: DataRequest[AnyContent], ec: ExecutionContext, messages: Messages): Future[Result] = {
    val formFilled = request.userAnswers.get(addressPage).fold(form)(v => form.fill(v.toAddress))
    renderer.render(viewTemplate, json(formFilled)).map(Ok(_))
  }

  def post(mode: Mode, json: Form[Address] => JsObject, addressPage: QuestionPage[TolerantAddress],
           regCall: Address => Future[RegistrationInfo])
          (implicit request: DataRequest[AnyContent], ec: ExecutionContext, messages: Messages): Future[Result] = {
    form.bindFromRequest().fold(
      formWithErrors =>
        renderer.render(viewTemplate, json(formWithErrors)).map(BadRequest(_)),
      address => {

        val answersWithRegInfo = countryOptions.regions(address.country) match {
          case RestOfTheWorld => Future.fromTry(request.userAnswers.remove(RegistrationInfoPage))
          case EuEea => regCall(address).flatMap { registrationInfo =>
              Future.fromTry(request.userAnswers.set(RegistrationInfoPage, registrationInfo))}
          case _ => Future.successful(request.userAnswers)
        }

        for {
          answersWithReg <- answersWithRegInfo
          updatedAnswers <- Future.fromTry(answersWithReg.set(addressPage, address.toTolerantAddress))
          _ <- userAnswersCacheConnector.save(updatedAnswers.data)
        } yield {
          Redirect(navigator.nextPage(addressPage, mode, updatedAnswers))
        }
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

