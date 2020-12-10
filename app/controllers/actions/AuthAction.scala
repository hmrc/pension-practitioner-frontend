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
import models.WhatTypeBusiness.Yourselfasindividual
import models.requests.PSPUser
import models.requests.UserType
import models.requests.UserType.UserType
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core.AffinityGroup._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.UnauthorizedException
import uk.gov.hmrc.play.HeaderCarrierConverter
import models.requests.AuthenticatedRequest
import pages.JourneyPage
import pages.QuestionPage
import pages.WhatTypeBusinessPage
import pages.individual.AreYouUKResidentPage
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.libs.json.Reads

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class AuthenticatedAuthActionWithIV @Inject()(override val authConnector: AuthConnector,
  config: FrontendAppConfig,
  userAnswersCacheConnector: UserAnswersCacheConnector,
  ivConnector: IdentityVerificationConnector,
  val parser: BodyParsers.Default
)
  (implicit val executionContext: ExecutionContext) extends AuthAction with AuthorisedFunctions {

  protected def enrolmentsRedirect[A](authenticatedRequest: AuthenticatedRequest[A]): Option[Result] = None

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authorised(User or Assistant).retrieve(
      Retrievals.externalId and
        Retrievals.confidenceLevel and
        Retrievals.affinityGroup and
        Retrievals.allEnrolments and
        Retrievals.credentials and
        Retrievals.credentialRole
    ) {
      case Some(id) ~ cl ~ Some(affinityGroup) ~ enrolments ~ Some(credentials) ~ Some(credentialRole) =>
        allowAccess(id,
          affinityGroup,
          cl,
          enrolments,
          credentialRole,
          createAuthenticatedRequest(id, request, affinityGroup, credentials.providerId, enrolments),
          block
        )
      case _ =>
        Future.successful(Redirect(controllers.routes.UnauthorisedController.onPageLoad()))
    } recover handleFailure
  }

  protected def allowAccess[A](externalId: String, affinityGroup: AffinityGroup, cl: ConfidenceLevel,
    enrolments: Enrolments, role: CredentialRole, authRequest: => AuthenticatedRequest[A], block: AuthenticatedRequest[A] => Future[Result])
    (implicit hc: HeaderCarrier): Future[Result] = {

    (affinityGroup, role) match {
      case (AffinityGroup.Agent, _) => Future.successful(Redirect(controllers.routes.AgentCannotRegisterController.onPageLoad()))
      case (AffinityGroup.Individual, _) => Future.successful(Redirect(controllers.routes.NeedAnOrganisationAccountController.onPageLoad()))
      case (AffinityGroup.Organisation, Assistant) => Future.successful(Redirect(controllers.routes.AssistantNoAccessController.onPageLoad()))
      case (AffinityGroup.Organisation, _) =>
        enrolmentsRedirect(authRequest) match {
          case Some(redirect) => Future.successful(redirect)
          case _ =>
            getData(AreYouUKResidentPage).flatMap {
              case Some(true) =>
                doManualIVAndRetrieveNino(externalId, authRequest, block)
              case _ =>
                block(authRequest)
            }
        }
    }
  }

  private def doManualIVAndRetrieveNino[A](externalId: String, authRequest: AuthenticatedRequest[A],
    block: AuthenticatedRequest[A] => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    val journeyId = authRequest.request.getQueryString(key = "journeyId")
    getData(JourneyPage).flatMap {
      case Some(journey) => getNinoAndUpdateAuthRequest(externalId, journey, block, authRequest)
      case _ if journeyId.nonEmpty =>
        for {
          ua <- getUa
            uaWithJourneyId <- Future.fromTry(ua.set(JourneyPage, journeyId.getOrElse("")))
            _ <- userAnswersCacheConnector.save(uaWithJourneyId.data)
            finalAuthRequest <- getNinoAndUpdateAuthRequest(externalId, journeyId.getOrElse(""), block, authRequest)
        } yield {
          finalAuthRequest
        }
      case _ =>
        orgManualIV(authRequest, block)
    }
  }

  private def getNinoAndUpdateAuthRequest[A](externalId: String, journeyId: String, block: AuthenticatedRequest[A] => Future[Result],
    authRequest: AuthenticatedRequest[A])(implicit hc: HeaderCarrier): Future[Result] = {
    ivConnector.retrieveNinoFromIV(journeyId).flatMap {
      case Some(nino) =>
        val updatedAuth = AuthenticatedRequest(authRequest.request, externalId, authRequest.user.copy(nino = Some(nino)))
        block(updatedAuth)
      case _ =>
        getUa.flatMap { answers =>
          Future.fromTry(answers.remove(JourneyPage)).flatMap { ua =>
            userAnswersCacheConnector.save(ua.data).flatMap { _ =>
              orgManualIV(authRequest, block)
            }
          }
        }
    }
  }

  private def orgManualIV[A](authRequest: AuthenticatedRequest[A],
    block: AuthenticatedRequest[A] => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    getData(WhatTypeBusinessPage).flatMap {
      case Some(Yourselfasindividual) =>
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

  private def createAuthenticatedRequest[A](
    externalId: String,
    request: Request[A],
    affinityGroup: AffinityGroup,
    providerId: String,
    enrolments: Enrolments
  )(implicit hc: HeaderCarrier): AuthenticatedRequest[A] = {
    val psp = existingPSP(enrolments)
    val alreadyEnrolledPspId = enrolments.getEnrolment("HMRC-PODSPP-ORG")
      .flatMap(_.getIdentifier("PSPID").map(_.value))
    val pspUser = PSPUser(
      userType = userType(affinityGroup),
      nino = None,
      isExistingPSP = psp.nonEmpty,
      existingPSPId = psp,
      alreadyEnrolledPspId = alreadyEnrolledPspId,
      userId = providerId
    )
    AuthenticatedRequest(request, externalId, pspUser)
  }

  private def getData[A](typedId: QuestionPage[A])(implicit hc: HeaderCarrier, rds: Reads[A]): Future[Option[A]] =
    getUa.map(_.get(typedId))

  private def getUa(implicit hc: HeaderCarrier): Future[UserAnswers] =
    userAnswersCacheConnector.fetch.map {
      case Some(json) =>
        UserAnswers(json.as[JsObject])
      case None =>
        UserAnswers(Json.obj())
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
    enrolments.getEnrolment("HMRC-PODSPP-ORG").nonEmpty

  protected def userType(affinityGroup: AffinityGroup): UserType = {
    affinityGroup match {
      case Individual => UserType.Individual
      case Organisation => UserType.Organisation
      case _ =>
        throw new UnauthorizedException("Unable to authorise the user")
    }
  }
}

trait AuthAction extends ActionBuilder[AuthenticatedRequest, AnyContent] with ActionFunction[Request, AuthenticatedRequest]

class AuthenticatedAuthActionWithNoIV @Inject()(override val authConnector: AuthConnector,
  config: FrontendAppConfig,
  userAnswersCacheConnector: UserAnswersCacheConnector,
  identityVerificationConnector: IdentityVerificationConnector,
  parser: BodyParsers.Default
)(implicit executionContext: ExecutionContext) extends
  AuthenticatedAuthActionWithIV(authConnector, config, userAnswersCacheConnector, identityVerificationConnector, parser)

  with AuthorisedFunctions {
  override protected def enrolmentsRedirect[A](authenticatedRequest: AuthenticatedRequest[A]): Option[Result] = None
  override def allowAccess[A](externalId: String, affinityGroup: AffinityGroup, cl: ConfidenceLevel,
    enrolments: Enrolments, role: CredentialRole, authRequest: => AuthenticatedRequest[A],
    block: AuthenticatedRequest[A] => Future[Result])
    (implicit hc: HeaderCarrier): Future[Result] = block(authRequest)
}

class AuthenticatedAuthActionWithIVNoEnrolment @Inject()(override val authConnector: AuthConnector,
  config: FrontendAppConfig,
  userAnswersCacheConnector: UserAnswersCacheConnector,
  identityVerificationConnector: IdentityVerificationConnector,
  parser: BodyParsers.Default
)(implicit executionContext: ExecutionContext) extends
  AuthenticatedAuthActionWithIV(authConnector, config, userAnswersCacheConnector, identityVerificationConnector, parser)

  with AuthorisedFunctions {
  override protected def enrolmentsRedirect[A](authenticatedRequest: AuthenticatedRequest[A]): Option[Result] = {
    authenticatedRequest.user.alreadyEnrolledPspId.map(_ => Redirect(controllers.routes.AlreadyRegisteredController.onPageLoad()))
  }
}

class AuthenticatedAuthActionWithIVEnrolmentRequired @Inject()(override val authConnector: AuthConnector,
  config: FrontendAppConfig,
  userAnswersCacheConnector: UserAnswersCacheConnector,
  identityVerificationConnector: IdentityVerificationConnector,
  parser: BodyParsers.Default
)(implicit executionContext: ExecutionContext) extends
  AuthenticatedAuthActionWithIV(authConnector, config, userAnswersCacheConnector, identityVerificationConnector, parser)
  with AuthorisedFunctions {
  override protected def enrolmentsRedirect[A](authenticatedRequest: AuthenticatedRequest[A]): Option[Result] = {
    authenticatedRequest.user.alreadyEnrolledPspId match {
      case Some(_) => None
      case _ => Some(Redirect(config.youNeedToRegisterUrl))
    }
  }
}
