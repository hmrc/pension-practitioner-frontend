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
import models.requests.{AuthenticatedRequest, OptionalDataRequest, PSPUser, UserType}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DataRetrievalActionSpec extends SpecBase with MockitoSugar with ScalaFutures {

  private val fakeRequest = FakeRequest("", "")
  class Harness(dataCacheConnector: UserAnswersCacheConnector) extends DataRetrievalActionImpl(dataCacheConnector) {
    def callTransform[A](request: AuthenticatedRequest[A]): Future[OptionalDataRequest[A]] = transform(request)
  }

  private val dataCacheConnector: UserAnswersCacheConnector = mock[UserAnswersCacheConnector]

  "Data Retrieval Action" when {
    "there is no data in the cache" must {
      "set userAnswers to 'None' in the request" in {
        when(dataCacheConnector.fetch(any(), any())) thenReturn Future(None)
        val action = new Harness(dataCacheConnector)

        val futureResult = action.callTransform(AuthenticatedRequest(fakeRequest, "id",
          PSPUser(UserType.Organisation, None, isExistingPSP = false, None, None)))

        whenReady(futureResult) { result =>
          result.userAnswers.isEmpty mustBe true
        }
      }
    }

    "there is data in the cache" must {
      "build a userAnswers object and add it to the request" in {
        when(dataCacheConnector.fetch(any(), any())) thenReturn Future.successful(Some(Json.obj()))
        val action = new Harness(dataCacheConnector)

        val futureResult = action.callTransform(AuthenticatedRequest(fakeRequest, "id",
          PSPUser(UserType.Organisation, None, isExistingPSP = false, None, None)))

        whenReady(futureResult) { result =>
          result.userAnswers.isDefined mustBe true
        }
      }
    }
  }
}
