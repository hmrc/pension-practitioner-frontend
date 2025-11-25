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

package models.register

import models.TolerantAddress
import play.api.libs.functional.syntax._
import play.api.libs.json._

abstract class RegisterWithIdResponse(address: TolerantAddress)

case class OrganisationRegisterWithIdResponse(organisation: Organisation, address: TolerantAddress) extends RegisterWithIdResponse(address)

case class IndividualRegisterWithIdResponse(individual: TolerantIndividual, address: TolerantAddress) extends RegisterWithIdResponse(address)

case class OrganisationRegistration(response: OrganisationRegisterWithIdResponse, info: RegistrationInfo)

case class IndividualRegistration(response: IndividualRegisterWithIdResponse, info: RegistrationInfo)

object RegisterWithIdResponse {

  implicit lazy val readsOrganizationRegisterWithIdResponse: Reads[OrganisationRegisterWithIdResponse] =
    ((JsPath \ "organisation").read[Organisation] ~ (JsPath \ "address").read[TolerantAddress]).apply((organisation, address) => OrganisationRegisterWithIdResponse(organisation, address))

  implicit lazy val writesOrganizationRegisterWithIdResponse: Writes[OrganisationRegisterWithIdResponse] =
    Writes[OrganisationRegisterWithIdResponse] { response =>
      Json.obj(
        "address" -> response.address,
        "organisation" -> response.organisation
      )
    }

  implicit lazy val readsIndividualRegisterWithIdResponse: Reads[IndividualRegisterWithIdResponse] =
    for {
      individual <- (JsPath \ "individual").read[TolerantIndividual]
      address <- (JsPath \ "address").read[TolerantAddress]
    } yield IndividualRegisterWithIdResponse(individual, address)

  implicit lazy val writesIndividualRegisterWithIdResponse: Writes[IndividualRegisterWithIdResponse] = Writes { response =>
    Json.obj(
      "individual" -> response.individual,
      "address" -> response.address
    )
  }

  implicit lazy val formatsIndividualRegisterWithIdResponse: Format[IndividualRegisterWithIdResponse] = Format(readsIndividualRegisterWithIdResponse, writesIndividualRegisterWithIdResponse)

}
