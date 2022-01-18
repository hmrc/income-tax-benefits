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
import models.{CreateUpdateOverrideStateBenefit, DesErrorBodyModel}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import utils.DESTaxYearHelper.desTaxYearConverter

class CreateUpdateStateBenefitOverrideITest extends PlaySpec with WiremockSpec with ScalaFutures with AuthStub {

  val fullCreateUpdateStateBenefitData: CreateUpdateOverrideStateBenefit = CreateUpdateOverrideStateBenefit(
    amount = 21.22, taxPaid = Some(0.50))

  val fullCreateUpdateStateBenefitJson: JsValue = Json.parse(
    """{
      |	"amount": 21.22,
      |	"taxPaid": 0.50
      |}""".stripMargin)

  trait Setup {
    val timeSpan: Long = 5
    implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(timeSpan, Seconds))
    val nino: String = "AA123123A"
    val taxYear: Int = 2021
    val mtditidHeader: (String, String) = ("mtditid", "555555555")
    val benefitId: String = "a111111a-abcd-111a-123a-11a1a111a1"
    val requestHeaders: Seq[HttpHeader] = Seq(new HttpHeader("mtditid", "555555555"))
    val desUrl = s"/income-tax/income/state-benefits/$nino/${desTaxYearConverter(taxYear)}/$benefitId"
    val serviceUrl: String = s"/income-tax-benefits/state-benefits-override/nino/$nino/taxYear/$taxYear/benefitId/$benefitId"

    auditStubs()
  }

  "create or update state benefit" when {
    "return a No content Success response" in new Setup {

      stubPutWithoutResponseBody(desUrl, Json.toJson(fullCreateUpdateStateBenefitData).toString(), NO_CONTENT)

      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader)
        .put(fullCreateUpdateStateBenefitJson)) {
        result =>
          result.status mustBe NO_CONTENT

      }
    }

    "return 400 if the body payload validation fails" in new Setup {

      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader)
        .put(Json.obj())) {
        result =>
          result.status mustBe BAD_REQUEST
      }
    }

    "return 400 if a downstream bad request error occurs" in new Setup {

      val errorResponseBody: String = Json.toJson(DesErrorBodyModel(
        "INVALID_TAXABLE_ENTITY_ID", "Submission has not passed validation. Invalid parameter taxableEntityId.")).toString()

      stubPutWithResponseBody(desUrl, Json.toJson(fullCreateUpdateStateBenefitData).toString(), errorResponseBody, BAD_REQUEST)

      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader)
        .put(fullCreateUpdateStateBenefitJson)) {
        result =>
          result.status mustBe BAD_REQUEST
          result.body mustBe errorResponseBody
      }
    }

    "return 422 if a downstream invalid request error occurs" in new Setup {

      val errorResponseBody: String = Json.toJson(DesErrorBodyModel(
        "INVALID_REQUEST_BEFORE_TAX_YEAR", "The remote endpoint has indicated that submission is provided before the tax year has ended.")).toString()

      stubPutWithResponseBody(desUrl, Json.toJson(fullCreateUpdateStateBenefitData).toString(), errorResponseBody, UNPROCESSABLE_ENTITY)

      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader)
        .put(fullCreateUpdateStateBenefitJson)) {
        result =>
          result.status mustBe UNPROCESSABLE_ENTITY
          result.body mustBe errorResponseBody
      }
    }

    "return 500 if an unexpected error is returned from DES user" in new Setup {

      // e,g, 404 not found is not expected as create or update will create if not found
      val errorResponseBody: String = Json.toJson(DesErrorBodyModel.parsingError).toString()

      stubPutWithResponseBody(desUrl, Json.toJson(fullCreateUpdateStateBenefitData).toString(), errorResponseBody, NOT_FOUND)

      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader)
        .put(fullCreateUpdateStateBenefitJson)) {
        result =>
          result.status mustBe INTERNAL_SERVER_ERROR
          result.body mustBe errorResponseBody
      }
    }

    "return 503 if a downstream service unavailable error occurs" in new Setup {

      val errorResponseBody: String = Json.toJson(DesErrorBodyModel(
        "SERVICE_UNAVAILABLE", "Dependent systems are currently not responding.")).toString()

      stubPutWithResponseBody(desUrl, Json.toJson(fullCreateUpdateStateBenefitData).toString(), errorResponseBody, SERVICE_UNAVAILABLE)

      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader)
        .put(fullCreateUpdateStateBenefitJson)) {
        result =>
          result.status mustBe SERVICE_UNAVAILABLE
          result.body mustBe errorResponseBody
          Json.parse(result.body) mustBe Json.obj("code" -> "SERVICE_UNAVAILABLE", "reason" -> "Dependent systems are currently not responding.")
      }
    }

    "return 500 if a downstream internal server error occurs" in new Setup {

      val errorResponseBody: String = Json.toJson(DesErrorBodyModel(
        "SERVER_ERROR", "DES is currently experiencing problems that require live service intervention.")).toString()

      stubPutWithResponseBody(desUrl, Json.toJson(fullCreateUpdateStateBenefitData).toString(), errorResponseBody, INTERNAL_SERVER_ERROR)

      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader)
        .put(fullCreateUpdateStateBenefitJson)) {
        result =>
          result.status mustBe INTERNAL_SERVER_ERROR
          result.body mustBe errorResponseBody
          Json.parse(result.body) mustBe Json.obj(
            "code" -> "SERVER_ERROR", "reason" -> "DES is currently experiencing problems that require live service intervention.")
      }
    }

    "return 401 if the user has no HMRC-MTD-IT enrolment" in new Setup {
      unauthorisedOtherEnrolment()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader)
        .put(fullCreateUpdateStateBenefitJson)) {
        result =>
          result.status mustBe UNAUTHORIZED
          result.body mustBe ""
      }
    }

    "return 401 if the request has no MTDITID header present" in new Setup {
      whenReady(buildClient(serviceUrl)
        .put(fullCreateUpdateStateBenefitJson)) {
        result =>
          result.status mustBe UNAUTHORIZED
          result.body mustBe ""
      }
    }


  }

}
