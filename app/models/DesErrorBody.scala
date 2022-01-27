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

package models

import play.api.libs.json.{JsValue, Json, OFormat}

sealed trait DesErrorBody

case class DesErrorModel(status: Int, body: DesErrorBody){
  def toJson: JsValue ={
    body match {
      case error: DesErrorBodyModel => Json.toJson(error)
      case errors: DesErrorsBodyModel => Json.toJson(errors)
    }
  }
}

/** Single DES Error **/
case class DesErrorBodyModel(code: String, reason: String) extends DesErrorBody

object DesErrorBodyModel {
  implicit val formats: OFormat[DesErrorBodyModel] = Json.format[DesErrorBodyModel]
  val parsingError: DesErrorBodyModel = DesErrorBodyModel("PARSING_ERROR", "Error parsing response from DES")
  val invalidView: DesErrorBodyModel = DesErrorBodyModel("INVALID_VIEW", "Submission has not passed validation. Invalid query parameter view.")
  val invalidTaxYear: DesErrorBodyModel = DesErrorBodyModel(
    "INVALID_TAX_YEAR", "Submission has not passed validation. Invalid parameter taxYear.")
  val invalidTaxableEntityId: DesErrorBodyModel = DesErrorBodyModel(
    "INVALID_TAXABLE_ENTITY_ID", "Submission has not passed validation. Invalid parameter taxableEntityId.")
  val invalidCorrelationId: DesErrorBodyModel = DesErrorBodyModel(
    "INVALID_CORRELATIONID", "Submission has not passed validation. Invalid Header parameter CorrelationId.")
  val invalidPayload: DesErrorBodyModel = DesErrorBodyModel(
    "INVALID_PAYLOAD", "Submission has not passed validation. Invalid payload.")
  val noDataFound: DesErrorBodyModel = DesErrorBodyModel(
    "NO_DATA_FOUND", "The remote endpoint has indicated that the requested resource could not be found.")
  val invalidBenefitId: DesErrorBodyModel = DesErrorBodyModel(
    "INVALID_BENEFIT_ID", "Submission has not passed validation. Invalid parameter benefitId.")
  val serviceUnavailable: DesErrorBodyModel = DesErrorBodyModel("SERVICE_UNAVAILABLE", "Dependent systems are currently not responding.")
  val serverError: DesErrorBodyModel = DesErrorBodyModel("SERVER_ERROR", "DES is currently experiencing problems that require live service intervention.")
  val invalidDateRange: DesErrorBodyModel = DesErrorBodyModel(
    "INVALID_DATE_RANGE", "The remote endpoint has indicated that tax year requested exceeds CY-4.")
  val taxYearNotSupported: DesErrorBodyModel = DesErrorBodyModel(
    "TAX_YEAR_NOT_SUPPORTED", "The remote endpoint has indicated that requested tax year is not supported.")
  val deleteForbidden: DesErrorBodyModel = DesErrorBodyModel(
    "DELETE_FORBIDDEN", "The remote endpoint has indicated that HMRC held State Benefit cannot be deleted.")
  val requestBeforeTaxYear: DesErrorBodyModel = DesErrorBodyModel(
    "INVALID_REQUEST_BEFORE_TAX_YEAR", "The remote endpoint has indicated that submission is provided before the tax year has ended.")
  val ignoreForbidden: DesErrorBodyModel = DesErrorBodyModel(
    "IGNORE_FORBIDDEN", "The remote endpoint has indicated that HMRC held State Benefit cannot be ignored.")
  val notSupportedTaxYear: DesErrorBodyModel = DesErrorBodyModel(
    "NOT_SUPPORTED_TAX_YEAR", "The remote endpoint has indicated that submission is provided before the tax year has ended.")
  val forbiddenUnignore: DesErrorBodyModel = DesErrorBodyModel(
    "CUSTOMER_ADDED", "The remote endpoint has indicated you Cannot un-ignore customer added State Benefit.")
  val beforeTaxYear: DesErrorBodyModel = DesErrorBodyModel(
    "BEFORE_TAX_YEAR_ENDED", "Submission is provided before the tax year has ended.")
  val invalidRequestTaxYear: DesErrorBodyModel = DesErrorBodyModel(
    "INVALID_REQUEST_TAX_YEAR", "The remote endpoint has indicated that tax year provided is invalid.")
  val invalidStartDate: DesErrorBodyModel = DesErrorBodyModel(
    "INVALID_START_DATE", "The remote endpoint has indicated that start date is after the end of the tax year.")
  val invalidCessationDate: DesErrorBodyModel = DesErrorBodyModel(
    "INVALID_CESSATION_DATE", "The remote endpoint has indicated that cessation date is before the start of the tax year.")
}

/** Multiple DES Errors **/
case class DesErrorsBodyModel(failures: Seq[DesErrorBodyModel]) extends DesErrorBody

object DesErrorsBodyModel {
  implicit val formats: OFormat[DesErrorsBodyModel] = Json.format[DesErrorsBodyModel]
}
