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

package controllers.actions

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.IdentityVerificationConnector
import connectors.cache.UserAnswersCacheConnector
import models.UserAnswers
import models.requests.UserType.UserType
import models.requests.IdentifierRequest
import models.requests.PSPUser
import models.requests.UserType
import pages.AreYouInUKPage
import pages.JourneyPage
import pages.QuestionPage
import pages.RegisterAsBusinessPage
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core.AffinityGroup._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.UnauthorizedException
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class AuthenticatedIdentifierActionWithIV @Inject()(override val authConnector: AuthConnector,
                                                    config: FrontendAppConfig,
                                                    userAnswersCacheConnector: UserAnswersCacheConnector,
                                                    ivConnector: IdentityVerificationConnector,
                                                    val parser: BodyParsers.Default
                                                   )
                                                   (implicit val executionContext: ExecutionContext) extends IdentifierAction with AuthorisedFunctions {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authorised(User).retrieve(
      Retrievals.confidenceLevel and
        Retrievals.affinityGroup and
        Retrievals.allEnrolments and
        Retrievals.credentials
    ) {
      case cl ~ Some(affinityGroup) ~ enrolments ~ Some(credentials) =>
        val authRequest = IdentifierRequest(request, pspUser(cl, affinityGroup, None, enrolments, credentials.providerId))
        successRedirect(affinityGroup, cl, enrolments, authRequest, block)
      case _ =>
        Future.successful(Redirect(controllers.routes.UnauthorisedController.onPageLoad()))

    } recover handleFailure
  }


  protected def successRedirect[A](affinityGroup: AffinityGroup, cl: ConfidenceLevel,
                                   enrolments: Enrolments, authRequest: IdentifierRequest[A], block: IdentifierRequest[A] => Future[Result])
                                  (implicit hc: HeaderCarrier): Future[Result] = {

    getData(AreYouInUKPage).flatMap {
      case _ if alreadyEnrolledInPODS(enrolments) =>
        savePspIdAndReturnAuthRequest(enrolments, authRequest, block)
      case Some(true) if affinityGroup == Organisation =>
        doManualIVAndRetrieveNino(authRequest, enrolments, block)
      case _ =>
        block(authRequest)
    }
  }

  protected def savePspIdAndReturnAuthRequest[A](enrolments: Enrolments, authRequest: IdentifierRequest[A],
                                                 block: IdentifierRequest[A] => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    if (alreadyEnrolledInPODS(enrolments)) {
      val pspId = getPSPId(enrolments)
      block(IdentifierRequest(authRequest.request, authRequest.user.copy(alreadyEnrolledPspId = Some(pspId))))
    }
    else {
      block(authRequest)
    }
  }


  private def doManualIVAndRetrieveNino[A](authRequest: IdentifierRequest[A], enrolments: Enrolments,
                                           block: IdentifierRequest[A] => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    val journeyId = authRequest.request.getQueryString(key = "journeyId")
    getData(JourneyPage).flatMap {
      case Some(journey) =>
        getNinoAndUpdateAuthRequest(journey, enrolments, block, authRequest)
      case _ if journeyId.nonEmpty =>
        for {
          ua <- getUa
          uaWithJourneyId <- Future.fromTry(ua.set(JourneyPage, journeyId.getOrElse("")))
          _ <- userAnswersCacheConnector.save(uaWithJourneyId.data)
          finalAuthRequest <- getNinoAndUpdateAuthRequest(journeyId.getOrElse(""), enrolments, block, authRequest)
        } yield {
          finalAuthRequest
        }
      case _ =>
        orgManualIV(enrolments, authRequest, block)
    }
  }

  private def getNinoAndUpdateAuthRequest[A](journeyId: String, enrolments: Enrolments, block: IdentifierRequest[A] => Future[Result],
                                             authRequest: IdentifierRequest[A])(implicit hc: HeaderCarrier): Future[Result] = {
    ivConnector.retrieveNinoFromIV(journeyId).flatMap {
      case Some(nino) =>
        val updatedAuth = IdentifierRequest(authRequest.request, authRequest.user.copy(nino = Some(nino)))
        block(updatedAuth)
      case _ =>
        orgManualIV(enrolments, authRequest, block)
    }
  }

  private def orgManualIV[A](enrolments: Enrolments, authRequest: IdentifierRequest[A],
                             block: IdentifierRequest[A] => Future[Result])(implicit hc: HeaderCarrier) = {

    getData(RegisterAsBusinessPage).flatMap {
      case Some(false) =>
        ivConnector.startRegisterOrganisationAsIndividual(
          config.ukJourneyContinueUrl,
          failureURL = s"${config.loginContinueUrl}/unauthorised"
        ).map { link =>
          Redirect(url = s"${config.manualIvUrl}$link")
        }
      case _ =>
        block(authRequest)
    }
  }

  private def getData[A](typedId: QuestionPage[A])(implicit hc: HeaderCarrier, rds: Reads[A]): Future[Option[A]] = {
    getUa.map(_.get(typedId))
  }

  private def getUa(implicit hc: HeaderCarrier): Future[UserAnswers] = {
    userAnswersCacheConnector.fetch.map {
      case Some(json) =>
        UserAnswers(json.as[JsObject])
      case None =>
        UserAnswers(Json.obj())
    }
  }

  private def handleFailure: PartialFunction[Throwable, Result] = {
    case _: NoActiveSession =>
      Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
    case _: InsufficientEnrolments =>
      Redirect(controllers.routes.UnauthorisedController.onPageLoad())
    case _: InsufficientConfidenceLevel =>
      Redirect(controllers.routes.UnauthorisedController.onPageLoad())
    case _: UnsupportedAuthProvider =>
      Redirect(controllers.routes.UnauthorisedController.onPageLoad())
    case _: UnsupportedAffinityGroup =>
      Redirect(controllers.routes.UnauthorisedController.onPageLoad())
    case _: UnauthorizedException =>
      Redirect(controllers.routes.UnauthorisedController.onPageLoad())
  }

  private def existingPSP(enrolments: Enrolments): Option[String] =
    enrolments.getEnrolment("HMRC-PP-ORG").flatMap(_.getIdentifier("PSPID")).map(_.value)

  protected def alreadyEnrolledInPODS(enrolments: Enrolments): Boolean =
    enrolments.getEnrolment("HMRC-PODS-ORG").nonEmpty

  protected def userType(affinityGroup: AffinityGroup, cl: ConfidenceLevel): UserType = {
    affinityGroup match {
      case Individual =>
        UserType.Individual
      case Organisation =>
        UserType.Organisation
      case _ =>
        throw new UnauthorizedException("Unable to authorise the user")
    }
  }

  protected def pspUser(cl: ConfidenceLevel, affinityGroup: AffinityGroup,
                        nino: Option[String], enrolments: Enrolments, userId: String): PSPUser = {
    val psp = existingPSP(enrolments)
    PSPUser(userType(affinityGroup, cl), nino, psp.nonEmpty, psp, None, userId)
  }

  protected def getPSPId(enrolments: Enrolments): String =
    enrolments.getEnrolment("HMRC-PODS-ORG").flatMap(_.getIdentifier("PSPID")).map(_.value)
      .getOrElse(throw new RuntimeException("PSP ID missing"))
}

trait IdentifierAction extends ActionBuilder[IdentifierRequest, AnyContent] with ActionFunction[Request, IdentifierRequest]

class AuthenticatedIdentifierActionWithNoIV @Inject()(override val authConnector: AuthConnector,
                                                      config: FrontendAppConfig,
                                                      userAnswersCacheConnector: UserAnswersCacheConnector,
                                                      identityVerificationConnector: IdentityVerificationConnector,
                                                      parser: BodyParsers.Default
                                                     )(implicit executionContext: ExecutionContext) extends
  AuthenticatedIdentifierActionWithIV(authConnector, config, userAnswersCacheConnector, identityVerificationConnector, parser)

  with AuthorisedFunctions {

  override def successRedirect[A](affinityGroup: AffinityGroup, cl: ConfidenceLevel,
                                  enrolments: Enrolments, authRequest: IdentifierRequest[A],
                                  block: IdentifierRequest[A] => Future[Result])
                                 (implicit hc: HeaderCarrier): Future[Result] =
    savePspIdAndReturnAuthRequest(enrolments, authRequest, block)
}
