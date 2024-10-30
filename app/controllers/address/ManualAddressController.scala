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
import connectors.cache.UserAnswersCacheConnector
import controllers.{Retrievals, Variation}
import models.AddressConfiguration.AddressConfiguration
import models.requests.DataRequest
import models.{Address, AddressConfiguration, Mode, TolerantAddress}
import navigators.CompoundNavigator
import pages.company.CompanyAddressPage
import pages.{AddressChange, QuestionPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.json.Json._
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.mvc.{AnyContent, Call, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.address.ManualAddressView

import scala.concurrent.{ExecutionContext, Future}

trait ManualAddressController
    extends FrontendBaseController
    with Retrievals
    with I18nSupport
    with Variation {

  protected def userAnswersCacheConnector: UserAnswersCacheConnector

  protected def navigator: CompoundNavigator

  protected def form(implicit messages: Messages): Form[Address]

  protected def config: FrontendAppConfig

  protected def addressPage: QuestionPage[Address] = CompanyAddressPage

  protected val submitRoute: Mode => Call = _ => Call("", "")

  protected val pageTitleMessageKey: String = "address.title"

  protected val pageTitleEntityTypeMessageKey: Option[String] = None

  protected val h1MessageKey: String = pageTitleMessageKey

  protected def addressConfigurationForPostcodeAndCountry(isUK:Boolean): AddressConfiguration =
    if(isUK) AddressConfiguration.PostcodeFirst else AddressConfiguration.CountryFirst

  protected def get(mode: Mode,
                    name: Option[String],
                    selectedAddress: QuestionPage[TolerantAddress],
                    addressLocation: AddressConfiguration,
                    manualAddressView: ManualAddressView,
                    isUkHintText: Boolean = false)(
                     implicit request: DataRequest[AnyContent],
                     ec: ExecutionContext
                   ): Future[Result] = {
    val preparedForm = request.userAnswers.get(addressPage) match {
      case None => request.userAnswers.get(selectedAddress) match {
        case Some(value) => form.fill(value.toPrepopAddress)
        case None => form
      }
      case Some(value) => form.fill(value)
    }
    val jsonValue: JsObject =  json(mode, name, preparedForm, addressLocation)
    Future.successful(Ok(manualAddressView(
      (jsonValue \ "pageTitle").asOpt[String].getOrElse(""),
      (jsonValue \ "h1").asOpt[String].getOrElse(""),
      (jsonValue \ "postcodeEntry").asOpt[Boolean].getOrElse(false),
      (jsonValue \ "postcodeFirst").asOpt[Boolean].getOrElse(false),
      (jsonValue \ "countries").asOpt[Array[models.Country]].getOrElse(Array.empty[models.Country]),
      submitRoute(mode),
      preparedForm,
      isUkHintText)))
  }

  protected def post(mode: Mode,
                     name: Option[String],
                     addressLocation: AddressConfiguration,
                     manualAddressView: ManualAddressView,
                     isUkHintText: Boolean = false)(
                      implicit request: DataRequest[AnyContent],
                      ec: ExecutionContext
                    ): Future[Result] = {
    form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          val jsonValue: JsObject =  json(mode, name, formWithErrors, addressLocation)
          Future.successful(BadRequest(manualAddressView((jsonValue \ "pageTitle").asOpt[String].getOrElse(""),
            (jsonValue \ "h1").asOpt[String].getOrElse(""),
            (jsonValue \ "postcodeEntry").asOpt[Boolean].getOrElse(false),
            (jsonValue \ "postcodeFirst").asOpt[Boolean].getOrElse(false),
            (jsonValue \ "countries").asOpt[Array[models.Country]].getOrElse(Array.empty[models.Country]),
            submitRoute(mode),
            formWithErrors,
            isUkHintText
          )))
        },
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(addressPage, value))
            answersWithChangeFlag <- Future.fromTry(setChangeFlag(updatedAnswers, AddressChange))
            _ <- userAnswersCacheConnector.save(answersWithChangeFlag.data)
          } yield {
            Redirect(navigator.nextPage(addressPage, mode, answersWithChangeFlag))
          }
      )
  }

  protected def json(
    mode: Mode,
    entityName: Option[String],
    form: Form[Address],
    addressLocation: AddressConfiguration
  )(implicit request: DataRequest[AnyContent]): JsObject = {
    val messages = request2Messages
    val extraJson = addressLocation match {
      case AddressConfiguration.PostcodeFirst =>
        Json.obj(
          "postcodeFirst" -> true,
          "postcodeEntry" -> true,
          "countries" -> jsonCountries(form.data.get("country"), config)(messages)
        )
      case AddressConfiguration.CountryFirst =>
        Json.obj(
          "postcodeEntry" -> true,
          "countries" -> jsonCountries(form.data.get("country"), config)(messages)
        )
      case AddressConfiguration.CountryOnly =>
        Json.obj("countries" -> jsonCountries(form.data.get("country"), config)(messages))
      case _ => Json.obj()
    }

    val pageTitle = pageTitleEntityTypeMessageKey match {
        case Some(key) => messages(pageTitleMessageKey, messages(key))
        case _ => messages(pageTitleMessageKey)
    }
    val h1 = entityName match {
      case Some(e) =>  messages (h1MessageKey, e)
      case _ => messages (h1MessageKey)
    }

    Json.obj(
      "submitUrl" -> submitRoute(mode).url,
      "pageTitle" -> pageTitle,
      "h1" -> h1
    ) ++ extraJson
  }

  private def countryJsonElement(tuple: (String, String),
                                 isSelected: Boolean): JsArray =
    Json.arr(if (isSelected) {
      Json.obj("value" -> tuple._1, "text" -> tuple._2, "selected" -> true)
    } else {
      Json.obj("value" -> tuple._1, "text" -> tuple._2)
    })

  def jsonCountries(countrySelected: Option[String], config: FrontendAppConfig)(
    implicit messages: Messages
  ): JsArray = {
    config.validCountryCodes
      .map(countryCode => (countryCode, messages(s"country.$countryCode")))
      .sortWith(_._2 < _._2)
      .foldLeft(JsArray(Seq(Json.obj("value" -> "", "text" -> "")))) {
        (acc, nextCountryTuple) =>
          acc ++ countryJsonElement(
            nextCountryTuple,
            countrySelected.contains(nextCountryTuple._1)
          )
      }
  }
}
