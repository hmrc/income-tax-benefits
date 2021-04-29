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

package api

import com.github.tomakehurst.wiremock.http.HttpHeader
import helpers.{AuthStub, WiremockSpec}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._
import play.api.libs.json.Json
import utils.DESTaxYearHelper.desTaxYearConverter

class GetEmploymentBenefitsITest extends PlaySpec with WiremockSpec with ScalaFutures with AuthStub {

  trait Setup {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(5, Seconds))
    val successNino: String = "AA123123A"
    val taxYear = 2021
    val agentClientCookie: Map[String, String] = Map("MTDITID" -> "555555555")
    val mtditidHeader = ("mtditid", "555555555")
    val requestHeaders: Seq[HttpHeader] = Seq(new HttpHeader("mtditid", "555555555"))
    val view = "LATEST"
    val id = "a1e8057e-fbbc-47a8-a8b4-78d9f015c934"
    auditStubs()
  }

  val GetEmploymentBenefitsDesResponseBody =
    """
      |{
      |	"submittedOn": "2020-12-12T12:12:12Z",
      |	"dateIgnored": "2020-12-11T12:12:12Z",
      |	"source": "CUSTOMER",
      |	"employment": {
      |		"benefitsInKind": {
      |
      |   }
      |	}
      |}
      |""".stripMargin

  "get employment benefits" when {

    "the user is an individual" must {
      "return 200 and the employment benefits for a user" in new Setup {

        stubGetWithResponseBody(
          url = s"/income-tax/income/employments/$successNino/${desTaxYearConverter(taxYear)}/$id\\?view=$view",
          status = OK,
          response = GetEmploymentBenefitsDesResponseBody
        )

        authorised()
        whenReady(buildClient(s"/income-tax-benefits/income-tax/nino/$successNino/sources/$id")
          .withQueryStringParameters(Seq("taxYear" -> s"$taxYear", "view" -> view): _*)
          .withHttpHeaders(mtditidHeader)
          .get) {
          result =>
            result.status mustBe OK
            Json.parse(result.body) mustBe Json.parse(GetEmploymentBenefitsDesResponseBody)
        }
      }

      "return 404 if a user has no recorded employment benefits" in new Setup {
        val desErrorBody = "{\"code\":\"NO_DATA_FOUND\",\"reason\":\"The remote endpoint has indicated that no data can be found.\"}"

        stubGetWithResponseBody(
          url = s"/income-tax/income/employments/$successNino/${desTaxYearConverter(taxYear)}/$id\\?view=$view",
          status = NOT_FOUND,
          response = desErrorBody
        )

        authorised()

        whenReady(buildClient(s"/income-tax-benefits/income-tax/nino/$successNino/sources/$id")
          .withQueryStringParameters(Seq("taxYear" -> s"$taxYear", "view" -> view): _*)
          .withHttpHeaders(mtditidHeader)
          .get) {
          result =>
            result.status mustBe NOT_FOUND
            result.body mustBe desErrorBody
        }
      }

      "return 422 if a downstream error occurs due to incorrect date range" in new Setup {
        val responseBody = "{\"code\":\"INVALID_DATE_RANGE\",\"reason\":\"The remote endpoint has indicated that date range exceeds CY-4.\"}"
        stubGetWithResponseBody(
          url = s"/income-tax/income/employments/$successNino/${desTaxYearConverter(taxYear)}/$id\\?view=$view",
          status = UNPROCESSABLE_ENTITY,
          response = responseBody
        )

        authorised()

        whenReady(buildClient(s"/income-tax-benefits/income-tax/nino/$successNino/sources/$id")
          .withQueryStringParameters(Seq("taxYear" -> s"$taxYear", "view" -> view): _*)
          .withHttpHeaders(mtditidHeader)
          .get) {
          result =>
            result.status mustBe UNPROCESSABLE_ENTITY
            result.body mustBe "{\"code\":\"INVALID_DATE_RANGE\",\"reason\":\"The remote endpoint has indicated that date range exceeds CY-4.\"}"
        }
      }


      "return 503 if a downstream error occurs" in new Setup {
        val responseBody = "{\"code\":\"SERVICE_UNAVAILABLE\",\"reason\":\"The service is temporarily unavailable\"}"
        stubGetWithResponseBody(
          url = s"/income-tax/income/employments/$successNino/${desTaxYearConverter(taxYear)}/$id\\?view=$view",
          status = SERVICE_UNAVAILABLE,
          response = responseBody
        )

        authorised()

        whenReady(buildClient(s"/income-tax-benefits/income-tax/nino/$successNino/sources/$id")
          .withQueryStringParameters(Seq("taxYear" -> s"$taxYear", "view" -> view): _*)
          .withHttpHeaders(mtditidHeader)
          .get) {
          result =>
            result.status mustBe SERVICE_UNAVAILABLE
            result.body mustBe "{\"code\":\"SERVICE_UNAVAILABLE\",\"reason\":\"The service is temporarily unavailable\"}"
        }
      }

      "return 401 if the user has no HMRC-MTD-IT enrolment" in new Setup {
        unauthorisedOtherEnrolment()

        whenReady(buildClient(s"/income-tax-benefits/income-tax/nino/$successNino/sources/$id")
          .withQueryStringParameters(Seq("taxYear" -> s"$taxYear", "view" -> view): _*)
          .withHttpHeaders(mtditidHeader)
          .get) {
          result =>
            result.status mustBe UNAUTHORIZED
            result.body mustBe ""
        }
      }

      "return 401 if the request has no MTDITID header present" in new Setup {
        whenReady(buildClient(s"/income-tax-benefits/income-tax/nino/$successNino/sources/$id")
          .withQueryStringParameters(Seq("taxYear" -> s"$taxYear", "view" -> view): _*)
          .get) {
          result =>
            result.status mustBe UNAUTHORIZED
            result.body mustBe ""
        }
      }
    }

  }
}