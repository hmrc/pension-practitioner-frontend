/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.base

import base.SpecBase
import com.kenshoo.play.metrics.Metrics
import config.FrontendAppConfig
import connectors.cache.UserAnswersCacheConnector
import controllers.actions._
import models.UserAnswers
import models.requests.DataRequest
import navigators.CompoundNavigator
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.HeaderNames
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.mvc.{ActionFilter, AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Result}
import play.api.test.Helpers.{GET, POST}
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.nunjucks.NunjucksRenderer
import utils.TestMetrics
import utils.annotations.{AuthMustHaveEnrolment, AuthMustHaveNoEnrolmentWithIV}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

trait ControllerSpecBase extends SpecBase with BeforeAndAfterEach with MockitoSugar {

  val FakeActionFilter: ActionFilter[DataRequest] = new ActionFilter[DataRequest] {
    override protected def executionContext: ExecutionContext = global

    override protected def filter[A](request: DataRequest[A]): Future[Option[Result]] = Future.successful(None)
  }

  override def beforeEach(): Unit = {
    reset(mockRenderer)
    reset(mockUserAnswersCacheConnector)
    reset(mockCompoundNavigator)
  }

  protected def mockDataRetrievalAction: DataRetrievalAction = mock[DataRetrievalAction]

  protected val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]

  protected val mockUserAnswersCacheConnector: UserAnswersCacheConnector = mock[UserAnswersCacheConnector]
  protected val mockCompoundNavigator: CompoundNavigator = mock[CompoundNavigator]
  protected val mockRenderer: NunjucksRenderer = mock[NunjucksRenderer]

  def modules: Seq[GuiceableModule] = Seq(
    bind[Metrics].toInstance(new TestMetrics),
    bind[DataRequiredAction].to[DataRequiredActionImpl],
    bind[AuthAction].to[FakeAuthAction],
    bind[AuthAction].qualifiedWith(classOf[AuthMustHaveNoEnrolmentWithIV]).to[FakeAuthActionNoEnrolment],
    bind[AuthAction].qualifiedWith(classOf[AuthMustHaveEnrolment]).to[FakeAuthAction],
    bind[NunjucksRenderer].toInstance(mockRenderer),
    bind[FrontendAppConfig].toInstance(mockAppConfig),
    bind[UserAnswersCacheConnector].toInstance(mockUserAnswersCacheConnector),
    bind[CompoundNavigator].toInstance(mockCompoundNavigator)
  )

  protected def applicationBuilder(
                                    userAnswers: Option[UserAnswers] = None,
                                    extraModules: Seq[GuiceableModule] = Seq.empty
                                  ): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        //turn off metrics
        "metrics.jvm" -> false,
        "metrics.enabled" -> false
      )
      .overrides(
        modules ++ extraModules ++ Seq[GuiceableModule](
          bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers))
        ): _*
      )

  protected def applicationBuilderMutableRetrievalAction(
                                                          mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction,
                                                          extraModules: Seq[GuiceableModule] = Seq.empty
                                                        ): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        //turn off metrics
        "metrics.jvm" -> false,
        "metrics.enabled" -> false
      )
      .overrides(
        modules ++ extraModules ++ Seq[GuiceableModule](
          bind[DataRetrievalAction].toInstance(mutableFakeDataRetrievalAction)
        ): _*
      )

  protected def httpGETRequest(path: String): FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, path)

  protected def httpPOSTRequest(path: String, values: Map[String, Seq[String]]): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest
      .apply(
        method = POST,
        uri = path,
        headers = FakeHeaders(Seq(HeaderNames.HOST -> "localhost")),
        body = AnyContentAsFormUrlEncoded(values))
}
