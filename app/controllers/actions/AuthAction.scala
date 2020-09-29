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

import com.google.inject.ImplementedBy
import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.routes
import models.requests.PSPUser
import models.requests.UserType
import models.requests.UserType.UserType
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core.AffinityGroup._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.domain
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.UnauthorizedException
import uk.gov.hmrc.play.HeaderCarrierConverter
import models.requests.AuthenticatedRequest

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class FullAuthentication @Inject()(override val authConnector: AuthConnector,
                                   config: FrontendAppConfig,
                                   val parser: BodyParsers.Default)
                                  (implicit val executionContext: ExecutionContext) extends AuthAction with AuthorisedFunctions {

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authorised(User).retrieve(
      Retrievals.externalId and
        Retrievals.confidenceLevel and
        Retrievals.affinityGroup and
        Retrievals.allEnrolments and
        Retrievals.credentials
    ) {
      case Some(id) ~ cl ~ Some(affinityGroup) ~ enrolments ~ Some(credentials) =>
        redirectToInterceptPages(enrolments, affinityGroup).fold {
          val authRequest = AuthenticatedRequest(request, id, pspUser(affinityGroup, None, enrolments, credentials.providerId))
          block(authRequest)
        } { result => Future.successful(result) }
      case _ =>
        Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))

    } recover handleFailure
  }


  protected def savePspIdAndReturnAuthRequest[A](enrolments: Enrolments,
                                                 authRequest: AuthenticatedRequest[A],
                                                 block: AuthenticatedRequest[A] => Future[Result])
                                                (implicit hc: HeaderCarrier): Future[Result] = {
    if (alreadyEnrolledInPODS(enrolments)) {
      val pspId = getPSAId(enrolments)
      block(AuthenticatedRequest(authRequest.request, authRequest.externalId, authRequest.user.copy(
        alreadyEnrolledPspId = Some(pspId))))
    }
    else {
      block(authRequest)
    }
  }

  private def redirectToInterceptPages[A](enrolments: Enrolments, affinityGroup: AffinityGroup): Option[Result] = {
    if (isPSP(enrolments) && !isPSA(enrolments)) {
      Some(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
    } else {
      affinityGroup match {
        case Agent =>
          Some(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
        case Individual if !alreadyEnrolledInPODS(enrolments) =>
          Some(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
        case _ =>
          None
      }
    }
  }

  private def handleFailure: PartialFunction[Throwable, Result] = {
    case _: NoActiveSession =>
      Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
    case _: InsufficientEnrolments =>
      Redirect(routes.UnauthorisedController.onPageLoad())
    case _: InsufficientConfidenceLevel =>
      Redirect(routes.UnauthorisedController.onPageLoad())
    case _: UnsupportedAuthProvider =>
      Redirect(routes.UnauthorisedController.onPageLoad())
    case _: UnsupportedAffinityGroup =>
      Redirect(routes.UnauthorisedController.onPageLoad())
    case _: UnsupportedCredentialRole =>
      Redirect(controllers.routes.SessionExpiredController.onPageLoad())
      //Redirect(routes.UnauthorisedAssistantController.onPageLoad())
    case _: UnauthorizedException =>
      Redirect(routes.UnauthorisedController.onPageLoad())
  }

  private def existingPSA(enrolments: Enrolments): Option[String] =
    enrolments.getEnrolment("HMRC-PSA-ORG").flatMap(_.getIdentifier("PSAID")).map(_.value)

  private def isPSP(enrolments: Enrolments): Boolean =
    enrolments.getEnrolment(key = "HMRC-PP-ORG").nonEmpty

  private def isPSA(enrolments: Enrolments): Boolean =
    enrolments.getEnrolment(key = "HMRC-PSA-ORG").nonEmpty

  protected def alreadyEnrolledInPODS(enrolments: Enrolments): Boolean =
    enrolments.getEnrolment("HMRC-PODS-ORG").nonEmpty

  protected def userType(affinityGroup: AffinityGroup): UserType = {
    affinityGroup match {
      case Individual =>
        UserType.Individual
      case Organisation =>
        UserType.Organisation
      case _ =>
        throw new UnauthorizedException("Unable to authorise the user")
    }
  }

  protected def pspUser(affinityGroup: AffinityGroup,
                        nino: Option[domain.Nino],
                        enrolments: Enrolments,
                        userId: String): PSPUser = {
    val psp = existingPSA(enrolments)
    PSPUser(userType(affinityGroup), nino, psp.nonEmpty, psp, None, userId)
  }

  protected def getPSAId(enrolments: Enrolments): String =
    enrolments.getEnrolment("HMRC-PODS-ORG").flatMap(_.getIdentifier("PSAID")).map(_.value)
      .getOrElse(throw new RuntimeException("PSA ID missing"))
}

@ImplementedBy(classOf[FullAuthentication])
trait AuthAction extends ActionBuilder[AuthenticatedRequest, AnyContent] with ActionFunction[Request, AuthenticatedRequest]
