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

import base.SpecBase
import connectors.IdentityVerificationConnector
import connectors.cache.UserAnswersCacheConnector
import models.WhatTypeBusiness.{Companyorpartnership, Yourselfasindividual}
import org.mockito.Matchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.Results.Ok
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.http.UnauthorizedException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthActionSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  import AuthActionSpec._

  override def beforeEach: Unit = {
    Mockito.reset(mockUserAnswersCacheConnector, authConnector, mockIVConnector)
  }

  "Auth Action" when {

    "called for already enrolled User" must {
      "return OK" when {
        "coming from any page" in {
          when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(authRetrievals(enrolments = enrolmentPODS))
          when(mockUserAnswersCacheConnector.fetch(any(), any())).thenReturn(Future(None))
          val result = controllerWithIVEnrolment.onPageLoad()(fakeRequest)
          status(result) mustBe OK
        }
      }
    }

    "called for agent" must {
      "redirect" in {
        when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(authRetrievals(affinityGroup = Some(AffinityGroup.Agent)))
        val userAnswersData = Json.obj("areYouUKResident" -> true)
        when(mockUserAnswersCacheConnector.fetch(any(), any())).thenReturn(Future(Some(userAnswersData)))
        val result = controllerWithIVEnrolment.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.AgentCannotRegisterController.onPageLoad().url)
      }
    }

    "called for individual" must {
      "redirect" in {
        when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(authRetrievals(affinityGroup = Some(AffinityGroup.Individual)))
        val userAnswersData = Json.obj("areYouUKResident" -> true)
        when(mockUserAnswersCacheConnector.fetch(any(), any())).thenReturn(Future(Some(userAnswersData)))
        val result = controllerWithIVEnrolment.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.NeedAnOrganisationAccountController.onPageLoad().url)
      }
    }

    "called for Organisation that is an assistant" must {
      "redirect" in {
        when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any()))
          .thenReturn(authRetrievals(affinityGroup = Some(AffinityGroup.Organisation), role = Some(Assistant)))
        val userAnswersData = Json.obj("areYouUKResident" -> true)
        when(mockUserAnswersCacheConnector.fetch(any(), any())).thenReturn(Future(Some(userAnswersData)))
        val result = controllerWithIVEnrolment.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.AssistantNoAccessController.onPageLoad().url)
      }
    }

    "called for Organisation user that is not an assistant" must {
      "redirect to Manual IV " when {
        "they want to register as Individual" in {
          val userAnswersData = Json.obj("areYouUKResident" -> true, "whatTypeBusiness" -> Yourselfasindividual.toString)
          when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(authRetrievals())
          when(mockIVConnector.startRegisterOrganisationAsIndividual(any(), any())(any(), any())).thenReturn(Future(startIVLink))
          when(mockUserAnswersCacheConnector.fetch(any(), any())).thenReturn(Future(Some(userAnswersData)))
          val result = controllerWithIVEnrolment.onPageLoad()(fakeRequest)
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(frontendAppConfig.identityVerificationFrontend + startIVLink)
        }

        "journey Id is correct and in the cache but no nino returned from IV" in {
          when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(authRetrievals())
          when(mockIVConnector.retrieveNinoFromIV(any())(any(), any())).thenReturn(Future(None))
          when(mockIVConnector.startRegisterOrganisationAsIndividual(any(), any())(any(), any())).thenReturn(Future(startIVLink))
          val userAnswersData = Json.obj("areYouUKResident" -> true,
            "whatTypeBusiness" -> Yourselfasindividual.toString, "journeyId" -> "test-journey")
          when(mockUserAnswersCacheConnector.fetch(any(), any())).thenReturn(Future(Some(userAnswersData)))
          when(mockUserAnswersCacheConnector.save(any())(any(), any())).thenReturn(Future(userAnswersData))
          val result = controllerWithIVEnrolment.onPageLoad()(fakeRequest)
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(frontendAppConfig.identityVerificationFrontend + startIVLink)
        }

        "journey Id is not present in url and not in the cache" in {
          when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(authRetrievals())
          when(mockIVConnector.retrieveNinoFromIV(any())(any(), any())).thenReturn(Future(None))
          when(mockIVConnector.startRegisterOrganisationAsIndividual(any(), any())(any(), any())).thenReturn(Future(startIVLink))
          val userAnswersData = Json.obj("areYouUKResident" -> true, "whatTypeBusiness" -> Yourselfasindividual.toString)
          when(mockUserAnswersCacheConnector.fetch(any(), any())).thenReturn(Future(Some(userAnswersData)))
          val result = controllerWithIVEnrolment.onPageLoad()(fakeRequest)
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(frontendAppConfig.identityVerificationFrontend + startIVLink)
        }
      }

      "return OK, retrieve the nino from IV when selected as Individual" when {

        "journey Id is saved in user answers" in {
          when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(authRetrievals())
          when(mockIVConnector.retrieveNinoFromIV(any())(any(), any())).thenReturn(Future(Some(nino)))
          val userAnswersData = Json.obj("areYouUKResident" -> true,
            "whatTypeBusiness" -> Yourselfasindividual.toString, "journeyId" -> "test-journey")
          when(mockUserAnswersCacheConnector.fetch(any(), any())).thenReturn(Future(Some(userAnswersData)))

          val result = controllerWithIVEnrolment.onPageLoad()(fakeRequest)
          status(result) mustBe OK
        }

        "journey Id is not in user answers but present in url" in {
          when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(authRetrievals())
          when(mockIVConnector.retrieveNinoFromIV(any())(any(), any())).thenReturn(Future(Some(nino)))
          when(mockIVConnector.startRegisterOrganisationAsIndividual(any(), any())(any(), any())).thenReturn(Future(startIVLink))
          val journeyId = "test-journey-id"
          val userAnswersData = Json.obj("areYouUKResident" -> true, "whatTypeBusiness" -> Yourselfasindividual.toString)
          when(mockUserAnswersCacheConnector.save(any())(any(), any())).thenReturn(Future(Json.obj()))
          when(mockUserAnswersCacheConnector.fetch(any(), any())).thenReturn(Future(Some(userAnswersData)))
          val result = controllerWithIVEnrolment.onPageLoad()(FakeRequest("", s"/url?journeyId=$journeyId"))
          status(result) mustBe OK
          verify(mockUserAnswersCacheConnector, times(1)).save(any())(any(), any())
        }
      }

      "return OK" when {
        "the user is non uk user" in {
          when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(authRetrievals())
          val userAnswersData = Json.obj("areYouUKResident" -> false)
          when(mockUserAnswersCacheConnector.fetch(any(), any())).thenReturn(Future(Some(userAnswersData)))
          val result = controllerWithIVEnrolment.onPageLoad()(fakeRequest)
          status(result) mustBe OK
        }

        "user is in UK and wants to register as Organisation" in {
          when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(authRetrievals())
          val userAnswersData = Json.obj("areYouUKResident" -> true, "whatTypeBusiness" -> Companyorpartnership.toString)
          when(mockUserAnswersCacheConnector.fetch(any(), any())).thenReturn(Future(Some(userAnswersData)))

          val result = controllerWithIVEnrolment.onPageLoad()(fakeRequest)
          status(result) mustBe OK
        }
      }
    }

    "the user hasn't logged in" must {
      "redirect the user to log in " in {
        when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(Future.failed(new MissingBearerToken))
        val result = controllerWithIVEnrolment.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must startWith(frontendAppConfig.loginUrl)
      }
    }

    "the user's session has expired" must {
      "redirect the user to log in " in {
        when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(Future.failed(new BearerTokenExpired))
        val result = controllerWithIVEnrolment.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must startWith(frontendAppConfig.loginUrl)
      }
    }

    "redirect the user to the unauthorised page" when {
      "the user doesn't have sufficient enrolments" in {
        when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(Future.failed(new InsufficientEnrolments))
        val result = controllerWithIVEnrolment.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad().url)
      }

      "the user doesn't have sufficient confidence level" in {
        when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(Future.failed(new InsufficientConfidenceLevel))
        val result = controllerWithIVEnrolment.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad().url)
      }

      "the user used an unaccepted auth provider" in {
        when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(Future.failed(new UnsupportedAuthProvider))
        val result = controllerWithIVEnrolment.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad().url)
      }

      "the user has an unsupported affinity group" in {
        when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(Future.failed(new UnsupportedAffinityGroup))
        val result = controllerWithIVEnrolment.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad().url)
      }

      "there is no affinity group" in {
        when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(authRetrievals(affinityGroup = None))

        val result = controllerWithIVEnrolment.onPageLoad()(FakeRequest("GET", "/foo"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad().url)
      }

      "the user is not an authorised user" in {
        when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(Future.failed(new UnauthorizedException("Unknown User")))
        val result = controllerWithIVEnrolment.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad().url)
      }
    }
  }

  "AuthenticatedAuthActionWithNoIV" when {
    "called for Company user" must {
      "return OK and able to view the page and not redirect to IV" when {
        "they want to register as Individual" in {
          when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(authRetrievals())
          val userAnswersData = Json.obj("areYouInUK" -> true, "registerAsBusiness" -> false)
          when(mockUserAnswersCacheConnector.fetch(any(), any())).thenReturn(Future(Some(userAnswersData)))
          val authAction = new AuthenticatedAuthActionWithNoIV(authConnector, frontendAppConfig,
            mockUserAnswersCacheConnector, mockIVConnector, bodyParsers)

          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(fakeRequest)
          status(result) mustBe OK
        }
      }
    }
  }
}

object AuthActionSpec extends SpecBase with MockitoSugar {
  private val pspId = "00000000"
  private val nino = uk.gov.hmrc.domain.Nino("AB100100A")
  type authRetrievalsType = Option[String] ~ ConfidenceLevel ~ Option[AffinityGroup] ~ Enrolments ~ Option[Credentials] ~Option[CredentialRole]

  private val enrolmentPODS = Enrolments(Set(Enrolment("HMRC-PODSPP-ORG", Seq(EnrolmentIdentifier("PSPID", pspId)), "")))
  private val startIVLink = "/start-iv-link"

  private def authRetrievals(confidenceLevel: ConfidenceLevel = ConfidenceLevel.L50,
                             affinityGroup: Option[AffinityGroup] = Some(AffinityGroup.Organisation),
                             enrolments: Enrolments = Enrolments(Set()),
                             creds: Option[Credentials] = Option(Credentials(providerId = "test provider", providerType = "")),
                            role: Option[CredentialRole] = Option(User)
                            ): Future[authRetrievalsType] = Future.successful(
    new ~(new ~(new ~(new ~(new ~(Some("id"), confidenceLevel),
      affinityGroup),
      enrolments),
      creds),
      role
    )
  )

  class Harness(identifierAction: AuthAction) {
    def onPageLoad(): Action[AnyContent] = identifierAction {
      implicit request =>
        Ok(Json.obj("userId" -> request.user.userId))
    }
  }
  private val mockUserAnswersCacheConnector: UserAnswersCacheConnector = mock[UserAnswersCacheConnector]
  private val mockIVConnector: IdentityVerificationConnector = mock[IdentityVerificationConnector]
  private val authConnector: AuthConnector = mock[AuthConnector]
  private val bodyParsers: BodyParsers.Default = app.injector.instanceOf[BodyParsers.Default]

  val authActionWithIVEnrolment = new AuthenticatedAuthActionWithIVEnrolment(
    authConnector, frontendAppConfig,
    mockUserAnswersCacheConnector, mockIVConnector, bodyParsers
  )

  val authActionWithIVNoEnrolment = new AuthenticatedAuthActionWithIVNoEnrolment(
    authConnector, frontendAppConfig,
    mockUserAnswersCacheConnector, mockIVConnector, bodyParsers
  )

  val controllerWithIVEnrolment = new Harness(authActionWithIVEnrolment)
  val controllerWithIVNoEnrolment = new Harness(authActionWithIVNoEnrolment)
}
