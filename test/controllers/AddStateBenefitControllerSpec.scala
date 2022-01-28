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

package controllers

import connectors.httpParsers.AddStateBenefitHttpParser.AddStateBenefitResponse
import models.{AddStateBenefitRequestModel, AddStateBenefitResponseModel}
import org.scalamock.handlers.CallHandler4
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.json.Json
import services.StateBenefitsService
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.Future

class AddStateBenefitControllerSpec extends TestUtils {

  val stateBenefitsService: StateBenefitsService = mock[StateBenefitsService]
  val stateBenefitController = new StateBenefitsController(stateBenefitsService, authorisedAction, mockControllerComponents)
  val nino: String = "123456789"
  val mtdItID: String = "123123123"
  val taxYear: Int = 2021
  val responseModel: AddStateBenefitResponseModel = AddStateBenefitResponseModel("a1e8057e-fbbc-47a8-a8b4-78d9f015c934")

  def requestModel(benefitType: String): AddStateBenefitRequestModel = AddStateBenefitRequestModel(benefitType, "2020-01-01", Some("2020-03-01"))

  def mockAddStateBenefitValid(): CallHandler4[String, Int, AddStateBenefitRequestModel, HeaderCarrier, Future[AddStateBenefitResponse]] = {
    (stateBenefitsService.addStateBenefit(_: String, _: Int, _: AddStateBenefitRequestModel)(_: HeaderCarrier))
      .expects(*, *, *, *)
      .returning(Future.successful(Right(responseModel)))
  }


  "calling .addStateBenefit" should {

    Seq("statePension", "incapacityBenefit", "statePensionLumpSum", "employmentSupportAllowance", "jobSeekersAllowance",
      "bereavementAllowance", "otherStateBenefits").foreach { benefitType =>

      s"return an OK 200 response when request body has valid benefitType $benefitType" in {
        val result = {
          mockAuth()
          mockAddStateBenefitValid()
          stateBenefitController.addStateBenefit(nino, taxYear)(fakeRequest.withJsonBody(Json.toJson(requestModel(benefitType))))
        }
        status(result) mustBe OK
        Json.parse(bodyOf(result)) mustBe
          Json.parse("""{"benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c934"}""".stripMargin)
      }
    }

    "return 400 when request body has an invalid benefit type" in {
      val result = {
        mockAuth()
        stateBenefitController.addStateBenefit(nino, taxYear)(fakeRequest.withJsonBody(Json.toJson(requestModel("invalidType"))))
      }
      status(result) mustBe BAD_REQUEST
    }
  }
}
