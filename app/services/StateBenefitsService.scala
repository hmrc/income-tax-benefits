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

package services

import connectors.StateBenefitsConnector
import connectors.httpParsers.AddStateBenefitHttpParser.AddStateBenefitResponse
import connectors.httpParsers.CreateUpdateOverrideStateBenefitHttpParser.CreateUpdateOverrideStateBenefitResponse
import connectors.httpParsers.DeleteOverrideStateBenefitHttpParser.DeleteStateBenefitOverrideResponse
import connectors.httpParsers.DeleteStateBenefitsHttpParser.DeleteStateBenefitsResponse
import connectors.httpParsers.GetStateBenefitsHttpParser.GetStateBenefitsResponse
import connectors.httpParsers.IgnoreStateBenefitHttpParser.IgnoreStateBenefitResponse
import connectors.httpParsers.UnignoreStateBenefitHttpParser.UnignoreStateBenefitHttpParserResponse
import connectors.httpParsers.UpdateStateBenefitHttpParser.UpdateStateBenefitResponse
import models.{AddStateBenefitRequestModel, CreateUpdateOverrideStateBenefit, UpdateStateBenefitModel}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.Future

class StateBenefitsService @Inject() (connector: StateBenefitsConnector) {

  def getStateBenefits(nino: String, taxYear: Int, benefitId: Option[String])(implicit
    hc: HeaderCarrier
  ): Future[GetStateBenefitsResponse] =
    connector.getStateBenefits(nino, taxYear, benefitId)

  def deleteStateBenefit(nino: String, taxYear: Int, benefitId: String)(implicit
    hc: HeaderCarrier
  ): Future[DeleteStateBenefitsResponse] =
    connector.deleteStateBenefitEndOfYear(nino, taxYear, benefitId)

  def deleteOverrideStateBenefit(nino: String, taxYear: Int, benefitId: String)(implicit
    hc: HeaderCarrier
  ): Future[DeleteStateBenefitOverrideResponse] =
    connector.deleteOverrideStateBenefit(nino, taxYear, benefitId)

  def createUpdateStateBenefitOverride(
    nino: String,
    taxYear: Int,
    benefitId: String,
    model: CreateUpdateOverrideStateBenefit
  )(implicit hc: HeaderCarrier): Future[CreateUpdateOverrideStateBenefitResponse] =
    connector.createUpdateStateBenefitOverride(nino, taxYear, benefitId, model)

  def ignoreStateBenefit(nino: String, taxYear: Int, benefitId: String, ignoreBenefit: Boolean)(implicit
    hc: HeaderCarrier
  ): Future[IgnoreStateBenefitResponse] =
    connector.ignoreStateBenefit(nino, taxYear, benefitId, ignoreBenefit)

  def unignoreStateBenefit(nino: String, taxYear: Int, benefitId: String)(implicit
    hc: HeaderCarrier
  ): Future[UnignoreStateBenefitHttpParserResponse] =
    connector.unignoreStateBenefit(nino, taxYear, benefitId)

  def addStateBenefit(nino: String, taxYear: Int, requestModel: AddStateBenefitRequestModel)(implicit
    hc: HeaderCarrier
  ): Future[AddStateBenefitResponse] =
    connector.addStateBenefit(nino, taxYear, requestModel)

  def updateStateBenefit(nino: String, taxYear: Int, benefitId: String, benefitUpdateData: UpdateStateBenefitModel)(
    implicit hc: HeaderCarrier
  ): Future[UpdateStateBenefitResponse] =
    connector.updateStateBenefit(nino, taxYear, benefitId, benefitUpdateData)
}
