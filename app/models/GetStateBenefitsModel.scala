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

package models

import play.api.libs.json.{Json, OFormat}

case class StateBenefit(
  benefitId: String,
  startDate: String,
  dateIgnored: Option[String],
  submittedOn: Option[String],
  endDate: Option[String],
  amount: Option[BigDecimal],
  taxPaid: Option[BigDecimal]
)

object StateBenefit {
  implicit val format: OFormat[StateBenefit] = Json.format[StateBenefit]
}

case class StateBenefits(
  incapacityBenefit: Option[Seq[StateBenefit]],
  statePension: Option[StateBenefit],
  statePensionLumpSum: Option[StateBenefit],
  employmentSupportAllowance: Option[Seq[StateBenefit]],
  jobSeekersAllowance: Option[Seq[StateBenefit]],
  bereavementAllowance: Option[StateBenefit],
  otherStateBenefits: Option[StateBenefit]
)

object StateBenefits {
  implicit val format: OFormat[StateBenefits] = Json.format[StateBenefits]
}

case class GetStateBenefitsModel(
  stateBenefits: Option[StateBenefits],
  customerAddedStateBenefits: Option[StateBenefits]
)

object GetStateBenefitsModel {
  implicit val format: OFormat[GetStateBenefitsModel] = Json.format[GetStateBenefitsModel]
}
