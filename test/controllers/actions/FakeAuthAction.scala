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
import models.requests.{AuthenticatedRequest, PSPUser, UserType}
import models.requests.UserType.UserType
import play.api.mvc._
import uk.gov.hmrc.domain.Nino

import scala.concurrent.{ExecutionContext, Future}

case class FakeAuthAction @Inject()(bodyParsers: PlayBodyParsers) extends AuthAction {
  private val defaultUserType: UserType = UserType.Organisation
  private val defaultPspId: String = "test psp id"
  implicit val executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global
  override def invokeBlock[A](
    request: Request[A],
    block: AuthenticatedRequest[A] => Future[Result]
  ): Future[Result] =
    block(
      AuthenticatedRequest(
        request, "id",
        PSPUser(defaultUserType, Some(Nino("AB100100A")), isExistingPSP = false, None, Some(defaultPspId))
      )
    )
  override def parser: BodyParser[AnyContent] =
    bodyParsers.default
}

case class FakeAuthActionNoEnrolment @Inject()(bodyParsers: PlayBodyParsers) extends AuthAction {
  private val defaultUserType: UserType = UserType.Organisation
  implicit val executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global
  override def invokeBlock[A](
    request: Request[A],
    block: AuthenticatedRequest[A] => Future[Result]
  ): Future[Result] =
    block(
      AuthenticatedRequest(
        request, "id",
        PSPUser(defaultUserType, Some(Nino("AB100100A")), isExistingPSP = false, None, None)
      )
    )
  override def parser: BodyParser[AnyContent] =
    bodyParsers.default
}

