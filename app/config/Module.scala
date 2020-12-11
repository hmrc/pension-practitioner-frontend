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

package config

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import connectors.cache.{UserAnswersCacheConnectorImpl, UserAnswersCacheConnector}
import controllers.actions._
import navigators._
import utils.annotations.AuthMustHaveEnrolment
import utils.annotations.AuthMustHaveNoEnrolmentWithIV
import utils.annotations.AuthMustHaveNoEnrolmentWithNoIV

class Module extends AbstractModule {

  override def configure(): Unit = {

    val navigators = Multibinder.newSetBinder(binder(), classOf[Navigator])
    navigators.addBinding().to(classOf[IndividualNavigator])

    bind(classOf[UserAnswersCacheConnector]).to(classOf[UserAnswersCacheConnectorImpl]).asEagerSingleton()
    bind(classOf[DataRetrievalAction]).to(classOf[DataRetrievalActionImpl]).asEagerSingleton()
    bind(classOf[DataRequiredAction]).to(classOf[DataRequiredActionImpl]).asEagerSingleton()

    bind(classOf[AuthAction]).to(classOf[AuthenticatedAuthActionWithIV]).asEagerSingleton()
    bind(classOf[AuthAction]).annotatedWith(classOf[AuthMustHaveNoEnrolmentWithIV])
      .to(classOf[AuthenticatedAuthActionMustHaveNoEnrolmentWithIV]).asEagerSingleton()
    bind(classOf[AuthAction]).annotatedWith(classOf[AuthMustHaveEnrolment])
      .to(classOf[AuthenticatedAuthActionMustHaveEnrolment]).asEagerSingleton()
    bind(classOf[AuthAction]).annotatedWith(classOf[AuthMustHaveNoEnrolmentWithNoIV])
      .to(classOf[AuthenticatedAuthActionMustHaveNoEnrolmentWithNoIV]).asEagerSingleton()

    bind(classOf[CompoundNavigator]).to(classOf[CompoundNavigatorImpl])


    navigators.addBinding().to(classOf[PractitionerNavigator])
    navigators.addBinding().to(classOf[CompanyNavigator])
    navigators.addBinding().to(classOf[PartnershipNavigator])
    navigators.addBinding().to(classOf[DeregisterNavigator])

    bind(classOf[CompoundNavigator]).to(classOf[CompoundNavigatorImpl])

  }
}
