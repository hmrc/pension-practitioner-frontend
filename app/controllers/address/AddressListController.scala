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

import connectors.cache.UserAnswersCacheConnector
import controllers.{Retrievals, Variation}
import models.requests.DataRequest
import models.{Address, Mode, TolerantAddress}
import navigators.CompoundNavigator
import pages.{AddressChange, QuestionPage}
import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AnyContent, Call, Result}
import play.twirl.api.Html
import renderer.Renderer
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.countryOptions.CountryOptions
import viewmodels.CommonViewModelTwirl

import scala.concurrent.{ExecutionContext, Future}

trait AddressListController extends FrontendBaseController with Retrievals with Variation {

  protected def renderer: Renderer
  protected def userAnswersCacheConnector: UserAnswersCacheConnector
  protected def navigator: CompoundNavigator
  protected def form(implicit messages: Messages): Form[Int]

  def get(json: Form[Int] => JsObject, onSubmitCall: Call, manualUrl: String, twirlView: (CommonViewModelTwirl, Seq[RadioItem]) => Html)
         (implicit request: DataRequest[AnyContent], ec: ExecutionContext, messages: Messages): Future[Result] = {
    val jsonValue: JsObject = json(form)

    val model = CommonViewModelTwirl(
      entityType = (jsonValue \ "viewmodel" \ "entityType").asOpt[String].getOrElse(""),
      entityName = (jsonValue \ "viewmodel" \ "entityName").asOpt[String].getOrElse(""),
      submitUrl = onSubmitCall,
      enterManuallyUrl = Some(manualUrl))
    Future.successful(Ok(twirlView(model, twirlAddressRadios(jsonValue))))
  }

  def post(mode: Mode,
           json: Form[Int] => JsObject,
           pages: AddressPages,
           manualUrlCall:Call,
           onSubmitCall: Call,
           twirlView: (CommonViewModelTwirl, Seq[RadioItem], Form[Int]) => Html)
          (implicit request: DataRequest[AnyContent], ec: ExecutionContext, hc: HeaderCarrier, messages: Messages): Future[Result] = {
    form.bindFromRequest().fold(
      formWithErrors => {
        val jsonObject: JsObject = json(formWithErrors)

        val model = CommonViewModelTwirl(
          entityType = (jsonObject \ "viewmodel" \ "entityType").asOpt[String].getOrElse(""),
          entityName = (jsonObject \ "viewmodel" \ "entityName").asOpt[String].getOrElse(""),
          submitUrl = onSubmitCall,
          enterManuallyUrl = Some(manualUrlCall.url))

        val radios = twirlAddressRadios(jsonObject)
        Future.successful(BadRequest(twirlView(model, radios, formWithErrors)))

      },
      value =>
        pages.postcodePage.retrieve.map { addresses =>
          val address = addresses(value).copy(countryOpt = Some("GB"))
          address.toAddress match {
            case Some(addr) =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(pages.addressPage,
                  addr))
                answersWithChangeFlag <- Future.fromTry(setChangeFlag(updatedAnswers, AddressChange))
                _ <- userAnswersCacheConnector.save(answersWithChangeFlag.data)
              } yield Redirect(navigator.nextPage(pages.addressListPage, mode, answersWithChangeFlag))
            case None =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(pages.addressListPage,
                  address)
                )
                _ <- userAnswersCacheConnector.save(updatedAnswers.data)
              } yield {
                Redirect(manualUrlCall)
              }
          }
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

  def twirlAddressRadios(jsonObject: JsObject): Seq[RadioItem] = {
    (jsonObject \ "addresses").asOpt[Seq[JsObject]] match{
      case Some(x) => x.map{json =>
        RadioItem(content = Text((json \ "text").asOpt[String].getOrElse("")), value = (json \ "value").asOpt[Int].map(_.toString))
      }
      case _ => Seq.empty
    }
  }

}

case class AddressPages(postcodePage: QuestionPage[Seq[TolerantAddress]],
                        addressListPage: QuestionPage[TolerantAddress],
                        addressPage: QuestionPage[Address])
