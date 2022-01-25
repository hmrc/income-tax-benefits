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

package controllers

import controllers.predicates.AuthorisedAction
import models.{AddStateBenefitRequestModel, CreateUpdateOverrideStateBenefit, StateBenefitTypes}
import play.api.Logging
import play.api.libs.json.{JsSuccess, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.StateBenefitsService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import javax.inject.Inject
import models.UpdateStateBenefitModel
import scala.concurrent.{ExecutionContext, Future}

class StateBenefitsController @Inject()(service: StateBenefitsService,
                                        auth: AuthorisedAction,
                                        cc: ControllerComponents
                                       )(implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  def getStateBenefits(nino: String, taxYear: Int, benefitId: Option[String]): Action[AnyContent] = auth.async { implicit user =>
    service.getStateBenefits(nino, taxYear, benefitId).map {
      case Right(model) => Ok(Json.toJson(model))
      case Left(errorModel) => Status(errorModel.status)(errorModel.toJson)
    }
  }

  def deleteStateBenefit(nino: String, taxYear: Int, benefitId: String): Action[AnyContent] = auth.async { implicit user =>
    service.deleteStateBenefit(nino, taxYear, benefitId).map {
      case Right(_) => NoContent
      case Left(errorModel) => Status(errorModel.status)(errorModel.toJson)
    }
  }

  def deleteOverrideStateBenefit(nino: String, taxYear: Int, benefitId: String): Action[AnyContent] = auth.async { implicit user =>
    service.deleteOverrideStateBenefit(nino, taxYear, benefitId).map {
      case Right(_) => NoContent
      case Left(errorModel) => Status(errorModel.status)(errorModel.toJson)
    }
  }

  def createUpdateOverrideStateBenefit(nino: String, taxYear: Int, benefitId: String): Action[AnyContent] = auth.async { implicit user =>
    user.body.asJson.map(_.validate[CreateUpdateOverrideStateBenefit]) match {
      case Some(JsSuccess(model, _)) => service.createUpdateStateBenefitOverride(nino, taxYear, benefitId, model).map {
        case Right(_) => NoContent
        case Left(errorModel) => Status(errorModel.status)(errorModel.toJson)
      }
      case _ => Future.successful(BadRequest)
    }
  }

  def ignoreStateBenefit(nino: String, taxYear: Int, benefitId: String, ignoreBenefit: Boolean): Action[AnyContent] = auth.async { implicit user =>
    service.ignoreStateBenefit(nino, taxYear, benefitId, ignoreBenefit).map {
      case Right(_) => NoContent
      case Left(errorModel) => Status(errorModel.status)(errorModel.toJson)
    }
  }

  def unignoreStateBenefit(nino: String, taxYear: Int, benefitId: String): Action[AnyContent] = auth.async { implicit user =>
    service.unignoreStateBenefit(nino, taxYear, benefitId).map {
      case Right(_) => NoContent
      case Left(errorModel) => Status(errorModel.status)(errorModel.toJson)
    }
  }

  def addStateBenefit(nino: String, taxYear: Int): Action[AnyContent] = auth.async { implicit user =>

    user.body.asJson.map(_.validate[AddStateBenefitRequestModel]) match {
      case Some(JsSuccess(model, _)) =>
        if (StateBenefitTypes(model.benefitType).isDefined) {

          service.addStateBenefit(nino, taxYear, model).map {
            case Right(responseModel) => Ok(Json.toJson(responseModel))
            case Left(errorModel) => Status(errorModel.status)(errorModel.toJson)
          }
        } else {
          logger.warn("[StateBenefitsController][addStateBenefit] The AddStateBenefit request body has an invalid benefit type")
          Future.successful(BadRequest)
        }
      case _ =>
        logger.warn("[StateBenefitsController][addStateBenefit] The AddStateBenefit request body is invalid")
        Future.successful(BadRequest)
    }
  }
  def updateStateBenefit(nino: String, taxYear: Int, benefitId: String): Action[AnyContent] = auth.async { implicit user =>
    user.body.asJson.map(_.validate[UpdateStateBenefitModel]) match {
      case Some(JsSuccess(model@UpdateStateBenefitModel(_, _), _)) => {
        service.updateStateBenefit(nino, taxYear, benefitId, model).map {
          case Right(_) => NoContent
          case Left(errorModel) => Status(errorModel.status)(errorModel.toJson)
        }
      }
      case _ => {
        logger.warn("[StateBenefitsController][updateStateBenefit] Update state benefit request is invalid")
        Future.successful(BadRequest)
      }
    }
  }
}
