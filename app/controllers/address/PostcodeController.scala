/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.mvc.{AnyContent, Result}
import renderer.Renderer
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import forms.FormsHelper.formWithError
import play.twirl.api.Html
import utils.TwirlMigration

import scala.concurrent.{ExecutionContext, Future}

trait PostcodeController extends FrontendBaseController with Retrievals {

  protected def renderer: Renderer
  protected def userAnswersCacheConnector: UserAnswersCacheConnector
  protected def navigator: CompoundNavigator
  protected def form(implicit messages: Messages): Form[String]
  protected def addressLookupConnector: AddressLookupConnector
  protected def viewTemplate = "address/postcode.njk"

  protected def twirlMigration: TwirlMigration

  def get(json: Form[String] => JsObject, twirlTemplate: Option[Html] = None)
         (implicit request: DataRequest[AnyContent], ec: ExecutionContext, messages: Messages): Future[Result] = {
    twirlTemplate match {
      case Some(template) =>
        twirlMigration.duoTemplate(
          renderer.render(viewTemplate, json(form)),
          template
        ).map(Ok(_))
      case None => renderer.render(viewTemplate, json(form)).map(Ok(_))
    }
  }

  def post(mode: Mode, formToJson: Form[String] => JsObject, postcodePage: QuestionPage[Seq[TolerantAddress]],
           errorMessage: String, formToTwirlTemplate: Option[Form[String] => Html] = None)
          (implicit request: DataRequest[AnyContent], ec: ExecutionContext, hc: HeaderCarrier, messages: Messages): Future[Result] = {
    form.bindFromRequest().fold(
      formWithErrors =>
        formToTwirlTemplate match {
          case Some(formToTemplate) =>
            twirlMigration.duoTemplate(
              renderer.render(viewTemplate, formToJson(formWithErrors)),
              formToTemplate(formWithErrors)
            ).map(BadRequest(_))
          case None => renderer.render(viewTemplate, formToJson(formWithErrors)).map(BadRequest(_))
        },
      value =>
          addressLookupConnector.addressLookupByPostCode(value).flatMap {
            case Nil =>
              val formWithErrors = formWithError(form, errorMessage)
              val json = formToJson(formWithErrors)
              formToTwirlTemplate match {
                case Some(formToTemplate) =>
                  twirlMigration.duoTemplate(
                    renderer.render(viewTemplate, json),
                    formToTemplate(formWithErrors)
                  ).map(BadRequest(_))
                case None => renderer.render(viewTemplate, json).map(BadRequest(_))
              }

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

