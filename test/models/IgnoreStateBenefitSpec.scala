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

class IgnoreStateBenefitSpec extends TestUtils {

  val fullIgnoreStateBenefitModel: IgnoreStateBenefit = IgnoreStateBenefit(true)
  val fullCreateUpdateStateBenefitJson: JsValue = Json.parse("""{"ignoreBenefit": true}""")

  "The IgnoreStateBenefit model" should {

    "serialize" when {
      "there is a valid model" in {
        Json.toJson(fullIgnoreStateBenefitModel) mustBe fullCreateUpdateStateBenefitJson
      }
    }

    "deserialize" when {
      "parsing valid ignore benefit json" in {
        fullCreateUpdateStateBenefitJson.as[IgnoreStateBenefit] mustBe fullIgnoreStateBenefitModel
      }
    }
  }

}
