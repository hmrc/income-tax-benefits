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
import models.{DesErrorBodyModel, DesErrorModel}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import utils.DESTaxYearHelper.desTaxYearConverter

class UnignoreStateBenefitITest extends PlaySpec with WiremockSpec with ScalaFutures with AuthStub {

  trait Setup {
    val timeSpan: Long = 5
    implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(timeSpan, Seconds))
    val nino: String = "AA123123A"
    val taxYear: Int = 2021
    val benefitId: String = "a111111a-abcd-111a-123a-11a1a111a1"
    val mtditidHeader: (String, String) = ("mtditid", "555555555")
    val requestHeaders: Seq[HttpHeader] = Seq(new HttpHeader("mtditid", "555555555"))
    val desUrl: String = s"/income-tax/state-benefits/$nino/${desTaxYearConverter(taxYear)}/ignore/$benefitId"
    val serviceUrl: String = s"/income-tax-benefits/state-benefits/nino/$nino/taxYear/$taxYear/ignore/benefitId/$benefitId"
    auditStubs()
  }

  "unignore state benefit" should {

    "return a 204 No Content response on success" in new Setup {
      stubDeleteWithoutResponseBody(desUrl, NO_CONTENT)
      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader)
        .delete) {
        result =>
          result.status mustBe NO_CONTENT
      }
    }

    "return a 403 if a downstream Forbidden error occurs" in new Setup {
      val errorResponseBody: String = Json.toJson(DesErrorBodyModel(
        "CUSTOMER_ADDED", "The remote endpoint has indicated you Cannot un-ignore customer added State Benefit."
      )).toString()

      val expectedResult: JsValue =  DesErrorModel(FORBIDDEN, DesErrorBodyModel.forbiddenUnignore).toJson

      stubDeleteWithResponseBody(desUrl, FORBIDDEN, errorResponseBody)
      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader)
        .delete()) {
        result =>
          result.status mustBe FORBIDDEN
          Json.parse(result.body) mustBe expectedResult
      }
    }

    "return a 422 if a downstream Unprocessable entity error occurs" in new Setup {
      val errorResponseBody: String = Json.toJson(DesErrorBodyModel(
        "BEFORE_TAX_YEAR_ENDED", "Submission is provided before the tax year has ended."
      )).toString()

      val expectedResult: JsValue =  DesErrorModel(UNPROCESSABLE_ENTITY, DesErrorBodyModel.beforeTaxYear).toJson

      stubDeleteWithResponseBody(desUrl, UNPROCESSABLE_ENTITY, errorResponseBody)
      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader)
        .delete()) {
        result =>
          result.status mustBe UNPROCESSABLE_ENTITY
          Json.parse(result.body) mustBe expectedResult
      }
    }

    "return a 400 if a downstream Bad Request error occurs" in new Setup {
      val errorResponseBody: String = Json.toJson(DesErrorBodyModel(
        "INVALID_BENEFIT_ID", "Submission has not passed validation. Invalid parameter benefitId."
      )).toString()

      val expectedResult: JsValue =  DesErrorModel(BAD_REQUEST, DesErrorBodyModel.invalidBenefitId).toJson

      stubDeleteWithResponseBody(desUrl, BAD_REQUEST, errorResponseBody)
      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader)
        .delete()) {
        result =>
          result.status mustBe BAD_REQUEST
          Json.parse(result.body) mustBe expectedResult


      }
    }

    "return a 404 if no data is found to delete" in new Setup {
      val errorResponseBody: String = Json.toJson(DesErrorBodyModel(
        "NO_DATA_FOUND", "The remote endpoint has indicated that the requested resource could not be found."
      )).toString()

      val expectedResult: JsValue =  DesErrorModel(NOT_FOUND, DesErrorBodyModel.noDataFound).toJson

      stubDeleteWithResponseBody(desUrl, NOT_FOUND, errorResponseBody)
      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader)
        .delete()) {
        result =>
          result.status mustBe NOT_FOUND
          Json.parse(result.body) mustBe expectedResult
      }
    }

    "return 500 if a downstream error occurs" in new Setup {

      val errorResponseBody: String = Json.toJson(DesErrorBodyModel(
        "SERVER_ERROR", "DES is currently experiencing problems that require live service intervention.")).toString()

      stubDeleteWithResponseBody(desUrl, INTERNAL_SERVER_ERROR, errorResponseBody)
      authorised()

      val expectedResult: JsValue =  DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.serverError).toJson

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader)
        .delete()) {
        result =>
          result.status mustBe INTERNAL_SERVER_ERROR
          Json.parse(result.body) mustBe expectedResult
      }
    }

    "return 503 if a downstream error occurs" in new Setup {

      val errorResponseBody: String = Json.toJson(DesErrorBodyModel(
        "SERVICE_UNAVAILABLE", "Dependent systems are currently not responding.")).toString()

      stubDeleteWithResponseBody(desUrl, SERVICE_UNAVAILABLE, errorResponseBody)
      val expectedResult: JsValue =  DesErrorModel(SERVICE_UNAVAILABLE, DesErrorBodyModel.serviceUnavailable).toJson

      authorised()

      whenReady(buildClient(serviceUrl)
        .withHttpHeaders(mtditidHeader)
        .delete()) {
        result =>
          result.status mustBe SERVICE_UNAVAILABLE
          Json.parse(result.body) mustBe expectedResult
      }
    }

    "return 401 if user is not authorised" when {

      "user has no HMRC-MTD-IT enrolment" in new Setup {
        unauthorisedOtherEnrolment()

        whenReady(buildClient(serviceUrl)
          .withHttpHeaders(mtditidHeader)
          .delete()) {
          result =>
            result.status mustBe UNAUTHORIZED
            result.body mustBe ""
        }
      }

      "the request has no MTDITID header present" in new Setup {
        whenReady(buildClient(serviceUrl)
          .delete()) {
          result =>
            result.status mustBe UNAUTHORIZED
            result.body mustBe ""
        }
      }
    }
  }
}
