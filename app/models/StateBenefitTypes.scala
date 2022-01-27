/*
 * Copyright 2022 HM Revenue & Customs
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

sealed abstract class StateBenefitTypes(val benefitType: String)

object StateBenefitTypes {

  private val statePension = "statePension"
  private val incapacityBenefit = "incapacityBenefit"
  private val statePensionLumpSum = "statePensionLumpSum"
  private val employmentSupportAllowance = "employmentSupportAllowance"
  private val jobSeekersAllowance = "jobSeekersAllowance"
  private val bereavementAllowance = "bereavementAllowance"
  private val otherStateBenefits = "otherStateBenefits"

  case object StatePension extends StateBenefitTypes(statePension)
  case object IncapacityBenefit extends StateBenefitTypes(incapacityBenefit)
  case object StatePensionLumpSum extends StateBenefitTypes(statePensionLumpSum)
  case object EmploymentSupportAll extends StateBenefitTypes(employmentSupportAllowance)
  case object JobSeekersAllowance extends StateBenefitTypes(jobSeekersAllowance)
  case object BereavementAllowance extends StateBenefitTypes(bereavementAllowance)
  case object OtherStateBenefits extends StateBenefitTypes(otherStateBenefits)

  def apply(string: String): Option[StateBenefitTypes] = {
    string match {
      case `statePension`               => Some(StatePension)
      case `incapacityBenefit`          => Some(IncapacityBenefit)
      case `statePensionLumpSum`        => Some(StatePensionLumpSum)
      case `employmentSupportAllowance` => Some(EmploymentSupportAll)
      case `jobSeekersAllowance`        => Some(JobSeekersAllowance)
      case `bereavementAllowance`       => Some(BereavementAllowance)
      case `otherStateBenefits`         => Some(OtherStateBenefits)
      case _ => None
    }
  }
}
