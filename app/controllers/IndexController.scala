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

package controllers

import java.time.LocalDate

import connectors.RegistrationConnector
import javax.inject.Inject
import models.Address
import models.registration.{Organisation, OrganisationTypeEnum, RegistrationLegalStatus}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController

import scala.concurrent.ExecutionContext

class IndexController @Inject()(
    val controllerComponents: MessagesControllerComponents,
    renderer: Renderer,
    registrationConnector: RegistrationConnector
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = Action.async { implicit request =>

    registrationConnector.registerWithIdIndividual("AB123456C").map {response =>
      println("\n\n Reg with id individual: "+response)
    }

    registrationConnector.registerWithIdOrganisation("12345678",
      Organisation("organisation name", OrganisationTypeEnum.CorporateBody), RegistrationLegalStatus.LimitedCompany).map { response =>
      println("\n\n Reg with id org: "+response)
    }

    registrationConnector.registerWithNoIdIndividual("first", "last",
      Address("x", "y", None, None, None, "FR"), LocalDate.of(2002, 12, 1)).map {response =>
      println("\n\n Reg with no id individual: "+response)
    }

    registrationConnector.registerWithNoIdOrganisation("org name", Address("a", "b", None, None, None, "GB"),
      RegistrationLegalStatus.Partnership).map {response =>
      println("\n\n Reg with no id org: "+response)
    }

    renderer.render("index.njk").map(Ok(_))
  }
}
