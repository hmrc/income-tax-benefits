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

package test.api

import com.github.tomakehurst.wiremock.http.HttpHeader
import models.{DesErrorBodyModel, IgnoreStateBenefit}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.PlaySpec
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import test.helpers.{AuthStub, WiremockSpec}
import utils.DESTaxYearHelper.desTaxYearConverter

class IgnoreStateBenefitITest extends PlaySpec with WiremockSpec with ScalaFutures with AuthStub {

  val fullIgnoreStateBenefit: IgnoreStateBenefit = IgnoreStateBenefit(true)

  val fullCreateUpdateStateBenefitJson: JsValue = Json.parse("""{"ignoreBenefit": true}""")

  trait Setup {
    val timeSpan: Long = 5
    implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(timeSpan, Seconds))
    val nino: String = "AA123123A"
    val taxYear: Int = 2021
    val mtditidHeader: (String, String) = ("mtditid", "555555555")
    val authorization: (String, String) = HeaderNames.AUTHORIZATION -> "mock-bearer-token"
    val benefitId: String = "a111111a-abcd-111a-123a-11a1a111a1"
    val requestHeaders: Seq[HttpHeader] = Seq(new HttpHeader("mtditid", "555555555"))
    val desUrl = s"/income-tax/income/state-benefits/$nino/${desTaxYearConverter(taxYear)}/ignore/$benefitId"
    val serviceUrl: String = s"/income-tax-benefits/state-benefits/nino/$nino/taxYear/$taxYear/benefitId/$benefitId/ignoreBenefit/true"

    auditStubs()
  }

  "ignore state benefit" should {
    "return a No content Success response" in new Setup {

      stubPutWithoutResponseBody(desUrl, Json.toJson(fullIgnoreStateBenefit).toString(), NO_CONTENT)

      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader, authorization)
        .put(fullCreateUpdateStateBenefitJson)) {
        result =>
          result.status mustBe NO_CONTENT

      }
    }

    "return 400 if a downstream invalid taxable entity request error occurs" in new Setup {

      val errorResponseBody: String = Json.toJson(DesErrorBodyModel(
        "INVALID_TAXABLE_ENTITY_ID", "Submission has not passed validation. Invalid parameter taxableEntityId.")).toString()

      stubPutWithResponseBody(desUrl, Json.toJson(fullIgnoreStateBenefit).toString(), errorResponseBody, BAD_REQUEST)

      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader, authorization)
        .put(fullCreateUpdateStateBenefitJson)) {
        result =>
          result.status mustBe BAD_REQUEST
          result.body mustBe errorResponseBody
      }
    }

    "return 400 if a downstream invalid tax year request error occurs" in new Setup {

      val errorResponseBody: String = Json.toJson(DesErrorBodyModel(
        "INVALID_TAX_YEAR", "Submission has not passed validation. Invalid parameter taxYear.")).toString()

      stubPutWithResponseBody(desUrl, Json.toJson(fullIgnoreStateBenefit).toString(), errorResponseBody, BAD_REQUEST)

      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader, authorization)
        .put(fullCreateUpdateStateBenefitJson)) {
        result =>
          result.status mustBe BAD_REQUEST
          result.body mustBe errorResponseBody
      }
    }

    "return 400 if a downstream invalid benefit id request error occurs" in new Setup {

      val errorResponseBody: String = Json.toJson(DesErrorBodyModel(
        "INVALID_BENEFIT_ID", "Submission has not passed validation. Invalid parameter benefitId.")).toString()

      stubPutWithResponseBody(desUrl, Json.toJson(fullIgnoreStateBenefit).toString(), errorResponseBody, BAD_REQUEST)

      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader, authorization)
        .put(fullCreateUpdateStateBenefitJson)) {
        result =>
          result.status mustBe BAD_REQUEST
          result.body mustBe errorResponseBody
      }
    }

    "return 400 if a downstream invalid payload request error occurs" in new Setup {

      val errorResponseBody: String = Json.toJson(DesErrorBodyModel(
        "INVALID_PAYLOAD", "Submission has not passed validation. Invalid payload.")).toString()

      stubPutWithResponseBody(desUrl, Json.toJson(fullIgnoreStateBenefit).toString(), errorResponseBody, BAD_REQUEST)

      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader, authorization)
        .put(fullCreateUpdateStateBenefitJson)) {
        result =>
          result.status mustBe BAD_REQUEST
          result.body mustBe errorResponseBody
      }
    }

    "return 400 if a downstream invalid correlation id request error occurs" in new Setup {

      val errorResponseBody: String = Json.toJson(DesErrorBodyModel(
        "INVALID_CORRELATIONID", "Submission has not passed validation. Invalid Header parameter CorrelationId.")).toString()

      stubPutWithResponseBody(desUrl, Json.toJson(fullIgnoreStateBenefit).toString(), errorResponseBody, BAD_REQUEST)

      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader, authorization)
        .put(fullCreateUpdateStateBenefitJson)) {
        result =>
          result.status mustBe BAD_REQUEST
          result.body mustBe errorResponseBody
      }
    }

    "return 422 if a downstream not supported request error occurs" in new Setup {

      val errorResponseBody: String = Json.toJson(DesErrorBodyModel(
        "NOT_SUPPORTED_TAX_YEAR", "The remote endpoint has indicated that submission is provided before the tax year has ended.")).toString()

      stubPutWithResponseBody(desUrl, Json.toJson(fullIgnoreStateBenefit).toString(), errorResponseBody, UNPROCESSABLE_ENTITY)

      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader, authorization)
        .put(fullCreateUpdateStateBenefitJson)) {
        result =>
          result.status mustBe UNPROCESSABLE_ENTITY
          result.body mustBe errorResponseBody
      }
    }

    "return 403 if a downstream not forbidden request error occurs" in new Setup {

      val errorResponseBody: String = Json.toJson(DesErrorBodyModel(
        "IGNORE_FORBIDDEN", "The remote endpoint has indicated that HMRC held State Benefit cannot be ignored.")).toString()

      stubPutWithResponseBody(desUrl, Json.toJson(fullIgnoreStateBenefit).toString(), errorResponseBody, FORBIDDEN)

      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader, authorization)
        .put(fullCreateUpdateStateBenefitJson)) {
        result =>
          result.status mustBe FORBIDDEN
          result.body mustBe errorResponseBody
      }
    }

    "return 500 if an unexpected error is returned from DES user" in new Setup {

      val errorResponseBody: String = Json.toJson(DesErrorBodyModel.parsingError).toString()

      stubPutWithResponseBody(desUrl, Json.toJson(fullIgnoreStateBenefit).toString(), errorResponseBody, NOT_FOUND)

      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader, authorization)
        .put(fullCreateUpdateStateBenefitJson)) {
        result =>
          result.status mustBe INTERNAL_SERVER_ERROR
          result.body mustBe errorResponseBody
      }
    }

    "return 503 if a downstream service unavailable error occurs" in new Setup {

      val errorResponseBody: String = Json.toJson(DesErrorBodyModel(
        "SERVICE_UNAVAILABLE", "Dependent systems are currently not responding.")).toString()

      stubPutWithResponseBody(desUrl, Json.toJson(fullIgnoreStateBenefit).toString(), errorResponseBody, SERVICE_UNAVAILABLE)

      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader, authorization)
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

      stubPutWithResponseBody(desUrl, Json.toJson(fullIgnoreStateBenefit).toString(), errorResponseBody, INTERNAL_SERVER_ERROR)

      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader, authorization)
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
        .withHttpHeaders(mtditidHeader, authorization)
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
