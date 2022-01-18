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

import play.api.libs.json.{JsValue, Json}
import utils.TestUtils

class CreateUpdateOverrideStateBenefitSpec extends TestUtils {

  val fullCreateUpdateStateBenefitModel: CreateUpdateOverrideStateBenefit = CreateUpdateOverrideStateBenefit(
    amount = 21.22, taxPaid = Some(0.50))

  val fullCreateUpdateStateBenefitJson: JsValue = Json.parse(
    """{
      |	"amount": 21.22,
      |	"taxPaid": 0.50
      |}""".stripMargin)

  val minimalCreateUpdateStateBenefitModel: CreateUpdateOverrideStateBenefit = CreateUpdateOverrideStateBenefit(
    amount = 21.22, None)

  val minimalCreateUpdateStateBenefitJson: JsValue = Json.parse("""{"amount": 21.22}""".stripMargin)

  "The CreateUpdateOverrideStateBenefit model" should {

    "serialize valid values" when {
      "there is a full model" in {
        Json.toJson(fullCreateUpdateStateBenefitModel) mustBe fullCreateUpdateStateBenefitJson
      }
      "there is a minimal model" in {
        Json.toJson(minimalCreateUpdateStateBenefitModel) mustBe minimalCreateUpdateStateBenefitJson
      }
    }

    "deserialize valid values" when {
      "parsing full benefit amount json" in {
        fullCreateUpdateStateBenefitJson.as[CreateUpdateOverrideStateBenefit] mustBe fullCreateUpdateStateBenefitModel
      }
      "there is a minimal amount model" in {
        minimalCreateUpdateStateBenefitJson.as[CreateUpdateOverrideStateBenefit] mustBe minimalCreateUpdateStateBenefitModel
      }
    }
  }

}
