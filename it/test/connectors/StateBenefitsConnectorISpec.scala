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

package test.connectors

import com.github.tomakehurst.wiremock.http.HttpHeader
import config.{AppConfig, BackendAppConfig}
import connectors.StateBenefitsConnector
import StateBenefitsConnectorISpec.expectedResponseBody
import models.{CreateUpdateOverrideStateBenefit, _}
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.http.Status._
import play.api.libs.json.Json
import test.helpers.WiremockSpec
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpClient, SessionId}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.DESTaxYearHelper.desTaxYearConverter

class StateBenefitsConnectorISpec extends PlaySpec with WiremockSpec {

  lazy val connector: StateBenefitsConnector = app.injector.instanceOf[StateBenefitsConnector]
  lazy val httpClient: HttpClient = app.injector.instanceOf[HttpClient]

  def appConfig(desHost: String): AppConfig = new BackendAppConfig(app.injector.instanceOf[Configuration], app.injector.instanceOf[ServicesConfig]) {
    override val desBaseUrl: String = s"http://$desHost:$wireMockPort"
  }

  val nino: String = "AA123123A"
  val taxYear: Int = 2021
  val benefitId: String = "a111111a-abcd-111a-123a-11a1a111a1"

  lazy val headersSentToDes = Seq(
    new HttpHeader(HeaderNames.authorisation, "Bearer secret"),
    new HttpHeader(HeaderNames.xSessionId, "sessionIdValue")
  )

  lazy val internalHost = "localhost"
  lazy val externalHost = "127.0.0.1"

  ".deleteOverrideStateBenefit" should {

    val deleteOverrideUrl: String = s"/income-tax/income/state-benefits/$nino/${desTaxYearConverter(taxYear)}/$benefitId"

    "include internal headers" when {

      "the host for DES is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new StateBenefitsConnector(httpClient, appConfig(internalHost))

        stubDeleteWithoutResponseBody(deleteOverrideUrl,
          NO_CONTENT, headersSentToDes)

        val result = await(connector.deleteOverrideStateBenefit(nino, taxYear, benefitId)(hc))

        result mustBe Right(())
      }

      "the host for DES is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new StateBenefitsConnector(httpClient, appConfig(externalHost))

        stubDeleteWithoutResponseBody(deleteOverrideUrl,
          NO_CONTENT, headersSentToDes)

        val result = await(connector.deleteOverrideStateBenefit(nino, taxYear, benefitId)(hc))

        result mustBe Right(())
      }
    }

    "return a Right on success" in {
      stubDeleteWithoutResponseBody(deleteOverrideUrl, NO_CONTENT)

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.deleteOverrideStateBenefit(nino, taxYear, benefitId)(hc))

      result mustBe Right(())
    }

    "handle a Left error" when {

      val desErrorBodyModel = DesErrorBodyModel("DES_CODE", "DES_REASON")

      Seq(INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE, NOT_FOUND, BAD_REQUEST).foreach { errorStatus =>

        s"DES returns expected error $errorStatus" in {
          val desError = DesErrorModel(errorStatus, desErrorBodyModel)
          implicit val hc: HeaderCarrier = HeaderCarrier()

          stubDeleteWithResponseBody(deleteOverrideUrl, errorStatus, desError.toJson.toString())

          val result = await(connector.deleteOverrideStateBenefit(nino, taxYear, benefitId)(hc))

          result mustBe Left(desError)
        }
      }

      "DES returns a non parsable response" in {
        val errorResponseBody = "a non parsable body"
        val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.parsingError)

        stubDeleteWithResponseBody(deleteOverrideUrl, INTERNAL_SERVER_ERROR, errorResponseBody)
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.deleteOverrideStateBenefit(nino, taxYear, benefitId)(hc))

        result mustBe Left(expectedResult)
      }

      "DES returns an unexpected http response that is parsable" in {

        val errorResponseBody = Json.obj(
          "code" -> "SERVER_ERROR",
          "reason" -> "DES is currently experiencing problems that require live service intervention."
        )

        val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel(
          "SERVER_ERROR", "DES is currently experiencing problems that require live service intervention."))

        stubDeleteWithResponseBody(deleteOverrideUrl, BAD_GATEWAY, errorResponseBody.toString())
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.deleteOverrideStateBenefit(nino, taxYear, benefitId)(hc))

        result mustBe Left(expectedResult)

      }
    }
  }

  ".getStateBenefits" should {

    val desUrl = s"/income-tax/income/state-benefits/$nino/${desTaxYearConverter(taxYear)}\\?benefitId=$benefitId"

    "include internal headers" when {

      "the host for DES is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new StateBenefitsConnector(httpClient, appConfig(internalHost))
        val expectedResult = Json.parse(expectedResponseBody).as[GetStateBenefitsModel]

        stubGetWithResponseBody(desUrl, OK, expectedResponseBody, headersSentToDes)

        val result = await(connector.getStateBenefits(nino, taxYear, Some(benefitId))(hc))

        result mustBe Right(expectedResult)
      }

      "the host for DES is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new StateBenefitsConnector(httpClient, appConfig(externalHost))
        val expectedResult = Json.parse(expectedResponseBody).as[GetStateBenefitsModel]

        stubGetWithResponseBody(desUrl, OK, expectedResponseBody, headersSentToDes)

        val result = await(connector.getStateBenefits(nino, taxYear, Some(benefitId))(hc))

        result mustBe Right(expectedResult)
      }
    }

    "return a GetStateBenefitsModel when nino and taxYear are present" in {

      val expectedResult = Json.parse(expectedResponseBody).as[GetStateBenefitsModel]
      stubGetWithResponseBody(desUrl, OK, expectedResponseBody)

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getStateBenefits(nino, taxYear, Some(benefitId))(hc))

      result mustBe Right(expectedResult)
    }

    "return a Parsing error INTERNAL_SERVER_ERROR response" in {
      lazy val invalidJson = Json.obj("stateBenefits" -> true)

      val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.parsingError)

      stubGetWithResponseBody(desUrl, OK, invalidJson.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getStateBenefits(nino, taxYear, Some(benefitId))(hc))

      result mustBe Left(expectedResult)
    }

    "return a SERVICE_UNAVAILABLE" in {
      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "Dependent systems are currently not responding.")
      val expectedResult = DesErrorModel(SERVICE_UNAVAILABLE, DesErrorBodyModel("SERVICE_UNAVAILABLE", "Dependent systems are currently not responding."))

      stubGetWithResponseBody(desUrl, SERVICE_UNAVAILABLE, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getStateBenefits(nino, taxYear, Some(benefitId))(hc))

      result mustBe Left(expectedResult)
    }

    "return a BAD_REQUEST" in {
      val responseBody = Json.obj(
        "code" -> "INVALID_NINO",
        "reason" -> "Nino is invalid"
      )
      val expectedResult = DesErrorModel(BAD_REQUEST, DesErrorBodyModel("INVALID_NINO", "Nino is invalid"))

      stubGetWithResponseBody(desUrl, BAD_REQUEST, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getStateBenefits(nino, taxYear, Some(benefitId))(hc))

      result mustBe Left(expectedResult)
    }

    "return a NOT_FOUND" in {
      val responseBody = Json.obj(
        "code" -> "NOT_FOUND",
        "reason" -> "The remote endpoint has indicated that no data can be found."
      )
      val expectedResult = DesErrorModel(NOT_FOUND, DesErrorBodyModel("NOT_FOUND", "The remote endpoint has indicated that no data can be found."))

      stubGetWithResponseBody(desUrl, NOT_FOUND, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getStateBenefits(nino, taxYear, Some(benefitId))(hc))

      result mustBe Left(expectedResult)
    }

    "return an INTERNAL_SERVER_ERROR" in {
      val responseBody = Json.obj(
        "code" -> "SERVER_ERROR",
        "reason" -> "DES is currently experiencing problems that require live service intervention."
      )
      val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR,
        DesErrorBodyModel("SERVER_ERROR", "DES is currently experiencing problems that require live service intervention."))

      stubGetWithResponseBody(desUrl, INTERNAL_SERVER_ERROR, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getStateBenefits(nino, taxYear, Some(benefitId))(hc))

      result mustBe Left(expectedResult)
    }

    "return a INTERNAL_SERVER_ERROR  when DES throws an unexpected result" in {
      val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.parsingError)

      stubGetWithoutResponseBody(desUrl, NO_CONTENT)
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getStateBenefits(nino, taxYear, Some(benefitId))(hc))

      result mustBe Left(expectedResult)
    }
  }

  ".deleteStateBenefitEndOfYear" should {

    val deleteUrl: String = s"/income-tax/income/state-benefits/$nino/${desTaxYearConverter(taxYear)}/custom/$benefitId"

    "include internal headers" when {

      "the host for DES is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new StateBenefitsConnector(httpClient, appConfig(internalHost))

        stubDeleteWithoutResponseBody(deleteUrl,
          NO_CONTENT, headersSentToDes)

        val result = await(connector.deleteStateBenefitEndOfYear(nino, taxYear, benefitId)(hc))

        result mustBe Right(())
      }

      "the host for DES is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new StateBenefitsConnector(httpClient, appConfig(externalHost))

        stubDeleteWithoutResponseBody(deleteUrl,
          NO_CONTENT, headersSentToDes)

        val result = await(connector.deleteStateBenefitEndOfYear(nino, taxYear, benefitId)(hc))

        result mustBe Right(())
      }
    }

    "return a Right on success" in {
      stubDeleteWithoutResponseBody(deleteUrl, NO_CONTENT)

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.deleteStateBenefitEndOfYear(nino, taxYear, benefitId)(hc))

      result mustBe Right(())
    }

    "handle a Left error" when {

      val desErrorBodyModel = DesErrorBodyModel("DES_CODE", "DES_REASON")

      Seq(INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE, NOT_FOUND, BAD_REQUEST, FORBIDDEN).foreach { errorStatus =>

        s"DES returns expected error $errorStatus" in {
          val desError = DesErrorModel(errorStatus, desErrorBodyModel)
          implicit val hc: HeaderCarrier = HeaderCarrier()

          stubDeleteWithResponseBody(deleteUrl, errorStatus, desError.toJson.toString())

          val result = await(connector.deleteStateBenefitEndOfYear(nino, taxYear, benefitId)(hc))

          result mustBe Left(desError)
        }
      }

      "DES returns a non parsable response" in {
        val errorResponseBody = "a non parsable body"
        val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.parsingError)

        stubDeleteWithResponseBody(deleteUrl, INTERNAL_SERVER_ERROR, errorResponseBody)
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.deleteStateBenefitEndOfYear(nino, taxYear, benefitId)(hc))

        result mustBe Left(expectedResult)
      }

      "DES returns an unexpected http response that is parsable" in {


        val errorResponseBody = Json.obj(
          "code" -> "SERVER_ERROR",
          "reason" -> "DES is currently experiencing problems that require live service intervention."
        )

        val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel(
          "SERVER_ERROR", "DES is currently experiencing problems that require live service intervention."))

        stubDeleteWithResponseBody(deleteUrl, BAD_GATEWAY, errorResponseBody.toString())
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.deleteStateBenefitEndOfYear(nino, taxYear, benefitId)(hc))

        result mustBe Left(expectedResult)

      }
    }
  }

  ".createUpdateOverrideStateBenefit" should {

    val createUpdateDesUrl: String = s"/income-tax/income/state-benefits/$nino/${desTaxYearConverter(taxYear)}/$benefitId"

    val createUpdateOverrideStateBenefit: CreateUpdateOverrideStateBenefit = CreateUpdateOverrideStateBenefit(
        amount = 21.22, taxPaid = Some(0.50))

    val createUpdatePensionChargesRequestBody = Json.toJson(createUpdateOverrideStateBenefit)

    "include internal headers" when {

      "the host for DES is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new StateBenefitsConnector(httpClient, appConfig(internalHost))

        stubPutWithoutResponseBody(createUpdateDesUrl, createUpdatePensionChargesRequestBody.toString(), NO_CONTENT, headersSentToDes)

        val result = await(connector.createUpdateStateBenefitOverride(nino, taxYear,benefitId, createUpdateOverrideStateBenefit)(hc))

        result mustBe Right(())
      }

      "the host for DES is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new StateBenefitsConnector(httpClient, appConfig(externalHost))

        stubPutWithoutResponseBody(createUpdateDesUrl, createUpdatePensionChargesRequestBody.toString(), NO_CONTENT, headersSentToDes)

        val result = await(connector.createUpdateStateBenefitOverride(nino, taxYear, benefitId, createUpdateOverrideStateBenefit)(hc))

        result mustBe Right(())
      }
    }

    "return a Right when there is a valid request body" in {
      stubPutWithoutResponseBody(createUpdateDesUrl, createUpdatePensionChargesRequestBody.toString(), NO_CONTENT)

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createUpdateStateBenefitOverride(nino, taxYear, benefitId, createUpdateOverrideStateBenefit)(hc))

      result mustBe Right(())
    }

    "return a Left error" when {

      Seq(INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE, BAD_REQUEST, UNPROCESSABLE_ENTITY).foreach { errorStatus =>

        val desResponseBody = Json.obj(
          "code" -> "SOME_DES_ERROR_CODE",
          "reason" -> "SOME_DES_ERROR_REASON"
        ).toString

        s"Des returns $errorStatus" in {
          val expectedResult = DesErrorModel(errorStatus, DesErrorBodyModel("SOME_DES_ERROR_CODE", "SOME_DES_ERROR_REASON"))

          implicit val hc: HeaderCarrier = HeaderCarrier()
          stubPutWithResponseBody(createUpdateDesUrl, createUpdatePensionChargesRequestBody.toString(), desResponseBody, errorStatus)

          val result = await(connector.createUpdateStateBenefitOverride(nino, taxYear, benefitId, createUpdateOverrideStateBenefit)(hc))

          result mustBe Left(expectedResult)
        }

        s"DES returns $errorStatus response that does not have a parsable error body" in {
          val expectedResult = DesErrorModel(errorStatus, DesErrorBodyModel.parsingError)

          stubPutWithResponseBody(createUpdateDesUrl, createUpdatePensionChargesRequestBody.toString, "UNEXPECTED RESPONSE BODY", errorStatus)

          implicit val hc: HeaderCarrier = HeaderCarrier()
          val result = await(connector.createUpdateStateBenefitOverride(nino, taxYear, benefitId, createUpdateOverrideStateBenefit)(hc))

          result mustBe Left(expectedResult)
        }
      }

      "DES returns an unexpected http response that is parsable" in {

        val responseBody = Json.obj(
          "code" -> "NOT_FOUND",
          "reason" -> "not found"
        )

        val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel("NOT_FOUND", "not found"))

        stubPutWithResponseBody(createUpdateDesUrl, createUpdatePensionChargesRequestBody.toString(), responseBody.toString(), NOT_FOUND)
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createUpdateStateBenefitOverride(nino, taxYear, benefitId, createUpdateOverrideStateBenefit)(hc))

        result mustBe Left(expectedResult)
      }

    }
  }

  ".ignoreStateBenefit" should {

    val ignoreStateBenefitDesUrl: String = s"/income-tax/income/state-benefits/$nino/${desTaxYearConverter(taxYear)}/ignore/$benefitId"
    val ignoreStateBenefitRequestBody = Json.toJson(IgnoreStateBenefit(true))

    "include internal headers" when {

      "the host for DES is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new StateBenefitsConnector(httpClient, appConfig(internalHost))

        stubPutWithoutResponseBody(ignoreStateBenefitDesUrl, ignoreStateBenefitRequestBody.toString(), NO_CONTENT, headersSentToDes)

        val result = await(connector.ignoreStateBenefit(nino, taxYear, benefitId, ignoreBenefit = true)(hc))

        result mustBe Right(())
      }

      "the host for DES is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new StateBenefitsConnector(httpClient, appConfig(externalHost))

        stubPutWithoutResponseBody(ignoreStateBenefitDesUrl, ignoreStateBenefitRequestBody.toString(), NO_CONTENT, headersSentToDes)

        val result = await(connector.ignoreStateBenefit(nino, taxYear, benefitId, ignoreBenefit = true)(hc))

        result mustBe Right(())
      }
    }

    "return a Right when there is a valid request body" in {
      stubPutWithoutResponseBody(ignoreStateBenefitDesUrl, ignoreStateBenefitRequestBody.toString(), NO_CONTENT)

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.ignoreStateBenefit(nino, taxYear, benefitId, ignoreBenefit = true)(hc))

      result mustBe Right(())
    }

    "return a Left error" when {

      Seq(INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE, BAD_REQUEST, UNPROCESSABLE_ENTITY, FORBIDDEN).foreach { errorStatus =>

        val desResponseBody = Json.obj(
          "code" -> "SOME_DES_ERROR_CODE",
          "reason" -> "SOME_DES_ERROR_REASON"
        ).toString

        s"Des returns $errorStatus" in {
          val expectedResult = DesErrorModel(errorStatus, DesErrorBodyModel("SOME_DES_ERROR_CODE", "SOME_DES_ERROR_REASON"))

          implicit val hc: HeaderCarrier = HeaderCarrier()
          stubPutWithResponseBody(ignoreStateBenefitDesUrl, ignoreStateBenefitRequestBody.toString(), desResponseBody, errorStatus)

          val result = await(connector.ignoreStateBenefit(nino, taxYear, benefitId, ignoreBenefit = true)(hc))

          result mustBe Left(expectedResult)
        }

        s"DES returns $errorStatus response that does not have a parsable error body" in {
          val expectedResult = DesErrorModel(errorStatus, DesErrorBodyModel.parsingError)

          stubPutWithResponseBody(ignoreStateBenefitDesUrl, ignoreStateBenefitRequestBody.toString, "UNEXPECTED RESPONSE BODY", errorStatus)

          implicit val hc: HeaderCarrier = HeaderCarrier()
          val result = await(connector.ignoreStateBenefit(nino, taxYear, benefitId, ignoreBenefit = true)(hc))

          result mustBe Left(expectedResult)
        }
      }

      "DES returns an unexpected http response that is parsable" in {

        val responseBody = Json.obj(
          "code" -> "NOT_FOUND",
          "reason" -> "not found"
        )

        val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel("NOT_FOUND", "not found"))

        stubPutWithResponseBody(ignoreStateBenefitDesUrl, ignoreStateBenefitRequestBody.toString(), responseBody.toString(), NOT_FOUND)
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.ignoreStateBenefit(nino, taxYear, benefitId, ignoreBenefit = true)(hc))

        result mustBe Left(expectedResult)
      }

    }
  }

  ".unignoreStateBenefit" should {
    val deleteIgnoreUrl: String = s"/income-tax/state-benefits/$nino/${desTaxYearConverter(taxYear)}/ignore/$benefitId"

    "include internal headers" when {

      "the host for DES is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new StateBenefitsConnector(httpClient, appConfig(internalHost))

        stubDeleteWithoutResponseBody(deleteIgnoreUrl,
          NO_CONTENT, headersSentToDes)

        val result = await(connector.unignoreStateBenefit(nino, taxYear, benefitId)(hc))
        result mustBe Right(())
      }

      "the host for DES is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new StateBenefitsConnector(httpClient, appConfig(externalHost))

        stubDeleteWithoutResponseBody(deleteIgnoreUrl,
          NO_CONTENT, headersSentToDes)

        val result = await(connector.unignoreStateBenefit(nino, taxYear, benefitId)(hc))

        result mustBe Right(())
      }
    }

    "return a Right on success" in {
      stubDeleteWithoutResponseBody(deleteIgnoreUrl, NO_CONTENT)

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.unignoreStateBenefit(nino, taxYear, benefitId)(hc))

      result mustBe Right(())
    }

    "handle a Left error" when {

      val desErrorBodyModel = DesErrorBodyModel("DES_CODE", "DES_REASON")

      Seq(INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE, NOT_FOUND, BAD_REQUEST, FORBIDDEN, UNPROCESSABLE_ENTITY).foreach { errorStatus =>

        s"DES returns expected error $errorStatus" in {
          val desError = DesErrorModel(errorStatus, desErrorBodyModel)
          implicit val hc: HeaderCarrier = HeaderCarrier()

          stubDeleteWithResponseBody(deleteIgnoreUrl, errorStatus, desError.toJson.toString())

          val result = await(connector.unignoreStateBenefit(nino, taxYear, benefitId)(hc))
          result mustBe Left(desError)
        }
      }

      "DES returns a non parsable response" in {
        val errorResponseBody = "a non parsable body"
        val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.parsingError)

        stubDeleteWithResponseBody(deleteIgnoreUrl, INTERNAL_SERVER_ERROR, errorResponseBody)
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.unignoreStateBenefit(nino, taxYear, benefitId)(hc))

        result mustBe Left(expectedResult)
      }

      "DES returns an unexpected http response that is parsable" in {

        val errorResponseBody = Json.obj(
          "code" -> "SERVER_ERROR",
          "reason" -> "DES is currently experiencing problems that require live service intervention."
        )

        val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel(
          "SERVER_ERROR", "DES is currently experiencing problems that require live service intervention."))

        stubDeleteWithResponseBody(deleteIgnoreUrl, BAD_GATEWAY, errorResponseBody.toString())
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.unignoreStateBenefit(nino, taxYear, benefitId)(hc))

        result mustBe Left(expectedResult)

      }
    }
  }

  ".addStateBenefit" should {

    val requestModel = AddStateBenefitRequestModel("statePension", "2020-08-03", Some("2020-12-03"))
    val responseModel = AddStateBenefitResponseModel(benefitId)
    val requestBody = Json.toJson(requestModel).toString
    val responseBody = Json.toJson(responseModel).toString

    val desAddUrl: String = s"/income-tax/income/state-benefits/$nino/${desTaxYearConverter(taxYear)}/custom"

    "include internal headers" when {

      "the host for DES is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new StateBenefitsConnector(httpClient, appConfig(internalHost))

        stubPostWithResponseBody(desAddUrl, OK, requestBody, responseBody, headersSentToDes)
        val result = await(connector.addStateBenefit(nino, taxYear, requestModel)(hc))

        result mustBe Right(responseModel)
      }

      "the host for DES is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new StateBenefitsConnector(httpClient, appConfig(externalHost))

        stubPostWithResponseBody(desAddUrl, OK, requestBody, responseBody, headersSentToDes)
        val result = await(connector.addStateBenefit(nino, taxYear, requestModel)(hc))

        result mustBe Right(responseModel)
      }
    }

    "return AddStateBenefitResponseModel when nino and tax year present" in {
      stubPostWithResponseBody(desAddUrl, OK, requestBody, responseBody)

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.addStateBenefit(nino, taxYear, requestModel)(hc))

      result mustBe Right(responseModel)
    }

    "handle a Left error" when {

      val desErrorBodyModel = DesErrorBodyModel("DES_CODE", "DES_REASON")

      Seq(INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE, BAD_REQUEST, UNPROCESSABLE_ENTITY).foreach { errorStatus =>

        s"DES returns expected error $errorStatus" in {
          val desError = DesErrorModel(errorStatus, desErrorBodyModel)
          implicit val hc: HeaderCarrier = HeaderCarrier()

          stubPostWithResponseBody(desAddUrl, errorStatus, requestBody, desError.toJson.toString())

          val result = await(connector.addStateBenefit(nino, taxYear, requestModel)(hc))

          result mustBe Left(desError)
        }
      }

      "DES returns a non parsable response" in {
        val errorResponseBody = "a non parsable body"
        val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.parsingError)

        stubPostWithResponseBody(desAddUrl, INTERNAL_SERVER_ERROR, requestBody, errorResponseBody)
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.addStateBenefit(nino, taxYear, requestModel)(hc))

        result mustBe Left(expectedResult)
      }

      "DES returns an unexpected http response that is parsable" in {

        val errorResponseBody = Json.obj(
          "code" -> "SERVER_ERROR",
          "reason" -> "DES is currently experiencing problems that require live service intervention."
        )

        val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel(
          "SERVER_ERROR", "DES is currently experiencing problems that require live service intervention."))

        stubPostWithResponseBody(desAddUrl, BAD_GATEWAY, requestBody, errorResponseBody.toString())
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.addStateBenefit(nino, taxYear, requestModel)(hc))

        result mustBe Left(expectedResult)
      }

      "DES returns a bad success Json" in {
        lazy val invalidJson = Json.obj("benfitId" -> false)
        val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.parsingError)

        stubPostWithResponseBody(desAddUrl, OK, requestBody, invalidJson.toString())
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.addStateBenefit(nino, taxYear, requestModel)(hc))

        result mustBe (Left(expectedResult))
      }
    }
  }


  ".updateStateBenefit" should {

    val updateUrl: String = s"/income-tax/income/state-benefits/$nino/${desTaxYearConverter(taxYear)}/custom/$benefitId"

    val appConfigWithInternalHost = appConfig("localhost")
    val appConfigWithExternalHost = appConfig("127.0.0.1")

    val updatedStateBenefitModel = UpdateStateBenefitModel("startDate", Some("endDate"))

    "include internal headers" when {
      val headersSentToDes = Seq(
        new HttpHeader(HeaderNames.authorisation, "Bearer secret"),
        new HttpHeader(HeaderNames.xSessionId, "sessionIdValue")
      )

      "the host for DES is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new StateBenefitsConnector(httpClient, appConfigWithInternalHost)

        stubPutWithoutResponseBody(
          updateUrl, Json.toJson(updatedStateBenefitModel).toString(), NO_CONTENT, headersSentToDes)

        val result = await(connector.updateStateBenefit(nino, taxYear, benefitId, updatedStateBenefitModel)(hc))
        result mustBe Right(())
      }

      "the host for DES is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new StateBenefitsConnector(httpClient, appConfigWithExternalHost)

        stubPutWithoutResponseBody(
          updateUrl, Json.toJson(updatedStateBenefitModel).toString(), NO_CONTENT)

        val result = await(connector.updateStateBenefit(nino, taxYear, benefitId, updatedStateBenefitModel)(hc))
        result mustBe Right(())
      }
    }
    "handle error" when {
      val desErrorBodyModel = DesErrorBodyModel("DES_CODE", "DES_REASON")

      Seq(BAD_REQUEST, UNPROCESSABLE_ENTITY, NOT_FOUND, FORBIDDEN, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE).foreach { status =>
        s"DES returns $status" in {
          val desError = DesErrorModel(status, desErrorBodyModel)
          implicit val hc: HeaderCarrier = HeaderCarrier()

          stubPutWithResponseBody(updateUrl, Json.toJson(updatedStateBenefitModel).toString(), desError.toJson.toString(), status)
          val result = await(connector.updateStateBenefit(nino, taxYear, benefitId, updatedStateBenefitModel))
          result mustBe Left(desError)
        }
      }
      s"DES returns unexpected error code - BAD_GATEWAY (502)" in {
        val desError = DesErrorModel(INTERNAL_SERVER_ERROR, desErrorBodyModel)
        implicit val hc: HeaderCarrier = HeaderCarrier()

        stubPutWithResponseBody(updateUrl, Json.toJson(updatedStateBenefitModel).toString(), desError.toJson.toString(), BAD_GATEWAY)

        val result = await(connector.updateStateBenefit(nino, taxYear, benefitId, updatedStateBenefitModel))

        result mustBe Left(desError)
      }
    }
  }
}

object StateBenefitsConnectorISpec {
  val expectedResponseBody: String =
    """
      |{
      |   "stateBenefits":{
      |      "incapacityBenefit":[
      |         {
      |            "dateIgnored":"2019-04-11T16:22:00Z",
      |            "submittedOn":"2020-09-11T17:23:00Z",
      |            "benefitId":"a1e8057e-fbbc-47a8-a8b4-78d9f015c934",
      |            "startDate":"2019-11-13",
      |            "endDate":"2020-08-23",
      |            "amount":1212.34,
      |            "taxPaid":22323.23
      |         }
      |      ],
      |      "statePension":{
      |         "dateIgnored":"2018-09-09T19:23:00Z",
      |         "submittedOn":"2020-08-07T12:23:00Z",
      |         "benefitId":"a1e8057e-fbbc-47a8-a8b4-78d9f015c935",
      |         "startDate":"2018-06-03",
      |         "endDate":"2020-09-13",
      |         "amount":42323.23,
      |         "taxPaid":2323.44
      |      },
      |      "statePensionLumpSum":{
      |         "dateIgnored":"2019-07-08T05:23:00Z",
      |         "submittedOn":"2020-03-13T19:23:00Z",
      |         "benefitId":"a1e8057e-fbbc-47a8-a8b4-78d9f015c936",
      |         "startDate":"2019-04-23",
      |         "endDate":"2020-08-13",
      |         "amount":45454.23,
      |         "taxPaid":45432.56
      |      },
      |      "employmentSupportAllowance":[
      |         {
      |            "dateIgnored":"2019-09-28T10:23:00Z",
      |            "submittedOn":"2020-11-13T19:23:00Z",
      |            "benefitId":"a1e8057e-fbbc-47a8-a8b4-78d9f015c937",
      |            "startDate":"2019-09-23",
      |            "endDate":"2020-08-23",
      |            "amount":44545.43,
      |            "taxPaid":35343.23
      |         }
      |      ],
      |      "jobSeekersAllowance":[
      |         {
      |            "dateIgnored":"2019-08-18T13:23:00Z",
      |            "submittedOn":"2020-07-10T18:23:00Z",
      |            "benefitId":"a1e8057e-fbbc-47a8-a8b4-78d9f015c938",
      |            "startDate":"2019-09-19",
      |            "endDate":"2020-09-23",
      |            "amount":33223.12,
      |            "taxPaid":44224.56
      |         }
      |      ],
      |      "bereavementAllowance":{
      |         "dateIgnored":"2020-08-10T12:23:00Z",
      |         "submittedOn":"2020-09-19T19:23:00Z",
      |         "benefitId":"a1e8057e-fbbc-47a8-a8b4-78d9f015c939",
      |         "startDate":"2019-05-22",
      |         "endDate":"2020-09-26",
      |         "amount":56534.23,
      |         "taxPaid":34343.57
      |      },
      |      "otherStateBenefits":{
      |         "dateIgnored":"2020-01-11T15:23:00Z",
      |         "submittedOn":"2020-09-13T15:23:00Z",
      |         "benefitId":"a1e8057e-fbbc-47a8-a8b4-78d9f015c940",
      |         "startDate":"2018-09-03",
      |         "endDate":"2020-06-03",
      |         "amount":56532.45,
      |         "taxPaid":5656.89
      |      }
      |   },
      |   "customerAddedStateBenefits":{
      |      "incapacityBenefit":[
      |         {
      |            "submittedOn":"2020-11-17T19:23:00Z",
      |            "benefitId":"a1e8057e-fbbc-47a8-a8b4-78d9f015c941",
      |            "startDate":"2018-07-17",
      |            "endDate":"2020-09-23",
      |            "amount":45646.78,
      |            "taxPaid":4544.34
      |         }
      |      ],
      |      "statePension":{
      |         "submittedOn":"2020-06-11T10:23:00Z",
      |         "benefitId":"a1e8057e-fbbc-47a8-a8b4-78d9f015c943",
      |         "startDate":"2018-04-03",
      |         "endDate":"2020-09-13",
      |         "amount":45642.45,
      |         "taxPaid":6764.34
      |      },
      |      "statePensionLumpSum":{
      |         "submittedOn":"2020-06-13T05:29:00Z",
      |         "benefitId":"a1e8057e-fbbc-47a8-a8b4-78d9f015c956",
      |         "startDate":"2019-09-23",
      |         "endDate":"2020-09-26",
      |         "amount":34322.34,
      |         "taxPaid":4564.45
      |      },
      |      "employmentSupportAllowance":[
      |         {
      |            "submittedOn":"2020-02-10T11:20:00Z",
      |            "benefitId":"a1e8057e-fbbc-47a8-a8b4-78d9f015c988",
      |            "startDate":"2019-09-11",
      |            "endDate":"2020-06-13",
      |            "amount":45424.23,
      |            "taxPaid":23232.34
      |         }
      |      ],
      |      "jobSeekersAllowance":[
      |         {
      |            "submittedOn":"2020-05-13T14:23:00Z",
      |            "benefitId":"a1e8057e-fbbc-47a8-a8b4-78d9f015c990",
      |            "startDate":"2019-07-10",
      |            "endDate":"2020-05-11",
      |            "amount":34343.78,
      |            "taxPaid":3433.56
      |         }
      |      ],
      |      "bereavementAllowance":{
      |         "submittedOn":"2020-02-13T11:23:00Z",
      |         "benefitId":"a1e8057e-fbbc-47a8-a8b4-78d9f015c997",
      |         "startDate":"2018-08-12",
      |         "endDate":"2020-07-13",
      |         "amount":45423.45,
      |         "taxPaid":4543.64
      |      },
      |      "otherStateBenefits":{
      |         "submittedOn":"2020-09-12T12:23:00Z",
      |         "benefitId":"a1e8057e-fbbc-47a8-a8b4-78d9f015c957",
      |         "startDate":"2018-01-13",
      |         "endDate":"2020-08-13",
      |         "amount":63333.33,
      |         "taxPaid":4644.45
      |      }
      |   }
      |}
      |
      |""".stripMargin
}
