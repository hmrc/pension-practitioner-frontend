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

package handlers

import config.FrontendAppConfig
import play.api.http.HeaderNames.CACHE_CONTROL
import play.api.http.Status._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc.{Request, RequestHeader, Result}
import play.api.{Logger, PlayException}
import play.twirl.api.Html
import renderer.Renderer
import utils.TwirlMigration
import views.html.templates.ErrorTemplate
import views.html.{BadRequestView, InternalServerErrorView, NotFoundView}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

// NOTE: There should be changes to bootstrap to make this easier, the API in bootstrap should allow a `Future[Html]` rather than just an `Html`
@Singleton
class FrontendErrorHandler @Inject()(
                                      renderer: Renderer,
                                      val messagesApi: MessagesApi,
                                      config: FrontendAppConfig,
                                      badRequestView: BadRequestView,
                                      internalServerErrorView: InternalServerErrorView,
                                      errorTemplate: ErrorTemplate,
                                      notFoundView: NotFoundView,
                                      twirlMigration: TwirlMigration
                                    )(implicit ec: ExecutionContext) extends uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler with I18nSupport {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html = {
    errorTemplate(pageTitle, heading, Some(message))
  }

  override def onClientError(request: RequestHeader, statusCode: Int, message: String = ""): Future[Result] = {

    implicit def requestImplicit: Request[_] = Request(request, "")

    statusCode match {
      case BAD_REQUEST =>
        def template = twirlMigration.duoTemplate(
          renderer.render("badRequest.njk"),
          badRequestView()
        )
        template.map(BadRequest(_))
      case NOT_FOUND =>
        val json = Json.obj(
          "yourPensionSchemesUrl" -> config.pspListSchemesUrl
        )
        val template = twirlMigration.duoTemplate(
          renderer.render("notFound.njk", json),
          notFoundView(config.pspListSchemesUrl)
        )
        template.map(NotFound(_))
      case _ => super.onClientError(request, statusCode, message)
    }
  }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {

    implicit def requestImplicit: Request[_] = Request(request, "")

    logError(request, exception)
    exception match {
      case ApplicationException(result, _) =>
        Future.successful(result)
      case _ =>
        def template = twirlMigration.duoTemplate(
          renderer.render("internalServerError.njk"),
          internalServerErrorView()
        )
        template.map {
          content =>
            InternalServerError(content).withHeaders(CACHE_CONTROL -> "no-cache")
        }
    }
  }

  private val logger = Logger(classOf[FrontendErrorHandler])

  private def logError(request: RequestHeader, ex: Throwable): Unit =
    logger.error(
      """
        |
        |! %sInternal server error, for (%s) [%s] ->
        | """.stripMargin.format(ex match {
        case p: PlayException => "@" + p.id + " - "
        case _ => ""
      }, request.method, request.uri),
      ex
    )
}

case class ApplicationException(result: Result, message: String) extends Exception(message)

