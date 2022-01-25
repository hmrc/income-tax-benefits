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

package connectors

import config.AppConfig
import connectors.httpParsers.CreateUpdateOverrideStateBenefitHttpParser._
import connectors.httpParsers.DeleteOverrideStateBenefitHttpParser.{DeleteOverrideStateBenefitHttpReads, DeleteStateBenefitOverrideResponse}
import connectors.httpParsers.DeleteStateBenefitsHttpParser.{DeleteStateBenefitsHttpReads, DeleteStateBenefitsResponse}
import connectors.httpParsers.GetStateBenefitsHttpParser.{GetStateBenefitsHttpReads, GetStateBenefitsResponse}
import connectors.httpParsers.IgnoreStateBenefitHttpParser.{IgnoreStateBenefitResponse, IgnoreStateBenefitResponseHttpReads}
import models.{CreateUpdateOverrideStateBenefit, IgnoreStateBenefit}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import utils.DESTaxYearHelper.desTaxYearConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StateBenefitsConnector @Inject()(val http: HttpClient,
                                       val appConfig: AppConfig)(implicit ec: ExecutionContext) extends DesConnector {

  def overrideStateBenefitUri(nino: String, taxYear: Int, benefitId: String): String = {
    appConfig.desBaseUrl + s"/income-tax/income/state-benefits/$nino/${desTaxYearConverter(taxYear)}/$benefitId"
  }

  def getStateBenefits(nino: String, taxYear: Int, benefitId: Option[String])(implicit hc: HeaderCarrier): Future[GetStateBenefitsResponse] = {

    val queryString = benefitId.fold("")(benefitId => s"?benefitId=$benefitId")
    val incomeSourcesUri: String =
      appConfig.desBaseUrl + s"/income-tax/income/state-benefits/$nino/${desTaxYearConverter(taxYear)}$queryString"

    def desCall(implicit hc: HeaderCarrier): Future[GetStateBenefitsResponse] = {
      http.GET[GetStateBenefitsResponse](incomeSourcesUri)
    }

    desCall(desHeaderCarrier(incomeSourcesUri))
  }

  def deleteStateBenefitEndOfYear(nino: String, taxYear: Int, benefitId: String)(implicit hc: HeaderCarrier): Future[DeleteStateBenefitsResponse] = {

    val incomeSourceUri: String =
      appConfig.desBaseUrl + s"/income-tax/income/state-benefits/$nino/${desTaxYearConverter(taxYear)}/custom/$benefitId"

    def desCall(implicit hc: HeaderCarrier): Future[DeleteStateBenefitsResponse] = {
      http.DELETE[DeleteStateBenefitsResponse](incomeSourceUri)(DeleteStateBenefitsHttpReads, hc, ec)
    }

    desCall(desHeaderCarrier(incomeSourceUri))

  }

  def deleteOverrideStateBenefit(nino: String, taxYear: Int, benefitId: String)(implicit hc: HeaderCarrier): Future[DeleteStateBenefitOverrideResponse] = {

    val incomeSourceUri: String = overrideStateBenefitUri(nino, taxYear, benefitId)

    def desCall(implicit hc: HeaderCarrier): Future[DeleteStateBenefitOverrideResponse] = {
      http.DELETE[DeleteStateBenefitOverrideResponse](incomeSourceUri)(DeleteOverrideStateBenefitHttpReads, hc, ec)
    }

    desCall(desHeaderCarrier(incomeSourceUri))
  }

  def createUpdateStateBenefitOverride(nino: String, taxYear: Int, benefitId: String, model: CreateUpdateOverrideStateBenefit)
                                      (implicit hc: HeaderCarrier): Future[CreateUpdateOverrideStateBenefitResponse] = {

    val incomeSourceUri: String = overrideStateBenefitUri(nino, taxYear, benefitId)

    def desCall(implicit hc: HeaderCarrier): Future[CreateUpdateOverrideStateBenefitResponse] = {
      http.PUT[CreateUpdateOverrideStateBenefit, CreateUpdateOverrideStateBenefitResponse](
        incomeSourceUri, model)(CreateUpdateOverrideStateBenefit.format.writes, CreateUpdateOverrideStateBenefitResponseHttpReads, hc, ec)
    }

    desCall(desHeaderCarrier(incomeSourceUri))
  }

  def ignoreStateBenefit(nino: String, taxYear: Int, benefitId: String, ignoreBenefit: Boolean)
                        (implicit hc: HeaderCarrier): Future[IgnoreStateBenefitResponse] = {

    val incomeSourceUri: String =
      appConfig.desBaseUrl + s"/income-tax/income/state-benefits/$nino/${desTaxYearConverter(taxYear)}/ignore/$benefitId"

    val model = IgnoreStateBenefit(ignoreBenefit)

    def desCall(implicit hc: HeaderCarrier): Future[IgnoreStateBenefitResponse] = {
      http.PUT[IgnoreStateBenefit, IgnoreStateBenefitResponse](
        incomeSourceUri, model)(IgnoreStateBenefit.format.writes, IgnoreStateBenefitResponseHttpReads, hc, ec)
    }

    desCall(desHeaderCarrier(incomeSourceUri))
  }

}
