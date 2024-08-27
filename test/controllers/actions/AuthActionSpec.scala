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

import base.SpecBase
import connectors.cache.UserAnswersCacheConnector
import connectors.{MinimalConnector, SessionDataCacheConnector}
import models.WhatTypeBusiness.Companyorpartnership
import models.{AdministratorOrPractitioner, MinimalPSP, UserAnswers}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.AdministratorOrPractitionerPage
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

  override def beforeEach(): Unit = {
    reset(mockUserAnswersCacheConnector)
    reset(authConnector)
    reset(mockMinimalConnector)
    when(mockMinimalConnector.getMinimalPspDetails(any())(any(), any())).thenReturn(Future(minimalPspDeceased()))
  }

  "the user has enrolled in PODS as both a PSA AND a PSP" must {
    "have access to PSP page when he has chosen to act as a PSP" in {
      val optionUAJson = UserAnswers()
        .set(AdministratorOrPractitionerPage, AdministratorOrPractitioner.Practitioner)
        .toOption.map(_.data)

      when(mockSessionDataCacheConnector.fetch(any())(any(), any())).thenReturn(Future.successful(optionUAJson))

      when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(authRetrievals(enrolments = bothEnrolments))
      when(mockUserAnswersCacheConnector.fetch(any(), any())).thenReturn(Future(None))
      val result = controllerWithIVEnrolment.onPageLoad()(fakeRequest)
      status(result) mustBe OK
    }

    "have access to PSP page when he has chosen to act as a PSP and has role of assistant" in {
      val optionUAJson = UserAnswers()
        .set(AdministratorOrPractitionerPage, AdministratorOrPractitioner.Practitioner)
        .toOption.map(_.data)

      when(mockSessionDataCacheConnector.fetch(any())(any(), any())).thenReturn(Future.successful(optionUAJson))

      when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(
        authRetrievals(affinityGroup = Some(AffinityGroup.Organisation),
          role = Some(Assistant),
          enrolments = bothEnrolments
        )
      )
      when(mockUserAnswersCacheConnector.fetch(any(), any())).thenReturn(Future(None))
      val result = controllerWithIVEnrolment.onPageLoad()(fakeRequest)
      status(result) mustBe OK
    }

    "redirect to cannot access as administrator when trying to access PSP page when chosen to act as a PSA" in {
      val optionUAJson = UserAnswers()
        .set(AdministratorOrPractitionerPage, AdministratorOrPractitioner.Administrator)
        .toOption.map(_.data)
      when(mockSessionDataCacheConnector.fetch(any())(any(), any())).thenReturn(Future.successful(optionUAJson))

      when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(authRetrievals(enrolments = bothEnrolments))
      when(mockUserAnswersCacheConnector.fetch(any(), any())).thenReturn(Future(None))
      val result = controllerWithIVEnrolment.onPageLoad()(fakeRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(frontendAppConfig.cannotAccessPageAsAdministratorUrl(frontendAppConfig.localFriendlyUrl(fakeRequest.uri)))
    }

    "redirect to administrator or practitioner page when trying to access PSA page when not chosen a role" in {
      val optionUAJson = Some(Json.obj())
      when(mockSessionDataCacheConnector.fetch(any())(any(), any())).thenReturn(Future.successful(optionUAJson))

      when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(authRetrievals(enrolments = bothEnrolments))
      when(mockUserAnswersCacheConnector.fetch(any(), any())).thenReturn(Future(None))
      val result = controllerWithIVEnrolment.onPageLoad()(fakeRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(frontendAppConfig.administratorOrPractitionerUrl)
    }
  }

  "Auth Action AuthenticatedAuthActionMustHaveEnrolment" when {

    "called for already enrolled User" must {
      "return OK" when {
        "coming from any page" in {
          when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(authRetrievals(enrolments = enrolmentPODS))
          when(mockUserAnswersCacheConnector.fetch(any(), any())).thenReturn(Future(None))
          val result = controllerWithIVEnrolment.onPageLoad()(fakeRequest)
          status(result) mustBe OK
        }
      }

      "return redirect to you must contact HMRC page" when {
        "deceasedFlag is true" in {
          when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(authRetrievals(enrolments = enrolmentPODS))
          when(mockUserAnswersCacheConnector.fetch(any(), any())).thenReturn(Future(None))
          when(mockMinimalConnector.getMinimalPspDetails(ArgumentMatchers.eq(pspId))(any(), any())).thenReturn(Future(minimalPspDeceased(true)))
          val result = controllerWithIVEnrolment.onPageLoad()(fakeRequest)
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(frontendAppConfig.youMustContactHMRCUrl)
        }
      }
    }

    "called for user with no enrolment" must {
      "return redirect to you need to register page" when {
        "coming from any page" in {
          when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(authRetrievals())
          when(mockUserAnswersCacheConnector.fetch(any(), any())).thenReturn(Future(None))
          val result = controllerWithIVEnrolment.onPageLoad()(fakeRequest)
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(frontendAppConfig.youNeedToRegisterUrl)
        }
      }
    }

    behave like authAction(controllerWithIVEnrolment, enrolments = enrolmentPODS)

    "called for Organisation that is an assistant with PODSPP (practitioner) enrolment" must {
      "display the page" in {
        when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any()))
          .thenReturn(authRetrievals(affinityGroup = Some(AffinityGroup.Organisation),
            role = Some(Assistant),
            enrolments = enrolmentPODS))
        val userAnswersData = Json.obj("areYouUKResident" -> true)
        when(mockUserAnswersCacheConnector.fetch(any(), any())).thenReturn(Future(Some(userAnswersData)))
        val result = controllerWithIVEnrolment.onPageLoad()(fakeRequest)
        status(result) mustBe OK
      }
    }

    "called for Organisation that is an assistant with no enrolments at all" must {
      "redirect" in {
        when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any()))
          .thenReturn(authRetrievals(affinityGroup = Some(AffinityGroup.Organisation),
            role = Some(Assistant),
            enrolments = noEnrolmentPODS))
        val userAnswersData = Json.obj("areYouUKResident" -> true)
        when(mockUserAnswersCacheConnector.fetch(any(), any())).thenReturn(Future(Some(userAnswersData)))
        val result = controllerWithIVEnrolment.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.AssistantNoAccessController.onPageLoad().url)
      }
    }

    "called for Organisation that is an assistant with no PODSPP (practitioner) enrolment, only a PSA enrolment" must {
      "redirect" in {
        when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any()))
          .thenReturn(authRetrievals(affinityGroup = Some(AffinityGroup.Organisation),
            role = Some(Assistant),
            enrolments = enrolmentPODSPSA))
        val userAnswersData = Json.obj("areYouUKResident" -> true)
        when(mockUserAnswersCacheConnector.fetch(any(), any())).thenReturn(Future(Some(userAnswersData)))
        val result = controllerWithIVEnrolment.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.AssistantNoAccessController.onPageLoad().url)
      }
    }
  }

  "Auth Action AuthenticatedAuthActionMustHaveNoEnrolmentWithIV" when {

    "called for already enrolled User" must {
      "return redirect to already registered page" when {
        "coming from any page" in {
          when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(authRetrievals(enrolments = enrolmentPODS))
          when(mockUserAnswersCacheConnector.fetch(any(), any())).thenReturn(Future(None))
          val result = controllerWithIVNoEnrolment.onPageLoad()(fakeRequest)
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(frontendAppConfig.returnToPspDashboardUrl)
        }
      }
    }

    behave like authAction(controllerWithIVNoEnrolment, enrolments = Enrolments(Set()))

    "called for Organisation user that is not an assistant" must {

      "return OK" when {
        "the user is non uk user" in {
          when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(authRetrievals())
          val userAnswersData = Json.obj("areYouUKResident" -> false)
          when(mockUserAnswersCacheConnector.fetch(any(), any())).thenReturn(Future(Some(userAnswersData)))
          val result = controllerWithIVNoEnrolment.onPageLoad()(fakeRequest)
          status(result) mustBe OK
        }

        "user is in UK and wants to register as Organisation in IV" in {
          when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(authRetrievals())
          val userAnswersData = Json.obj("areYouUKResident" -> true, "whatTypeBusiness" -> Companyorpartnership.toString)
          when(mockUserAnswersCacheConnector.fetch(any(), any())).thenReturn(Future(Some(userAnswersData)))

          val result = controllerWithIVNoEnrolment.onPageLoad()(fakeRequest)
          status(result) mustBe OK
        }
      }
    }

  }

  "Auth Action AuthenticatedAuthActionMustHaveNoEnrolmentWithNoIV" when {
    "called for Company user" must {
      "return OK and able to view the page and not redirect to IV" when {
        "they want to register as Individual" in {
          when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(authRetrievals())
          val userAnswersData = Json.obj("areYouInUK" -> true, "registerAsBusiness" -> false)
          when(mockUserAnswersCacheConnector.fetch(any(), any())).thenReturn(Future(Some(userAnswersData)))
          val authAction = new AuthenticatedAuthActionMustHaveNoEnrolmentWithNoIV(
            authConnector, frontendAppConfig, mockMinimalConnector, bodyParsers, mockSessionDataCacheConnector
          )

          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(fakeRequest)
          status(result) mustBe OK
        }
      }
    }
  }

  // scalastyle:off method.length
  def authAction(harness: Harness, enrolments: Enrolments): Unit = {
    "called for agent" must {
      "redirect" in {
        when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any()))
          .thenReturn(authRetrievals(affinityGroup = Some(AffinityGroup.Agent), enrolments = enrolments))
        val userAnswersData = Json.obj("areYouUKResident" -> true)
        when(mockUserAnswersCacheConnector.fetch(any(), any())).thenReturn(Future(Some(userAnswersData)))
        val result = harness.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.AgentCannotRegisterController.onPageLoad().url)
      }
    }

    "called for individual" must {
      "redirect" in {
        when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any()))
          .thenReturn(authRetrievals(affinityGroup = Some(AffinityGroup.Individual), enrolments = enrolments))
        val userAnswersData = Json.obj("areYouUKResident" -> true)
        when(mockUserAnswersCacheConnector.fetch(any(), any())).thenReturn(Future(Some(userAnswersData)))
        val result = harness.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.NeedAnOrganisationAccountController.onPageLoad().url)
      }
    }

    "the user hasn't logged in" must {
      "redirect the user to log in " in {
        when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(Future.failed(new MissingBearerToken))
        val result = harness.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must startWith(frontendAppConfig.loginUrl)
      }
    }

    "the user's session has expired" must {
      "redirect the user to log in " in {
        when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(Future.failed(new BearerTokenExpired))
        val result = harness.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must startWith(frontendAppConfig.loginUrl)
      }
    }

    "redirect the user to the unauthorised page" when {
      "the user doesn't have sufficient enrolments" in {
        when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(Future.failed(new InsufficientEnrolments))
        val result = harness.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad().url)
      }

      "the user doesn't have sufficient confidence level" in {
        when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(Future.failed(new InsufficientConfidenceLevel))
        val result = harness.onPageLoad()(FakeRequest("GET", "/test"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("http://localhost:9938/mdtp/uplift?origin=pods&confidenceLevel=250&completionURL=/test&failureURL=/pension-scheme-practitioner/unauthorised")
      }

      "the user used an unaccepted auth provider" in {
        when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(Future.failed(new UnsupportedAuthProvider))
        val result = harness.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad().url)
      }

      "the user has an unsupported affinity group" in {
        when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(Future.failed(new UnsupportedAffinityGroup))
        val result = harness.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad().url)
      }

      "there is no affinity group" in {
        when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any()))
          .thenReturn(authRetrievals(affinityGroup = None, enrolments = enrolments))

        val result = harness.onPageLoad()(FakeRequest("GET", "/foo"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad().url)
      }

      "the user is not an authorised user" in {
        when(authConnector.authorise[authRetrievalsType](any(), any())(any(), any())).thenReturn(Future.failed(new UnauthorizedException("Unknown User")))
        val result = harness.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad().url)
      }

    }

  }

}

object AuthActionSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {
  private val pspId = "00000000"
  private val email = "a@a.c"
  type authRetrievalsType = Option[String] ~ Option[AffinityGroup] ~ Enrolments ~ Option[Credentials] ~ Option[CredentialRole] ~ Option[String] ~ Option[String]

  override def beforeEach(): Unit = {
    reset(mockSessionDataCacheConnector)
    super.beforeEach()
  }

  private val mockSessionDataCacheConnector = mock[SessionDataCacheConnector]

  private val enrolmentPSP = Enrolment(
    key = "HMRC-PODSPP-ORG",
    identifiers = Seq(EnrolmentIdentifier(key = "PSPID", value = "20000000")),
    state = "",
    delegatedAuthRule = None
  )

  private val enrolmentPSA = Enrolment(
    key = "HMRC-PODS-ORG",
    identifiers = Seq(EnrolmentIdentifier(key = "PSAID", value = "A0000000")),
    state = "",
    delegatedAuthRule = None
  )

  private val bothEnrolments = Enrolments(Set(enrolmentPSA, enrolmentPSP))
  private val enrolmentPODS = Enrolments(Set(Enrolment("HMRC-PODSPP-ORG", Seq(EnrolmentIdentifier("PSPID", pspId)), "")))
  private val noEnrolmentPODS = Enrolments(Set())
  private val enrolmentPODSPSA = Enrolments(Set(enrolmentPSA))

  private def minimalPspDeceased(deceasedFlag: Boolean = false) = MinimalPSP(
    email = email,
    organisationName = None,
    individualDetails = None,
    rlsFlag = false,
    deceasedFlag = deceasedFlag
  )

  private def authRetrievals(affinityGroup: Option[AffinityGroup] = Some(AffinityGroup.Organisation),
                             enrolments: Enrolments = Enrolments(Set()),
                             creds: Option[Credentials] = Option(Credentials(providerId = "test provider", providerType = "")),
                             role: Option[CredentialRole] = Option(User),
                             groupId: Option[String] = Some("test-group-id"),
                             nino: Option[String] = Some("AA000003D")
                            ): Future[authRetrievalsType] = Future.successful(
    new~(new~(new~(new~(new~(new~(
      Some("id"),
      affinityGroup),
      enrolments),
      creds),
      role),
      groupId),
      nino)
  )

  class Harness(identifierAction: AuthAction) {
    def onPageLoad(): Action[AnyContent] = identifierAction {
      implicit request =>
        Ok(Json.obj("userId" -> request.user.userId))
    }
  }

  private val mockMinimalConnector: MinimalConnector = mock[MinimalConnector]
  private val mockUserAnswersCacheConnector: UserAnswersCacheConnector = mock[UserAnswersCacheConnector]
  private val authConnector: AuthConnector = mock[AuthConnector]
  private val bodyParsers: BodyParsers.Default = fakeApplication().injector.instanceOf[BodyParsers.Default]

  val authActionWithIVEnrolment = new AuthenticatedAuthActionMustHaveEnrolmentWithNoIV(
    authConnector, frontendAppConfig, mockMinimalConnector, bodyParsers, mockSessionDataCacheConnector
  )

  val authActionWithIVNoEnrolment = new AuthenticatedAuthActionMustHaveNoEnrolmentWithIV(
    authConnector, frontendAppConfig,
    mockMinimalConnector, bodyParsers, mockSessionDataCacheConnector)

  val controllerWithIVEnrolment = new Harness(authActionWithIVEnrolment)
  val controllerWithIVNoEnrolment = new Harness(authActionWithIVNoEnrolment)
}
