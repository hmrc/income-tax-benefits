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

import com.codahale.metrics.SharedMetricRegistries
import play.api.libs.json.{JsValue, Json}
import utils.TestUtils

class AddStateBenefitModelSpec extends TestUtils {
  SharedMetricRegistries.clear()

  val benefitId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c934"

  val fullRequestModel: AddStateBenefitRequestModel =
    AddStateBenefitRequestModel(
      benefitType = "incapacityBenefit",
      endDate = Some("2020-12-03"),
      startDate = "2020-08-03"
    )

  val fullResponseModel: AddStateBenefitResponseModel =
    AddStateBenefitResponseModel(benefitId = "b1e8057e-fbbc-47a8-a8b4-78d9f015c253")

  val fullRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |  "benefitType": "incapacityBenefit",
      |  "startDate": "2020-08-03",
      |  "endDate": "2020-12-03"
      |}
      |""".stripMargin
  )

  val fullResponseBodyJson: JsValue = Json.parse(
    """
      |{
      |"benefitId" : "b1e8057e-fbbc-47a8-a8b4-78d9f015c253"
      |}
      |""".stripMargin
  )

  "AddStateBenefitRequestModel" should {
    "serialize valid values" in {
      Json.toJson(fullRequestModel) mustBe fullRequestBodyJson
    }

    "deserialize valid values" in {
      fullRequestBodyJson.as[AddStateBenefitRequestModel] mustBe fullRequestModel
    }
  }

  "AddStateBenefitResponseModel" should {
    "serialize valid values" in {
      Json.toJson(fullResponseModel) mustBe fullResponseBodyJson
    }
    "deserialize valid values" in {
      fullResponseBodyJson.as[AddStateBenefitResponseModel] mustBe fullResponseModel
    }
  }
}
