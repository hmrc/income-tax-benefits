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

import connectors.httpParsers.GetEmploymentBenefitsHttpParser.GetEmploymentBenefitsResponse
import models.{DesErrorBodyModel, DesErrorModel}
import org.scalamock.handlers.CallHandler5
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import services.GetEmploymentBenefitsService
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.Future

class GetEmploymentBenefitsControllerSpec extends TestUtils {

  val getEmploymentBenefitsService: GetEmploymentBenefitsService = mock[GetEmploymentBenefitsService]
  val getEmploymentBenefitsController = new GetEmploymentBenefitsController(getEmploymentBenefitsService,authorisedAction, mockControllerComponents)
  val nino :String = "123456789"
  val mtdItID :String = "123123123"
  val taxYear: Int = 1234
  val id = "a1e8057e-fbbc-47a8-a8b4-78d9f015c934"
  val view = "CUSTOMER"
  val badRequestModel: DesErrorBodyModel = DesErrorBodyModel("INVALID_NINO", "Nino is invalid")
  val notFoundModel: DesErrorBodyModel = DesErrorBodyModel("NOT_FOUND_INCOME_SOURCE", "Can't find income source")
  val serverErrorModel: DesErrorBodyModel = DesErrorBodyModel("SERVER_ERROR", "Internal server error")
  val serviceUnavailableErrorModel: DesErrorBodyModel = DesErrorBodyModel("SERVICE_UNAVAILABLE", "Service is unavailable")
  private val fakeGetRequest = FakeRequest("GET", "/").withHeaders("MTDITID" -> "1234567890")

  def mockGetEmploymentBenefitsValid(): CallHandler5[String, String, Int, String, HeaderCarrier, Future[GetEmploymentBenefitsResponse]] = {
    val customerExampleResponse: GetEmploymentBenefitsResponse = Right(customerExample)
    (getEmploymentBenefitsService.getEmploymentBenefits(_: String, _: String, _: Int, _:String)(_: HeaderCarrier))
      .expects(*, *, *, *, *)
      .returning(Future.successful(customerExampleResponse))
  }

  def mockGetEmploymentBenefitsBadRequest(): CallHandler5[String, String, Int, String, HeaderCarrier, Future[GetEmploymentBenefitsResponse]] = {
    val invalidEmploymentList: GetEmploymentBenefitsResponse = Left(DesErrorModel(BAD_REQUEST, badRequestModel))
    (getEmploymentBenefitsService.getEmploymentBenefits(_: String, _:String, _: Int, _:String)(_: HeaderCarrier))
      .expects(*, *, *, *, *)
      .returning(Future.successful(invalidEmploymentList))
  }

  def mockGetEmploymentBenefitsNotFound(): CallHandler5[String, String, Int, String, HeaderCarrier, Future[GetEmploymentBenefitsResponse]] = {
    val invalidEmploymentList: GetEmploymentBenefitsResponse = Left(DesErrorModel(NOT_FOUND, notFoundModel))
    (getEmploymentBenefitsService.getEmploymentBenefits(_: String, _:String, _: Int, _:String)(_: HeaderCarrier))
      .expects(*, *, *, *, *)
      .returning(Future.successful(invalidEmploymentList))
  }

  def mockGetEmploymentBenefitsServerError(): CallHandler5[String, String, Int, String, HeaderCarrier, Future[GetEmploymentBenefitsResponse]] = {
    val invalidEmploymentList: GetEmploymentBenefitsResponse = Left(DesErrorModel(INTERNAL_SERVER_ERROR, serverErrorModel))
    (getEmploymentBenefitsService.getEmploymentBenefits(_: String, _:String, _: Int, _:String)(_: HeaderCarrier))
      .expects(*, *, *, *, *)
      .returning(Future.successful(invalidEmploymentList))
  }

  def mockGetEmploymentBenefitsServiceUnavailable(): CallHandler5[String, String, Int, String, HeaderCarrier, Future[GetEmploymentBenefitsResponse]] = {
    val invalidEmploymentList: GetEmploymentBenefitsResponse = Left(DesErrorModel(SERVICE_UNAVAILABLE, serviceUnavailableErrorModel))
    (getEmploymentBenefitsService.getEmploymentBenefits(_: String, _:String, _: Int, _:String)(_: HeaderCarrier))
      .expects(*, *, *, *, *)
      .returning(Future.successful(invalidEmploymentList))
  }

  "calling .getEmploymentBenefits" should {

    "with existing employments" should {

      "return an OK 200 response when called as an individual" in {
        val result = {
          mockAuth()
          mockGetEmploymentBenefitsValid()
          getEmploymentBenefitsController.getEmploymentBenefits(nino, id, taxYear, view)(fakeGetRequest)
        }
        status(result) mustBe OK
        Json.parse(bodyOf(result)) mustBe
          Json.parse("""{
            |	"submittedOn": "2020-01-04T05:01:01Z",
            |	"customerAdded": "2020-04-04T01:01:01Z",
            |	"employment": {
            |		"benefitsInKind": {
            |			"accommodation": 455.67,
            |			"assets": 435.54,
            |			"assetTransfer": 24.58,
            |			"beneficialLoan": 33.89,
            |			"car": 3434.78,
            |			"carFuel": 34.56,
            |			"educationalServices": 445.67,
            |			"entertaining": 434.45,
            |			"expenses": 3444.32,
            |			"medicalInsurance": 4542.47,
            |			"telephone": 243.43,
            |			"service": 45.67,
            |			"taxableExpenses": 24.56,
            |			"van": 56.29,
            |			"vanFuel": 14.56,
            |			"mileage": 34.23,
            |			"nonQualifyingRelocationExpenses": 54.62,
            |			"nurseryPlaces": 84.29,
            |			"otherItems": 67.67,
            |			"paymentsOnEmployeesBehalf": 67.23,
            |			"personalIncidentalExpenses": 74.29,
            |			"qualifyingRelocationExpenses": 78.24,
            |			"employerProvidedProfessionalSubscriptions": 84.56,
            |			"employerProvidedServices": 56.34,
            |			"incomeTaxPaidByDirector": 67.34,
            |			"travelAndSubsistence": 56.89,
            |			"vouchersAndCreditCards": 34.9,
            |			"nonCash": 23.89
            |		}
            |	}
            |}""".stripMargin)
      }

      "return an OK 200 response when called as an agent" in {
        val result = {
          mockAuthAsAgent()
          mockGetEmploymentBenefitsValid()
          getEmploymentBenefitsController.getEmploymentBenefits(nino, id, taxYear, view)(fakeGetRequest)
        }
        status(result) mustBe OK
        Json.parse(bodyOf(result)) mustBe
          Json.parse("""{
                       |	"submittedOn": "2020-01-04T05:01:01Z",
                       |	"customerAdded": "2020-04-04T01:01:01Z",
                       |	"employment": {
                       |		"benefitsInKind": {
                       |			"accommodation": 455.67,
                       |			"assets": 435.54,
                       |			"assetTransfer": 24.58,
                       |			"beneficialLoan": 33.89,
                       |			"car": 3434.78,
                       |			"carFuel": 34.56,
                       |			"educationalServices": 445.67,
                       |			"entertaining": 434.45,
                       |			"expenses": 3444.32,
                       |			"medicalInsurance": 4542.47,
                       |			"telephone": 243.43,
                       |			"service": 45.67,
                       |			"taxableExpenses": 24.56,
                       |			"van": 56.29,
                       |			"vanFuel": 14.56,
                       |			"mileage": 34.23,
                       |			"nonQualifyingRelocationExpenses": 54.62,
                       |			"nurseryPlaces": 84.29,
                       |			"otherItems": 67.67,
                       |			"paymentsOnEmployeesBehalf": 67.23,
                       |			"personalIncidentalExpenses": 74.29,
                       |			"qualifyingRelocationExpenses": 78.24,
                       |			"employerProvidedProfessionalSubscriptions": 84.56,
                       |			"employerProvidedServices": 56.34,
                       |			"incomeTaxPaidByDirector": 67.34,
                       |			"travelAndSubsistence": 56.89,
                       |			"vouchersAndCreditCards": 34.9,
                       |			"nonCash": 23.89
                       |		}
                       |	}
                       |}""".stripMargin)
      }
    }

    "without existing employments" should {

      "return an NotFound response when called as an individual" in {
        val result = {
          mockAuth()
          mockGetEmploymentBenefitsNotFound()
          getEmploymentBenefitsController.getEmploymentBenefits(nino, id, taxYear, view)(fakeGetRequest)
        }
        status(result) mustBe NOT_FOUND
        Json.parse(bodyOf(result)) mustBe
          Json.parse("""{"code":"NOT_FOUND_INCOME_SOURCE","reason":"Can't find income source"}""".stripMargin)
      }

      "return an NotFound response when called as an agent" in {
        val result = {
          mockAuthAsAgent()
          mockGetEmploymentBenefitsNotFound()
          getEmploymentBenefitsController.getEmploymentBenefits(nino, id, taxYear, view)(fakeGetRequest)
        }
        status(result) mustBe NOT_FOUND
        Json.parse(bodyOf(result)) mustBe
          Json.parse("""{"code":"NOT_FOUND_INCOME_SOURCE","reason":"Can't find income source"}""".stripMargin)

      }

    }

    "with an invalid NINO" should {

      "return an BadRequest response when called as an individual" in {
        val result = {
          mockAuth()
          mockGetEmploymentBenefitsBadRequest()
          getEmploymentBenefitsController.getEmploymentBenefits(nino, id, taxYear, view)(fakeGetRequest)
        }
        status(result) mustBe BAD_REQUEST
        Json.parse(bodyOf(result)) mustBe
          Json.parse("""{"code":"INVALID_NINO","reason":"Nino is invalid"}""".stripMargin)
      }

      "return an BadRequest response when called as an agent" in {
        val result = {
          mockAuthAsAgent()
          mockGetEmploymentBenefitsBadRequest()
          getEmploymentBenefitsController.getEmploymentBenefits(nino, id, taxYear, view)(fakeGetRequest)
        }
        status(result) mustBe BAD_REQUEST
        Json.parse(bodyOf(result)) mustBe
          Json.parse("""{"code":"INVALID_NINO","reason":"Nino is invalid"}""".stripMargin)
      }
    }
    "with an invalid view" should {

      "return an BadRequest response when called as an individual" in {
        val result = {
          mockAuth()
          getEmploymentBenefitsController.getEmploymentBenefits(nino, id, taxYear, "view")(fakeGetRequest)
        }
        status(result) mustBe BAD_REQUEST
        Json.parse(bodyOf(result)) mustBe
          Json.parse("""{"code":"INVALID_VIEW","reason":"Submission has not passed validation. Invalid query parameter view."}""".stripMargin)
      }
    }

    "with something that causes and internal server error in DES" should {

      "return an BadRequest response when called as an individual" in {
        val result = {
          mockAuth()
          mockGetEmploymentBenefitsServerError()
          getEmploymentBenefitsController.getEmploymentBenefits(nino, id, taxYear, view)(fakeGetRequest)
        }
        status(result) mustBe INTERNAL_SERVER_ERROR
        Json.parse(bodyOf(result)) mustBe
          Json.parse("""{"code":"SERVER_ERROR","reason":"Internal server error"}""".stripMargin)
      }

      "return an BadRequest response when called as an agent" in {
        val result = {
          mockAuthAsAgent()
          mockGetEmploymentBenefitsServerError()
          getEmploymentBenefitsController.getEmploymentBenefits(nino, id, taxYear, view)(fakeGetRequest)
        }
        status(result) mustBe INTERNAL_SERVER_ERROR
        Json.parse(bodyOf(result)) mustBe
          Json.parse("""{"code":"SERVER_ERROR","reason":"Internal server error"}""".stripMargin)
      }
    }

    "with an unavailable service" should {

      "return an Service_Unavailable response when called as an individual" in {
        val result = {
          mockAuth()
          mockGetEmploymentBenefitsServiceUnavailable()
          getEmploymentBenefitsController.getEmploymentBenefits(nino, id, taxYear, view)(fakeGetRequest)
        }
        status(result) mustBe SERVICE_UNAVAILABLE
        Json.parse(bodyOf(result)) mustBe
          Json.parse("""{"code":"SERVICE_UNAVAILABLE","reason":"Service is unavailable"}""".stripMargin)
      }

      "return an Service_Unavailable response when called as an agent" in {
        val result = {
          mockAuthAsAgent()
          mockGetEmploymentBenefitsServiceUnavailable()
          getEmploymentBenefitsController.getEmploymentBenefits(nino, id, taxYear, view)(fakeGetRequest)
        }
        status(result) mustBe SERVICE_UNAVAILABLE
        Json.parse(bodyOf(result)) mustBe
          Json.parse("""{"code":"SERVICE_UNAVAILABLE","reason":"Service is unavailable"}""".stripMargin)
      }
    }

  }
}
