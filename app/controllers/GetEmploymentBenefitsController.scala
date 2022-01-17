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
import javax.inject.Inject
import models.DesErrorBodyModel.invalidView
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.GetEmploymentBenefitsService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.ViewParameterValidation.isValid

import scala.concurrent.{ExecutionContext, Future}

class GetEmploymentBenefitsController @Inject()(service: GetEmploymentBenefitsService,
                                                auth: AuthorisedAction,
                                                cc: ControllerComponents)
                                               (implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  def getEmploymentBenefits(nino: String, employmentId: String, taxYear: Int, view: String): Action[AnyContent] = auth.async { implicit user =>
    if(isValid(view)){
      service.getEmploymentBenefits(nino, employmentId, taxYear, view).map {
        case Right(model) => Ok(Json.toJson(model))
        case Left(errorModel) => Status(errorModel.status)(errorModel.toJson)
      }
    } else {
      logger.error(s"[GetEmploymentBenefitsController][getEmploymentBenefits] Supplied view is invalid. View: $view")
      Future(BadRequest(Json.toJson(invalidView)))
    }
  }
}
