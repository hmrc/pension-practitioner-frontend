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

package controllers.actions

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.{MinimalConnector, SessionDataCacheConnector}
import models.AdministratorOrPractitioner.Administrator
import models.UserAnswers
import models.requests.UserType.UserType
import models.requests.{AuthenticatedRequest, PSPUser, UserType}
import pages._
import play.api.Logger
import play.api.libs.json.JsObject
import play.api.mvc.Results.{Redirect, SeeOther}
import play.api.mvc._
import uk.gov.hmrc.auth.core.AffinityGroup._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

protected class AuthenticatedAuthAction(
                                                  override val authConnector: AuthConnector,
                                                  config: FrontendAppConfig,
                                                  minimalConnector: MinimalConnector,
                                                  val parser: BodyParsers.Default,
                                                  sessionDataCacheConnector: SessionDataCacheConnector,
                                                  minimalConfidenceLevel: Option[ConfidenceLevel] = None
                                                )(
                                                  implicit val executionContext: ExecutionContext
                                                ) extends AuthAction with AuthorisedFunctions {

  private val logger = Logger(classOf[AuthenticatedAuthAction])

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val auth = minimalConfidenceLevel.map(minimalConfidenceLevel =>
      authorised((User or Assistant) and minimalConfidenceLevel)
    ).getOrElse(authorised(User or Assistant))

    auth.retrieve(
      Retrievals.externalId and
        Retrievals.affinityGroup and
        Retrievals.allEnrolments and
        Retrievals.credentials and
        Retrievals.credentialRole and
        Retrievals.groupIdentifier and
        Retrievals.nino
    ) {
      case Some(id) ~
        Some(affinityGroup) ~
        enrolments ~
        Some(credentials) ~
        Some(credentialRole) ~
        Some(groupIdentifier) ~
        nino =>
        logger.debug(s"Logging auth details- externalId: $id, affinityGroup: ${affinityGroup.toJson}, " +
          s"enrolments: ${enrolments.enrolments}, credentials: ${credentials.providerType}=>${credentials.providerId}, " +
          s"credentialsRole: ${credentialRole.toJson} & request: $request")

        checkForBothEnrolments(id, request, enrolments).flatMap {
          case None =>
            allowAccess(id,
              affinityGroup,
              credentialRole,
              enrolments,
              createAuthenticatedRequest(id, request, affinityGroup, credentials.providerId, enrolments, groupIdentifier, nino),
              block
            )
          case Some(r) => Future.successful(r)
        }

      case _ =>
        Future.successful(Redirect(controllers.routes.UnauthorisedController.onPageLoad()))
    } recover handleFailure(request)
  }

  protected def allowAccess[A](
                                externalId: String,
                                affinityGroup: AffinityGroup,
                                role: CredentialRole,
                                enrolments: Enrolments,
                                authRequest: => AuthenticatedRequest[A], block: AuthenticatedRequest[A] => Future[Result])
                              (implicit hc: HeaderCarrier): Future[Result] = {
    (affinityGroup, role) match {
      case (AffinityGroup.Agent, _) =>
        Future.successful(Redirect(controllers.routes.AgentCannotRegisterController.onPageLoad()))
      case (AffinityGroup.Individual, _) =>
        Future.successful(Redirect(controllers.routes.NeedAnOrganisationAccountController.onPageLoad()))
      case (AffinityGroup.Organisation, Assistant) if enrolments.getEnrolment("HMRC-PODSPP-ORG").isEmpty =>
        Future.successful(Redirect(controllers.routes.AssistantNoAccessController.onPageLoad()))
      case (AffinityGroup.Organisation, _) =>
        (checkAuthenticatedRequest(authRequest), authRequest.user.alreadyEnrolledPspId) match {
          case (Some(redirect), _) =>
            Future.successful(redirect)
          case (_, None) =>
            block(authRequest)
          case _ =>
            completeAfterDeceasedFlagCheck(authRequest, block)
        }
      case _ =>
        Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
    }
  }

  private def completeAfterDeceasedFlagCheck[A](authRequest: => AuthenticatedRequest[A],
                                                block: AuthenticatedRequest[A] => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    authRequest.user.alreadyEnrolledPspId match {
      case None => block(authRequest)
      case Some(pspId) =>
        minimalConnector.getMinimalPspDetails(pspId).flatMap { minimalDetails =>
          if (minimalDetails.deceasedFlag) {
            Future.successful(Redirect(config.youMustContactHMRCUrl))
          } else {
            block(authRequest)
          }
        }
    }
  }

  private def checkForBothEnrolments[A](id: String, request: Request[A], enrolments: Enrolments): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    (enrolments.getEnrolment("HMRC-PODS-ORG"), enrolments.getEnrolment("HMRC-PODSPP-ORG")) match {
      case (Some(_), Some(_)) =>
        sessionDataCacheConnector.fetch(id).flatMap { optionJsValue =>
          optionJsValue.map(a => UserAnswers(a.as[JsObject])).flatMap(_.get(AdministratorOrPractitionerPage)) match {
            case None => Future.successful(Some(Redirect(config.administratorOrPractitionerUrl)))
            case Some(Administrator) =>
              Future.successful(Some(Redirect(Call("GET",
                config.cannotAccessPageAsAdministratorUrl(config.localFriendlyUrl(request.uri))))))
            case _ => Future.successful(None)
          }
        }
      case _ => Future.successful(None)
    }
  }

  private def createAuthenticatedRequest[A](
                                             externalId: String,
                                             request: Request[A],
                                             affinityGroup: AffinityGroup,
                                             providerId: String,
                                             enrolments: Enrolments,
                                             groupIdentifier: String,
                                             nino: Option[String]
                                           ): AuthenticatedRequest[A] = {
    val tpssPspId = enrolments.getEnrolment("HMRC-PP-ORG")
      .flatMap(_.getIdentifier("PPID")).map(_.value)
    val podsPspId = enrolments.getEnrolment("HMRC-PODSPP-ORG")
      .flatMap(_.getIdentifier("PSPID").map(_.value))
    val pspUser = PSPUser(
      userType = userType(affinityGroup),
      nino = nino.map(uk.gov.hmrc.domain.Nino(_)),
      isExistingPSP = tpssPspId.nonEmpty,
      existingPSPId = tpssPspId,
      alreadyEnrolledPspId = podsPspId,
      userId = providerId,
      groupIdentifier = groupIdentifier
    )
    AuthenticatedRequest(request, externalId, pspUser)
  }

  protected def userType(affinityGroup: AffinityGroup): UserType = {
    affinityGroup match {
      case Individual => UserType.Individual
      case Organisation => UserType.Organisation
      case _ =>
        throw new UnauthorizedException("Unable to authorise the user")
    }
  }

  private def handleFailure(request: RequestHeader): PartialFunction[Throwable, Result] = {
    case _: NoActiveSession =>
      Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
    case _: InsufficientEnrolments =>
      Redirect(controllers.routes.UnauthorisedController.onPageLoad())
    case _: InsufficientConfidenceLevel =>
      val completionURL = RedirectUrl(request.uri)
      val failureURL = RedirectUrl(controllers.routes.UnauthorisedController.onPageLoad().url)
      val url = config.identityValidationFrontEndEntry(completionURL, failureURL)
      SeeOther(url)
    case _: UnsupportedAuthProvider =>
      Redirect(controllers.routes.UnauthorisedController.onPageLoad())
    case _: UnsupportedAffinityGroup =>
      Redirect(controllers.routes.UnauthorisedController.onPageLoad())
    case _: UnauthorizedException =>
      Redirect(controllers.routes.UnauthorisedController.onPageLoad())
  }

  protected def checkAuthenticatedRequest[A](authenticatedRequest: AuthenticatedRequest[A]): Option[Result] = None
}

trait AuthAction extends ActionBuilder[AuthenticatedRequest, AnyContent] with ActionFunction[Request, AuthenticatedRequest]

class AuthenticatedAuthActionWithIV @Inject()(override val authConnector: AuthConnector,
                                              config: FrontendAppConfig,
                                              minimalConnector: MinimalConnector,
                                              parser: BodyParsers.Default,
                                              sessionDataCacheConnector: SessionDataCacheConnector
                                             )(implicit executionContext: ExecutionContext) extends
  AuthenticatedAuthAction(authConnector, config, minimalConnector, parser, sessionDataCacheConnector, Some(ConfidenceLevel.L250))
  with AuthorisedFunctions

class AuthenticatedAuthActionWithNoIV @Inject()(override val authConnector: AuthConnector,
                                              config: FrontendAppConfig,
                                              minimalConnector: MinimalConnector,
                                              parser: BodyParsers.Default,
                                              sessionDataCacheConnector: SessionDataCacheConnector
                                             )(implicit executionContext: ExecutionContext) extends
  AuthenticatedAuthAction(authConnector, config, minimalConnector, parser, sessionDataCacheConnector, None)
  with AuthorisedFunctions

class AuthenticatedAuthActionMustHaveNoEnrolmentWithIV @Inject()(override val authConnector: AuthConnector,
                                                                 config: FrontendAppConfig,
                                                                 minimalConnector: MinimalConnector,
                                                                 parser: BodyParsers.Default,
                                                                 sessionDataCacheConnector: SessionDataCacheConnector,
                                                                 appConfig: FrontendAppConfig
                                                                )(implicit executionContext: ExecutionContext) extends
  AuthenticatedAuthActionWithIV(authConnector, config, minimalConnector, parser, sessionDataCacheConnector)
  with AuthorisedFunctions {

  override protected def checkAuthenticatedRequest[A](authenticatedRequest: AuthenticatedRequest[A]): Option[Result] = {
    authenticatedRequest.user.alreadyEnrolledPspId.map(_ => Redirect(appConfig.returnToPspDashboardUrl))
  }
}

class AuthenticatedAuthActionMustHaveEnrolmentWithNoIV @Inject()(override val authConnector: AuthConnector,
                                                                 config: FrontendAppConfig,
                                                                 minimalConnector: MinimalConnector,
                                                                 parser: BodyParsers.Default,
                                                                 sessionDataCacheConnector: SessionDataCacheConnector,
                                                                 appConfig: FrontendAppConfig
                                                        )(implicit executionContext: ExecutionContext) extends
  AuthenticatedAuthAction(authConnector, config, minimalConnector, parser, sessionDataCacheConnector)
  with AuthorisedFunctions {

  override protected def checkAuthenticatedRequest[A](authenticatedRequest: AuthenticatedRequest[A]): Option[Result] = {
    authenticatedRequest.user.alreadyEnrolledPspId match {
      case Some(_) => None
      case _ => Some(Redirect(appConfig.returnToPspDashboardUrl))
    }
  }
}

class AuthenticatedAuthActionMustHaveNoEnrolmentWithNoIV @Inject()(override val authConnector: AuthConnector,
                                                                   config: FrontendAppConfig,
                                                                   minimalConnector: MinimalConnector,
                                                                   parser: BodyParsers.Default,
                                                                   sessionDataCacheConnector: SessionDataCacheConnector,
                                                                   appConfig: FrontendAppConfig
                                                                  )(implicit executionContext: ExecutionContext) extends
  AuthenticatedAuthAction(authConnector, config, minimalConnector, parser, sessionDataCacheConnector)
  with AuthorisedFunctions {

  override protected def checkAuthenticatedRequest[A](authenticatedRequest: AuthenticatedRequest[A]): Option[Result] =
    authenticatedRequest.user.alreadyEnrolledPspId.map(_ => Redirect(appConfig.returnToPspDashboardUrl))
}
