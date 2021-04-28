/*
 * Copyright 2021 HM Revenue & Customs
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
import org.scalamock.handlers.CallHandler4
import play.api.http.Status._
import play.api.test.FakeRequest
import services.GetEmploymentBenefitsService
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.Future

class GetEmploymentBenefitsControllerSpec extends TestUtils {

  val getEmploymentListService: GetEmploymentBenefitsService = mock[GetEmploymentBenefitsService]
  val getEmploymentListController = new GetEmploymentBenefitsController(getEmploymentListService,authorisedAction, mockControllerComponents)
  val nino :String = "123456789"
  val mtdItID :String = "123123123"
  val taxYear: Int = 1234
  val view = "CUSTOMER"
  val badRequestModel: DesErrorBodyModel = DesErrorBodyModel("INVALID_NINO", "Nino is invalid")
  val notFoundModel: DesErrorBodyModel = DesErrorBodyModel("NOT_FOUND_INCOME_SOURCE", "Can't find income source")
  val serverErrorModel: DesErrorBodyModel = DesErrorBodyModel("SERVER_ERROR", "Internal server error")
  val serviceUnavailableErrorModel: DesErrorBodyModel = DesErrorBodyModel("SERVICE_UNAVAILABLE", "Service is unavailable")
  private val fakeGetRequest = FakeRequest("GET", "/").withHeaders("MTDITID" -> "1234567890")

  def mockGetEmploymentListValid(): CallHandler4[String, Int, String, HeaderCarrier, Future[GetEmploymentBenefitsResponse]] = {
    val customerExampleResponse: GetEmploymentBenefitsResponse = Right(customerExample)
    (getEmploymentListService.getEmploymentBenefits(_: String, _: Int, _:String)(_: HeaderCarrier))
      .expects(*, *, *, *)
      .returning(Future.successful(customerExampleResponse))
  }

  def mockGetEmploymentListBadRequest(): CallHandler4[String, Int, String, HeaderCarrier, Future[GetEmploymentBenefitsResponse]] = {
    val invalidEmploymentList: GetEmploymentBenefitsResponse = Left(DesErrorModel(BAD_REQUEST, badRequestModel))
    (getEmploymentListService.getEmploymentBenefits(_: String, _: Int, _:String)(_: HeaderCarrier))
      .expects(*, *, *, *)
      .returning(Future.successful(invalidEmploymentList))
  }

  def mockGetEmploymentListNotFound(): CallHandler4[String, Int, String, HeaderCarrier, Future[GetEmploymentBenefitsResponse]] = {
    val invalidEmploymentList: GetEmploymentBenefitsResponse = Left(DesErrorModel(NOT_FOUND, notFoundModel))
    (getEmploymentListService.getEmploymentBenefits(_: String, _: Int, _:String)(_: HeaderCarrier))
      .expects(*, *, *, *)
      .returning(Future.successful(invalidEmploymentList))
  }

  def mockGetEmploymentListServerError(): CallHandler4[String, Int, String, HeaderCarrier, Future[GetEmploymentBenefitsResponse]] = {
    val invalidEmploymentList: GetEmploymentBenefitsResponse = Left(DesErrorModel(INTERNAL_SERVER_ERROR, serverErrorModel))
    (getEmploymentListService.getEmploymentBenefits(_: String, _: Int, _:String)(_: HeaderCarrier))
      .expects(*, *, *, *)
      .returning(Future.successful(invalidEmploymentList))
  }

  def mockGetEmploymentListServiceUnavailable(): CallHandler4[String, Int, String, HeaderCarrier, Future[GetEmploymentBenefitsResponse]] = {
    val invalidEmploymentList: GetEmploymentBenefitsResponse = Left(DesErrorModel(SERVICE_UNAVAILABLE, serviceUnavailableErrorModel))
    (getEmploymentListService.getEmploymentBenefits(_: String, _: Int, _:String)(_: HeaderCarrier))
      .expects(*, *, *, *)
      .returning(Future.successful(invalidEmploymentList))
  }

  "calling .getEmploymentBenefits" should {

    "with existing employments" should {

      "return an OK 200 response when called as an individual" in {
        val result = {
          mockAuth()
          mockGetEmploymentListValid()
          getEmploymentListController.getEmploymentBenefits(nino, taxYear, view)(fakeGetRequest)
        }
        status(result) mustBe OK
        bodyOf(result) mustBe ""
      }

      "return an OK 200 response when called as an agent" in {
        val result = {
          mockAuthAsAgent()
          mockGetEmploymentListValid()
          getEmploymentListController.getEmploymentBenefits(nino, taxYear, view)(fakeGetRequest)
        }
        status(result) mustBe OK
        bodyOf(result) mustBe ""
      }
    }

    "without existing employments" should {

      "return an NotFound response when called as an individual" in {
        val result = {
          mockAuth()
          mockGetEmploymentListNotFound()
          getEmploymentListController.getEmploymentBenefits(nino, taxYear, view)(fakeGetRequest)
        }
        status(result) mustBe NOT_FOUND
        bodyOf(result) mustBe ""
      }

      "return an NotFound response when called as an agent" in {
        val result = {
          mockAuthAsAgent()
          mockGetEmploymentListNotFound()
          getEmploymentListController.getEmploymentBenefits(nino, taxYear, view)(fakeGetRequest)
        }
        status(result) mustBe NOT_FOUND
        bodyOf(result) mustBe ""
      }

    }

    "with an invalid NINO" should {

      "return an BadRequest response when called as an individual" in {
        val result = {
          mockAuth()
          mockGetEmploymentListBadRequest()
          getEmploymentListController.getEmploymentBenefits(nino, taxYear, view)(fakeGetRequest)
        }
        status(result) mustBe BAD_REQUEST
        bodyOf(result) mustBe ""
      }

      "return an BadRequest response when called as an agent" in {
        val result = {
          mockAuthAsAgent()
          mockGetEmploymentListBadRequest()
          getEmploymentListController.getEmploymentBenefits(nino, taxYear, view)(fakeGetRequest)
        }
        status(result) mustBe BAD_REQUEST
        bodyOf(result) mustBe ""
      }
    }
    "with an invalid view" should {

      "return an BadRequest response when called as an individual" in {
        val result = {
          mockAuth()
          mockGetEmploymentListBadRequest()
          getEmploymentListController.getEmploymentBenefits(nino, taxYear, "view")(fakeGetRequest)
        }
        status(result) mustBe BAD_REQUEST
        bodyOf(result) mustBe ""
      }
    }

    "with something that causes and internal server error in DES" should {

      "return an BadRequest response when called as an individual" in {
        val result = {
          mockAuth()
          mockGetEmploymentListServerError()
          getEmploymentListController.getEmploymentBenefits(nino, taxYear, view)(fakeGetRequest)
        }
        status(result) mustBe INTERNAL_SERVER_ERROR
        bodyOf(result) mustBe ""
      }

      "return an BadRequest response when called as an agent" in {
        val result = {
          mockAuthAsAgent()
          mockGetEmploymentListServerError()
          getEmploymentListController.getEmploymentBenefits(nino, taxYear, view)(fakeGetRequest)
        }
        status(result) mustBe INTERNAL_SERVER_ERROR
        bodyOf(result) mustBe ""
      }
    }

    "with an unavailable service" should {

      "return an Service_Unavailable response when called as an individual" in {
        val result = {
          mockAuth()
          mockGetEmploymentListServiceUnavailable()
          getEmploymentListController.getEmploymentBenefits(nino, taxYear, view)(fakeGetRequest)
        }
        status(result) mustBe SERVICE_UNAVAILABLE
        bodyOf(result) mustBe ""
      }

      "return an Service_Unavailable response when called as an agent" in {
        val result = {
          mockAuthAsAgent()
          mockGetEmploymentListServiceUnavailable()
          getEmploymentListController.getEmploymentBenefits(nino, taxYear, view)(fakeGetRequest)
        }
        status(result) mustBe SERVICE_UNAVAILABLE
        bodyOf(result) mustBe ""
      }
    }

  }
}
