/*
 * Copyright 2024 HM Revenue & Customs
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

import config.AppConfig
import connectors.httpParsers.AddStateBenefitHttpParser.{AddStateBenefitHttpReads, AddStateBenefitResponse}
import connectors.httpParsers.CreateUpdateOverrideStateBenefitHttpParser._
import connectors.httpParsers.DeleteOverrideStateBenefitHttpParser.{DeleteOverrideStateBenefitHttpReads, DeleteStateBenefitOverrideResponse}
import connectors.httpParsers.DeleteStateBenefitsHttpParser.{DeleteStateBenefitsHttpReads, DeleteStateBenefitsResponse}
import connectors.httpParsers.GetStateBenefitsHttpParser.{GetStateBenefitsHttpReads, GetStateBenefitsResponse}
import connectors.httpParsers.IgnoreStateBenefitHttpParser.{IgnoreStateBenefitResponse, IgnoreStateBenefitResponseHttpReads}
import connectors.httpParsers.UnignoreStateBenefitHttpParser.{UnignoreStateBenefitHttpParserResponse, UnignoreStateBenefitHttpReads}
import connectors.httpParsers.UpdateStateBenefitHttpParser.{UpdateStateBenefitHttpReads, UpdateStateBenefitResponse}
import models.{AddStateBenefitRequestModel, CreateUpdateOverrideStateBenefit, IgnoreStateBenefit, UpdateStateBenefitModel}
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import utils.DESTaxYearHelper.desTaxYearConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StateBenefitsConnector @Inject() (val http: HttpClientV2, val appConfig: AppConfig)(implicit ec: ExecutionContext)
    extends DesConnector {

  def getStateBenefits(nino: String, taxYear: Int, benefitId: Option[String])(implicit
    hc: HeaderCarrier
  ): Future[GetStateBenefitsResponse] = {

    val queryString = benefitId.fold("")(benefitId => s"?benefitId=$benefitId")
    val incomeSourcesUri: String =
      appConfig.desBaseUrl + s"/income-tax/income/state-benefits/$nino/${desTaxYearConverter(taxYear)}$queryString"

    def desCall(implicit hc: HeaderCarrier): Future[GetStateBenefitsResponse] =
      http
        .get(url"$incomeSourcesUri")
        .execute[GetStateBenefitsResponse]

    desCall(desHeaderCarrier(incomeSourcesUri))
  }

  def deleteStateBenefitEndOfYear(nino: String, taxYear: Int, benefitId: String)(implicit
    hc: HeaderCarrier
  ): Future[DeleteStateBenefitsResponse] = {

    val incomeSourceUri: String =
      appConfig.desBaseUrl + s"/income-tax/income/state-benefits/$nino/${desTaxYearConverter(taxYear)}/custom/$benefitId"

    def desCall(implicit hc: HeaderCarrier): Future[DeleteStateBenefitsResponse] =
      http
        .delete(url"$incomeSourceUri")
        .execute[DeleteStateBenefitsResponse](DeleteStateBenefitsHttpReads, ec)


    //    http.DELETE[DeleteStateBenefitsResponse](incomeSourceUri)(DeleteStateBenefitsHttpReads, hc, ec)

    desCall(desHeaderCarrier(incomeSourceUri))

  }

  def deleteOverrideStateBenefit(nino: String, taxYear: Int, benefitId: String)(implicit
    hc: HeaderCarrier
  ): Future[DeleteStateBenefitOverrideResponse] = {

    val incomeSourceUri: String = overrideStateBenefitUri(nino, taxYear, benefitId)

    def desCall(implicit hc: HeaderCarrier): Future[DeleteStateBenefitOverrideResponse] =
      http
        .delete(url"$incomeSourceUri")
        .execute[DeleteStateBenefitOverrideResponse](DeleteOverrideStateBenefitHttpReads, ec)

//    http.DELETE[DeleteStateBenefitOverrideResponse](incomeSourceUri)(DeleteOverrideStateBenefitHttpReads, hc, ec)

    desCall(desHeaderCarrier(incomeSourceUri))
  }

  def createUpdateStateBenefitOverride(
    nino: String,
    taxYear: Int,
    benefitId: String,
    model: CreateUpdateOverrideStateBenefit
  )(implicit hc: HeaderCarrier): Future[CreateUpdateOverrideStateBenefitResponse] = {

    val incomeSourceUri: String = overrideStateBenefitUri(nino, taxYear, benefitId)

    def desCall(implicit hc: HeaderCarrier): Future[CreateUpdateOverrideStateBenefitResponse] =
      http
        .put(url"$incomeSourceUri")
        .withBody(Json.toJson(model))
        .execute[CreateUpdateOverrideStateBenefitResponse](CreateUpdateOverrideStateBenefitResponseHttpReads, ec)

    //      http.PUT[CreateUpdateOverrideStateBenefit, CreateUpdateOverrideStateBenefitResponse](incomeSourceUri, model)(
//        implicitly[Writes[CreateUpdateOverrideStateBenefit]],
//        CreateUpdateOverrideStateBenefitResponseHttpReads,
//        hc,
//        ec
//      )

    desCall(desHeaderCarrier(incomeSourceUri))
  }

  def overrideStateBenefitUri(nino: String, taxYear: Int, benefitId: String): String =
    appConfig.desBaseUrl + s"/income-tax/income/state-benefits/$nino/${desTaxYearConverter(taxYear)}/$benefitId"

  def ignoreStateBenefit(nino: String, taxYear: Int, benefitId: String, ignoreBenefit: Boolean)(implicit
    hc: HeaderCarrier
  ): Future[IgnoreStateBenefitResponse] = {

    val incomeSourceUri: String =
      appConfig.desBaseUrl + s"/income-tax/income/state-benefits/$nino/${desTaxYearConverter(taxYear)}/ignore/$benefitId"

    val model = IgnoreStateBenefit(ignoreBenefit)

    def desCall(implicit hc: HeaderCarrier): Future[IgnoreStateBenefitResponse] =
      http
        .put(url"$incomeSourceUri")
        .withBody(Json.toJson(model))
        .execute[IgnoreStateBenefitResponse](IgnoreStateBenefitResponseHttpReads, ec)

//    http.PUT[IgnoreStateBenefit, IgnoreStateBenefitResponse](incomeSourceUri, model)(
//        implicitly[Writes[IgnoreStateBenefit]],
//        IgnoreStateBenefitResponseHttpReads,
//        hc,
//        ec
//      )

    desCall(desHeaderCarrier(incomeSourceUri))
  }

  def unignoreStateBenefit(nino: String, taxYear: Int, benefitId: String)(implicit
    hc: HeaderCarrier
  ): Future[UnignoreStateBenefitHttpParserResponse] = {

    val incomeSourceUri: String =
      appConfig.desBaseUrl + s"/income-tax/state-benefits/$nino/${desTaxYearConverter(taxYear)}/ignore/$benefitId"

    def desCall(implicit hc: HeaderCarrier): Future[UnignoreStateBenefitHttpParserResponse] =
      http
        .delete(url"$incomeSourceUri")
        .execute[UnignoreStateBenefitHttpParserResponse](UnignoreStateBenefitHttpReads, ec)

    //      http.DELETE[UnignoreStateBenefitHttpParserResponse](incomeSourceUri)(UnignoreStateBenefitHttpReads, hc, ec)

    desCall(desHeaderCarrier(incomeSourceUri))
  }

  def addStateBenefit(nino: String, taxYear: Int, model: AddStateBenefitRequestModel)(implicit
    hc: HeaderCarrier
  ): Future[AddStateBenefitResponse] = {
    val incomeSourceUri: String =
      appConfig.desBaseUrl + s"/income-tax/income/state-benefits/$nino/${desTaxYearConverter(taxYear)}/custom"

    def desCall(implicit hc: HeaderCarrier): Future[AddStateBenefitResponse] =
      http
        .post(url"$incomeSourceUri")
        .withBody(Json.toJson(model))
        .execute[AddStateBenefitResponse]

    //      http.POST[AddStateBenefitRequestModel, AddStateBenefitResponse](incomeSourceUri, model)

    desCall(desHeaderCarrier(incomeSourceUri))
  }

  def updateStateBenefit(nino: String, taxYear: Int, benefitId: String, benefitUpdateData: UpdateStateBenefitModel)(
    implicit hc: HeaderCarrier
  ): Future[UpdateStateBenefitResponse] = {

    val uri: String =
      appConfig.desBaseUrl + s"/income-tax/income/state-benefits/$nino/${desTaxYearConverter(taxYear)}/custom/$benefitId"

    def desCall(implicit hc: HeaderCarrier): Future[UpdateStateBenefitResponse] =
      http
        .put(url"$uri")
        .withBody(Json.toJson(benefitUpdateData))
        .execute[UpdateStateBenefitResponse](UpdateStateBenefitHttpReads, ec)

    //      http.PUT[UpdateStateBenefitModel, UpdateStateBenefitResponse](uri, benefitUpdateData)(
//        UpdateStateBenefitModel.format,
//        UpdateStateBenefitHttpReads,
//        hc,
//        ec
//      )

    desCall(desHeaderCarrier(uri))
  }
}
