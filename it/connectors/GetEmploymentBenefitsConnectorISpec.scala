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

package connectors

import connectors.getEmploymentBenefitsConnectorSpec.{expectedResponseBody, expectedResponseBodyHMRC, latestWithAllFields}
import helpers.WiremockSpec
import models.{Benefits, DesErrorBodyModel, DesErrorModel, Employment, EmploymentBenefits}
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import utils.DESTaxYearHelper.desTaxYearConverter

class GetEmploymentBenefitsConnectorISpec extends PlaySpec with WiremockSpec {

  lazy val connector: GetEmploymentBenefitsConnector = app.injector.instanceOf[GetEmploymentBenefitsConnector]

  val nino: String = "AA123456A"
  val taxYear: Int = 2022
  val view: String = "LATEST"
  val id = "a1e8057e-fbbc-47a8-a8b4-78d9f015c934"

  val amount: BigDecimal = 100
  val model: EmploymentBenefits = EmploymentBenefits(
    "2020-12-12T12:12:12Z",
    Some("2020-12-12T12:12:12Z"),
    None,
    Some("CUSTOMER"),
    employment = Employment(
      Some(Benefits(
        Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount),
        Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount),
        Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount)
      ))
    ))

  "GetEmploymentBenefitsConnector" should {
    "return a GetEmploymentBenefitsModel" when {
      "nino, employmentId, taxYear & view are present" in {
        val expectedResult = Json.parse(expectedResponseBody).as[EmploymentBenefits]

        stubGetWithResponseBody(s"/income-tax/income/employments/$nino/${desTaxYearConverter(taxYear)}/$id\\?view=$view", OK, expectedResponseBody)

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.getEmploymentBenefits(nino, id, taxYear, view)(hc)).right.get

        result mustBe expectedResult
        expectedResult mustBe model
      }
      ": nino, employmentId, taxYear & view are present for hmrc held" in {
        val expectedResult = Json.parse(expectedResponseBodyHMRC).as[EmploymentBenefits]

        stubGetWithResponseBody(s"/income-tax/income/employments/$nino/${desTaxYearConverter(taxYear)}/$id\\?view=HMRC-HELD", OK, expectedResponseBodyHMRC)

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.getEmploymentBenefits(nino, id, taxYear, "HMRC-HELD")(hc)).right.get

        result mustBe expectedResult
        expectedResult mustBe model.copy(customerAdded = None,source = Some("HMRC-HELD"))
      }
      "all the other data is returned including the benefits" in {
        val expectedResult = Json.parse(latestWithAllFields).as[EmploymentBenefits]

        stubGetWithResponseBody(s"/income-tax/income/employments/$nino/${desTaxYearConverter(taxYear)}/$id\\?view=$view", OK, latestWithAllFields)

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.getEmploymentBenefits(nino, id, taxYear, view)(hc)).right.get

        result mustBe expectedResult
        expectedResult.source mustBe Some("CUSTOMER")
        expectedResult.employment.benefitsInKind.get.employerProvidedProfessionalSubscriptions mustBe Some(84.56)
      }
    }

    "return a Parsing error INTERNAL_SERVER_ERROR response" in {
      val invalidJson = Json.obj(
        "source" -> true
      )

      val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.parsingError)

      stubGetWithResponseBody(s"/income-tax/income/employments/$nino/${desTaxYearConverter(taxYear)}/$id\\?view=$view", OK, invalidJson.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getEmploymentBenefits(nino, id, taxYear, view)(hc))

      result mustBe Left(expectedResult)
    }

    "return a NO_CONTENT" in {
      val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.parsingError)

      stubGetWithResponseBody(s"/income-tax/income/employments/$nino/${desTaxYearConverter(taxYear)}/$id\\?view=$view", NO_CONTENT, "{}")
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getEmploymentBenefits(nino, id, taxYear, view)(hc))

      result mustBe Left(expectedResult)
    }

    "return a Bad Request" in {
      val responseBody = Json.obj(
        "code" -> "INVALID_NINO",
        "reason" -> "Nino is invalid"
      )
      val expectedResult = DesErrorModel(BAD_REQUEST, DesErrorBodyModel("INVALID_NINO", "Nino is invalid"))

      stubGetWithResponseBody(s"/income-tax/income/employments/$nino/${desTaxYearConverter(taxYear)}/$id\\?view=$view", BAD_REQUEST, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getEmploymentBenefits(nino, id, taxYear, view)(hc))

      result mustBe Left(expectedResult)
    }

    "return a Not found" in {
      val responseBody = Json.obj(
        "code" -> "NOT_FOUND_INCOME_SOURCE",
        "reason" -> "Can't find income source"
      )
      val expectedResult = DesErrorModel(NOT_FOUND, DesErrorBodyModel("NOT_FOUND_INCOME_SOURCE", "Can't find income source"))

      stubGetWithResponseBody(s"/income-tax/income/employments/$nino/${desTaxYearConverter(taxYear)}/$id\\?view=$view", NOT_FOUND, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getEmploymentBenefits(nino, id, taxYear, view)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Internal server error" in {
      val responseBody = Json.obj(
        "code" -> "SERVER_ERROR",
        "reason" -> "Internal server error"
      )
      val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel("SERVER_ERROR", "Internal server error"))

      stubGetWithResponseBody(s"/income-tax/income/employments/$nino/${desTaxYearConverter(taxYear)}/$id\\?view=$view", INTERNAL_SERVER_ERROR, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getEmploymentBenefits(nino, id, taxYear, view)(hc))

      result mustBe Left(expectedResult)
    }

    "return a Service Unavailable" in {
      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "Service is unavailable"
      )
      val expectedResult = DesErrorModel(SERVICE_UNAVAILABLE, DesErrorBodyModel("SERVICE_UNAVAILABLE", "Service is unavailable"))

      stubGetWithResponseBody(s"/income-tax/income/employments/$nino/${desTaxYearConverter(taxYear)}/$id\\?view=$view", SERVICE_UNAVAILABLE, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getEmploymentBenefits(nino, id, taxYear, view)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Internal Server Error when DES throws an unexpected result" in {
      val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.parsingError)

      stubGetWithoutResponseBody(s"/income-tax/income/employments/$nino/${desTaxYearConverter(taxYear)}/$id\\?view=$view", NO_CONTENT)
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getEmploymentBenefits(nino, id, taxYear, view)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Internal Server Error when DES throws an unexpected result that is parsable" in {
      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "Service is unavailable"
      )
      val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR,  DesErrorBodyModel("SERVICE_UNAVAILABLE", "Service is unavailable"))

      stubGetWithResponseBody(s"/income-tax/income/employments/$nino/${desTaxYearConverter(taxYear)}/$id\\?view=$view", CONFLICT, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getEmploymentBenefits(nino, id, taxYear, view)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Internal Server Error when DES throws an unexpected result that isn't parsable" in {
      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE"
      )
      val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR,  DesErrorBodyModel.parsingError)

      stubGetWithResponseBody(s"/income-tax/income/employments/$nino/${desTaxYearConverter(taxYear)}/$id\\?view=$view", CONFLICT, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getEmploymentBenefits(nino, id, taxYear, view)(hc))

      result mustBe Left(expectedResult)
    }
  }
}

object getEmploymentBenefitsConnectorSpec {
  val expectedResponseBody: String =
    """
      |{
      |	"submittedOn": "2020-12-12T12:12:12Z",
      |	"customerAdded": "2020-12-12T12:12:12Z",
      |	"source": "CUSTOMER",
      |	"employment": {
      |		"benefitsInKind": {
      |       "accommodation": 100,
      |        "assets": 100,
      |        "assetTransfer": 100,
      |        "beneficialLoan": 100,
      |        "car": 100,
      |        "carFuel": 100,
      |        "educationalServices": 100,
      |        "entertaining": 100,
      |        "expenses": 100,
      |        "medicalInsurance": 100,
      |        "telephone": 100,
      |        "service": 100,
      |        "taxableExpenses": 100,
      |        "van": 100,
      |        "vanFuel": 100,
      |        "mileage": 100,
      |        "nonQualifyingRelocationExpenses": 100,
      |        "nurseryPlaces": 100,
      |        "otherItems": 100,
      |        "paymentsOnEmployeesBehalf": 100,
      |        "personalIncidentalExpenses": 100,
      |        "qualifyingRelocationExpenses": 100,
      |        "employerProvidedProfessionalSubscriptions": 100,
      |        "employerProvidedServices": 100,
      |        "incomeTaxPaidByDirector": 100,
      |        "travelAndSubsistence": 100,
      |        "vouchersAndCreditCards": 100,
      |        "nonCash": 100
      |  }
      |	}
      |}
      |""".stripMargin

  val expectedResponseBodyHMRC: String =
    """
      |{
      |	"submittedOn": "2020-12-12T12:12:12Z",
      |	"source": "HMRC-HELD",
      |	"employment": {
      |		"benefitsInKind": {
      |       "accommodation": 100,
      |        "assets": 100,
      |        "assetTransfer": 100,
      |        "beneficialLoan": 100,
      |        "car": 100,
      |        "carFuel": 100,
      |        "educationalServices": 100,
      |        "entertaining": 100,
      |        "expenses": 100,
      |        "medicalInsurance": 100,
      |        "telephone": 100,
      |        "service": 100,
      |        "taxableExpenses": 100,
      |        "van": 100,
      |        "vanFuel": 100,
      |        "mileage": 100,
      |        "nonQualifyingRelocationExpenses": 100,
      |        "nurseryPlaces": 100,
      |        "otherItems": 100,
      |        "paymentsOnEmployeesBehalf": 100,
      |        "personalIncidentalExpenses": 100,
      |        "qualifyingRelocationExpenses": 100,
      |        "employerProvidedProfessionalSubscriptions": 100,
      |        "employerProvidedServices": 100,
      |        "incomeTaxPaidByDirector": 100,
      |        "travelAndSubsistence": 100,
      |        "vouchersAndCreditCards": 100,
      |        "nonCash": 100
      |  }
      |	}
      |}
      |""".stripMargin

  val latestWithAllFields: String =
    """{
      |  "submittedOn": "2020-01-04T05:01:01Z",
      |  "source": "CUSTOMER",
      |  "employment": {
      |    "payrollId": "123456789999",
      |    "companyDirector": false,
      |    "closeCompany": true,
      |    "directorshipCeasedDate": "2020-02-12",
      |    "startDate": "2019-04-21",
      |    "cessationDate": "2020-03-11",
      |    "occPen": false,
      |    "disguisedRemuneration": false,
      |    "employer": {
      |      "employerRef": "223/AB12399",
      |      "employerName": "maggie"
      |    },
      |    "pay": {
      |      "taxablePayToDate": 34234.15,
      |      "totalTaxToDate": 6782.92,
      |      "tipsAndOtherPayments": 67676,
      |      "payFrequency": "CALENDAR MONTHLY",
      |      "paymentDate": "2020-04-23",
      |      "taxWeekNo": 32
      |    },
      |    "deductions": {
      |      "studentLoans": {
      |        "uglDeductionAmount": 13343.45,
      |        "pglDeductionAmount": 24242.56
      |      }
      |    },
      |    "benefitsInKind": {
      |      "accommodation": 455.67,
      |      "assets": 435.54,
      |      "assetTransfer": 24.58,
      |      "beneficialLoan": 33.89,
      |      "car": 3434.78,
      |      "carFuel": 34.56,
      |      "educationalServices": 445.67,
      |      "entertaining": 434.45,
      |      "expenses": 3444.32,
      |      "medicalInsurance": 4542.47,
      |      "telephone": 243.43,
      |      "service": 45.67,
      |      "taxableExpenses": 24.56,
      |      "van": 56.29,
      |      "vanFuel": 14.56,
      |      "mileage": 34.23,
      |      "nonQualifyingRelocationExpenses": 54.62,
      |      "nurseryPlaces": 84.29,
      |      "otherItems": 67.67,
      |      "paymentsOnEmployeesBehalf": 67.23,
      |      "personalIncidentalExpenses": 74.29,
      |      "qualifyingRelocationExpenses": 78.24,
      |      "employerProvidedProfessionalSubscriptions": 84.56,
      |      "employerProvidedServices": 56.34,
      |      "incomeTaxPaidByDirector": 67.34,
      |      "travelAndSubsistence": 56.89,
      |      "vouchersAndCreditCards": 34.90,
      |      "nonCash": 23.89
      |    }
      |  }
      |}
      |""".stripMargin
}