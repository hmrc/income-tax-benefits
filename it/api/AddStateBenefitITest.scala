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

package api

import com.github.tomakehurst.wiremock.http.HttpHeader
import helpers.{AuthStub, WiremockSpec}
import models.{AddStateBenefitRequestModel, DesErrorBodyModel}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._
import play.api.libs.json.Json
import utils.DESTaxYearHelper.desTaxYearConverter

class AddStateBenefitITest extends PlaySpec with WiremockSpec with ScalaFutures with AuthStub {
  trait Setup {
    val timeSpan: Long = 5
    implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(timeSpan, Seconds))
    val nino: String = "AA123123A"
    val taxYear: Int = 2021
    val mtditidHeader: (String, String) = ("mtditid", "555555555")
    val requestHeaders: Seq[HttpHeader] = Seq(new HttpHeader("mtditid", "555555555"))
    val desUrl: String = s"/income-tax/income/state-benefits/$nino/${desTaxYearConverter(taxYear)}/custom"
    val serviceUrl: String = s"/income-tax-benefits/state-benefits/nino/$nino/taxYear/$taxYear"
    auditStubs()
  }

  val requestModel: AddStateBenefitRequestModel = AddStateBenefitRequestModel("statePension", "2020-08-03", Some("2020-12-03"))
  val requestBody: String =
    """
      |{
      |  "benefitType": "statePension",
      |  "startDate": "2020-08-03",
      |  "endDate": "2020-12-03"
      |}
      |""".stripMargin

  val responseBody: String = """{"benefitId": "b1e8057e-fbbc-47a8-a8b4-78d9f015c253"}"""


  "add state benefit" should {

    "return 200 and benefitId on success" in new Setup {
      stubPostWithResponseBody(
        url = desUrl,
        status = OK,
        requestBody = requestBody,
        response = responseBody
      )
      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader)
        .post(Json.toJson(requestModel))) {
        result =>
          result.status mustBe OK
          Json.parse(result.body) mustBe Json.parse(responseBody)
      }
    }

    "return 400 if body payload validation fails" in new Setup {
      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader)
        .post(Json.obj())) {
        result =>
          result.status mustBe BAD_REQUEST
      }
    }

    "return 400 if a there is an invalid taxable entity id (nino)" in new Setup {

      val errorResponseBody: String = Json.toJson(DesErrorBodyModel.invalidTaxableEntityId).toString()
      stubPostWithResponseBody(
        url = desUrl,
        status = BAD_REQUEST,
        response = errorResponseBody,
        requestBody = requestBody
      )

      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader)
        .post(Json.toJson(requestModel))) {
        result =>
          result.status mustBe BAD_REQUEST
          Json.parse(result.body) mustBe Json.obj(
            "code" -> "INVALID_TAXABLE_ENTITY_ID", "reason" -> "Submission has not passed validation. Invalid parameter taxableEntityId.")
      }
    }

    "return 400 if a there is an invalid tax year" in new Setup {

      val errorResponseBody: String = Json.toJson(DesErrorBodyModel.invalidTaxYear).toString()
      stubPostWithResponseBody(
        url = desUrl,
        status = BAD_REQUEST,
        response = errorResponseBody,
        requestBody = requestBody
      )

      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader)
        .post(Json.toJson(requestModel))) {
        result =>
          result.status mustBe BAD_REQUEST
          Json.parse(result.body) mustBe Json.obj(
            "code" -> "INVALID_TAX_YEAR", "reason" -> "Submission has not passed validation. Invalid parameter taxYear.")
      }
    }

    "return 400 if there is an invalid payload" in new Setup {
      val errorResponseBody: String = Json.toJson(DesErrorBodyModel.invalidPayload).toString()
      stubPostWithResponseBody(
        url = desUrl,
        status = BAD_REQUEST,
        response = errorResponseBody,
        requestBody = requestBody
      )
      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader)
        .post(Json.toJson(requestModel))) {
        result =>
          result.status mustBe BAD_REQUEST
          Json.parse(result.body) mustBe Json.obj(
            "code" -> "INVALID_PAYLOAD", "reason" -> "Submission has not passed validation. Invalid payload.")

      }
    }

    "return 422 if the request tax year is invalid" in new Setup {
      val errorResponseBody: String = Json.toJson(DesErrorBodyModel.invalidRequestTaxYear).toString()
      stubPostWithResponseBody(
        url = desUrl,
        status = UNPROCESSABLE_ENTITY,
        response = errorResponseBody,
        requestBody = requestBody
      )
      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader)
        .post(Json.toJson(requestModel))) {
        result =>
          result.status mustBe UNPROCESSABLE_ENTITY
          Json.parse(result.body) mustBe Json.obj(
            "code" -> "INVALID_REQUEST_TAX_YEAR", "reason" -> "The remote endpoint has indicated that tax year provided is invalid.")
      }
    }

    "return 422 if the request is made for a tax year that has not ended" in new Setup {
      val errorResponseBody: String = Json.toJson(DesErrorBodyModel.notSupportedTaxYear).toString()
      stubPostWithResponseBody(
        url = desUrl,
        status = UNPROCESSABLE_ENTITY,
        response = errorResponseBody,
        requestBody = requestBody
      )
      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader)
        .post(Json.toJson(requestModel))) {
        result =>
          result.status mustBe UNPROCESSABLE_ENTITY
          Json.parse(result.body) mustBe Json.obj(
            "code" -> "NOT_SUPPORTED_TAX_YEAR", "reason" -> "The remote endpoint has indicated that submission is provided before the tax year has ended.")
      }
    }

    "return 422 if the start date is after the end date" in new Setup {
      val errorResponseBody: String = Json.toJson(DesErrorBodyModel.invalidStartDate).toString()
      stubPostWithResponseBody(
        url = desUrl,
        status = UNPROCESSABLE_ENTITY,
        response = errorResponseBody,
        requestBody = requestBody
      )
      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader)
        .post(Json.toJson(requestModel))) {
        result =>
          result.status mustBe UNPROCESSABLE_ENTITY
          Json.parse(result.body) mustBe Json.obj(
            "code" -> "INVALID_START_DATE", "reason" -> "The remote endpoint has indicated that start date is after the end of the tax year.")
      }
    }
    "return 422 if cessation date is invalid" in new Setup {
      val errorResponseBody: String = Json.toJson(DesErrorBodyModel.invalidCessationDate).toString()
      stubPostWithResponseBody(
        url = desUrl,
        status = UNPROCESSABLE_ENTITY,
        response = errorResponseBody,
        requestBody = requestBody
      )
      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader)
        .post(Json.toJson(requestModel))) {
        result =>
          result.status mustBe UNPROCESSABLE_ENTITY
          Json.parse(result.body) mustBe Json.obj(
            "code" -> "INVALID_CESSATION_DATE", "reason" -> "The remote endpoint has indicated that cessation date is before the start of the tax year.")
      }
    }

    "return 503 if a downstream error occurs" in new Setup {

      val errorResponseBody: String = Json.toJson(DesErrorBodyModel.serviceUnavailable).toString()

      stubPostWithResponseBody(
        url = desUrl,
        status = SERVICE_UNAVAILABLE,
        response = errorResponseBody,
        requestBody = requestBody
      )

      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader)
        .post(Json.toJson(requestModel))) {
        result =>
          result.status mustBe SERVICE_UNAVAILABLE
          Json.parse(result.body) mustBe Json.obj("code" -> "SERVICE_UNAVAILABLE", "reason" -> "Dependent systems are currently not responding.")
      }
    }

    "return 500 if a downstream error occurs" in new Setup {

      val errorResponseBody: String = Json.toJson(DesErrorBodyModel.serverError).toString()

      stubPostWithResponseBody(
        url = desUrl,
        status = INTERNAL_SERVER_ERROR,
        response = errorResponseBody,
        requestBody = requestBody
      )

      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader)
        .post(Json.toJson(requestModel))) {
        result =>
          result.status mustBe INTERNAL_SERVER_ERROR
          Json.parse(result.body) mustBe Json.obj(
            "code" -> "SERVER_ERROR", "reason" -> "DES is currently experiencing problems that require live service intervention.")
      }
    }

    "return 401 if user is not authorised" when {

      "user has no HMRC-MTD-IT enrolment" in new Setup {
        unauthorisedOtherEnrolment()

        whenReady(buildClient(serviceUrl)
          .withHttpHeaders(mtditidHeader)
          .post(Json.toJson(requestModel))) {
          result =>
            result.status mustBe UNAUTHORIZED
            result.body mustBe ""
        }
      }

      "the request has no MTDITID header present" in new Setup {
        whenReady(buildClient(serviceUrl)
          .post(Json.toJson(requestModel))) {
          result =>
            result.status mustBe UNAUTHORIZED
            result.body mustBe ""
        }
      }
    }
  }
}
