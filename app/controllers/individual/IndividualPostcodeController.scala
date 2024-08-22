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

package controllers.individual

import connectors.AddressLookupConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import controllers.address.PostcodeController
import forms.FormsHelper.formWithError
import forms.address.PostcodeFormProvider
import models.{Mode, TolerantAddress}
import models.requests.DataRequest
import navigators.CompoundNavigator
import pages.QuestionPage
import pages.individual.IndividualPostcodePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import renderer.Renderer
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.TwirlMigration
import utils.annotations.AuthWithIV
import views.html.individual.PostcodeView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IndividualPostcodeController @Inject()(override val messagesApi: MessagesApi,
                                             val userAnswersCacheConnector: UserAnswersCacheConnector,
                                             val navigator: CompoundNavigator,
                                             @AuthWithIV
                                             authenticate: AuthAction,
                                             getData: DataRetrievalAction,
                                             requireData: DataRequiredAction,
                                             formProvider: PostcodeFormProvider,
                                             val addressLookupConnector: AddressLookupConnector,
                                             val controllerComponents: MessagesControllerComponents,
                                             val renderer: Renderer,
                                             postcodeView: PostcodeView
                                            )(implicit ec: ExecutionContext) extends PostcodeController
  with Retrievals with I18nSupport with NunjucksSupport {

  def form(implicit messages: Messages): Form[String] =
    formProvider(
      messages("individual.postcode.error.required"),
      messages("individual.postcode.error.invalid")
    )

  override def viewTemplate: String = "individual/postcode.njk"

  def onPageLoad(mode: Mode): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      get(getFormToJson(mode), mode)
  }

  private def get(json: Form[String] => JsObject, mode: Mode)
         (implicit request: DataRequest[AnyContent], ec: ExecutionContext): Future[Result] = {
    val template = TwirlMigration.duoTemplate(
      renderer.render(viewTemplate, json(form)),
      postcodeView(
        routes.IndividualPostcodeController.onSubmit(mode),
        routes.IndividualContactAddressController.onPageLoad(mode).url,
        form
      )
    )
    template.map(Ok(_))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      doSubmit(mode, getFormToJson(mode), IndividualPostcodePage, "error.postcode.noResults")
  }

  private def doSubmit(mode: Mode, formToJson: Form[String] => JsObject, postcodePage: QuestionPage[Seq[TolerantAddress]], errorMessage: String)
          (implicit request: DataRequest[AnyContent], ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = {
    form.bindFromRequest().fold(
      formWithErrors =>
        {
        val template = TwirlMigration.duoTemplate(
          renderer.render(viewTemplate, formToJson(formWithErrors)),
          postcodeView(
            routes.IndividualPostcodeController.onSubmit(mode),
            routes.IndividualContactAddressController.onPageLoad(mode).url,
            formWithErrors
          )
        )
        template.map(BadRequest(_))
        },
      value =>
        addressLookupConnector.addressLookupByPostCode(value).flatMap {
          case Nil =>
            val json = formToJson(formWithError(form, errorMessage))
            val template = TwirlMigration.duoTemplate(
              renderer.render(viewTemplate, json),
              postcodeView(
                routes.IndividualPostcodeController.onSubmit(mode),
                routes.IndividualContactAddressController.onPageLoad(mode).url,
                formWithError(form, errorMessage)
              )
            )
            template.map(BadRequest(_))

          case addresses =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(postcodePage, addresses))
              _ <- userAnswersCacheConnector.save(updatedAnswers.data)
            } yield Redirect(navigator.nextPage(postcodePage, mode, updatedAnswers))
        }

    )
  }

  def getFormToJson(mode: Mode)(implicit request: DataRequest[AnyContent]): Form[String] => JsObject = {
    form =>
      Json.obj(
        "form" -> form,
        "submitUrl" -> routes.IndividualPostcodeController.onSubmit(mode).url,
        "enterManuallyUrl" -> Some(routes.IndividualContactAddressController.onPageLoad(mode).url)
      )
  }
}
