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

package utils


import com.codahale.metrics.SharedMetricRegistries
import common.{EnrolmentIdentifiers, EnrolmentKeys}
import config.{AppConfig, MockAppConfig}
import controllers.predicates.AuthorisedAction
import models.{Benefits, Employment, EmploymentBenefits}
import org.apache.pekko.actor.ActorSystem
import org.scalamock.handlers.CallHandler4
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.mvc.{AnyContentAsEmpty, ControllerComponents, DefaultActionBuilder, Result}
import play.api.test.{FakeRequest, Helpers}
import services.AuthService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Awaitable, ExecutionContext, Future}

trait TestUtils extends AnyWordSpecLike with Matchers with MockFactory with BeforeAndAfterEach {
  override def beforeEach(): Unit = {
    super.beforeEach()
    SharedMetricRegistries.clear()
  }

  implicit val actorSystem: ActorSystem = ActorSystem()

  def await[T](awaitable: Awaitable[T]): T = Await.result(awaitable, Duration.Inf)

  implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders("mtditid" -> "1234567890")
  val fakeRequestWithMtditid: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession("MTDITID" -> "1234567890")
  implicit val emptyHeaderCarrier: HeaderCarrier = HeaderCarrier()

  val mockAppConfig: AppConfig = new MockAppConfig
  implicit val mockControllerComponents: ControllerComponents = Helpers.stubControllerComponents()
  implicit val mockExecutionContext: ExecutionContext = ExecutionContext.Implicits.global
  implicit val mockAuthConnector: AuthConnector = mock[AuthConnector]
  implicit val mockAuthService: AuthService = new AuthService(mockAuthConnector)
  val defaultActionBuilder: DefaultActionBuilder = DefaultActionBuilder(mockControllerComponents.parsers.default)
  val authorisedAction = new AuthorisedAction()(mockAuthConnector, defaultActionBuilder, mockControllerComponents)


  def status(awaitable: Future[Result]): Int = await(awaitable).header.status

  def bodyOf(awaitable: Future[Result]): String = {
    val awaited = await(awaitable)
    await(awaited.body.consumeData.map(_.utf8String))
  }

  val individualEnrolments: Enrolments = Enrolments(Set(
    Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, "1234567890")), "Activated"),
    Enrolment(EnrolmentKeys.nino, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, "1234567890")), "Activated")))

  //noinspection ScalaStyle
  def mockAuth(enrolments: Enrolments = individualEnrolments): CallHandler4[Predicate, Retrieval[_], HeaderCarrier, ExecutionContext, Future[Any]] = {

    (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, Retrievals.affinityGroup, *, *)
      .returning(Future.successful(Some(AffinityGroup.Individual)))

    (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
      .returning(Future.successful(enrolments and ConfidenceLevel.L250))
  }

  val agentEnrolments: Enrolments = Enrolments(Set(

    Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, "1234567890")), "Activated"),
    Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, "0987654321")), "Activated")
  ))

  //noinspection ScalaStyle
  def mockAuthAsAgent(enrolments: Enrolments = agentEnrolments): CallHandler4[Predicate, Retrieval[_], HeaderCarrier, ExecutionContext, Future[Any]] = {

    (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, Retrievals.affinityGroup, *, *)
      .returning(Future.successful(Some(AffinityGroup.Agent)))

    (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, Retrievals.allEnrolments, *, *)
      .returning(Future.successful(enrolments))
  }

  //noinspection ScalaStyle
  def mockAuthReturnException(exception: Exception): CallHandler4[Predicate, Retrieval[_], HeaderCarrier, ExecutionContext, Future[Any]] = {
    (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *)
      .returning(Future.failed(exception))
  }

  val customerExample: EmploymentBenefits = EmploymentBenefits(
    "2020-01-04T05:01:01Z",
    Some("2020-04-04T01:01:01Z"),
    None,
    employment = Employment(
      Some(
        Benefits(
          accommodation = Some(455.67),
          assets = Some(435.54),
          assetTransfer = Some(24.58),
          beneficialLoan = Some(33.89),
          car = Some(3434.78),
          carFuel = Some(34.56),
          educationalServices = Some(445.67),
          entertaining = Some(434.45),
          expenses = Some(3444.32),
          medicalInsurance = Some(4542.47),
          telephone = Some(243.43),
          service = Some(45.67),
          taxableExpenses = Some(24.56),
          van = Some(56.29),
          vanFuel = Some(14.56),
          mileage = Some(34.23),
          nonQualifyingRelocationExpenses = Some(54.62),
          nurseryPlaces = Some(84.29),
          otherItems = Some(67.67),
          paymentsOnEmployeesBehalf = Some(67.23),
          personalIncidentalExpenses = Some(74.29),
          qualifyingRelocationExpenses = Some(78.24),
          employerProvidedProfessionalSubscriptions = Some(84.56),
          employerProvidedServices = Some(56.34),
          incomeTaxPaidByDirector = Some(67.34),
          travelAndSubsistence = Some(56.89),
          vouchersAndCreditCards = Some(34.90),
          nonCash = Some(23.89)
        )
      )
    )
  )

  val hmrcExample: EmploymentBenefits = EmploymentBenefits(
    "2020-01-04T05:01:01Z",
    None,
    None,
    employment = Employment(
      Some(
        Benefits(
          accommodation = Some(455.67),
          assets = Some(435.54),
          assetTransfer = Some(24.58),
          beneficialLoan = Some(33.89),
          car = Some(3434.78),
          carFuel = Some(34.56),
          educationalServices = Some(445.67),
          entertaining = Some(434.45),
          expenses = Some(3444.32),
          medicalInsurance = Some(4542.47),
          telephone = Some(243.43),
          service = Some(45.67),
          taxableExpenses = Some(24.56),
          van = Some(56.29),
          vanFuel = Some(14.56),
          mileage = Some(34.23),
          nonQualifyingRelocationExpenses = Some(54.62),
          nurseryPlaces = Some(84.29),
          otherItems = Some(67.67),
          paymentsOnEmployeesBehalf = Some(67.23),
          personalIncidentalExpenses = Some(74.29),
          qualifyingRelocationExpenses = Some(78.24),
          employerProvidedProfessionalSubscriptions = Some(84.56),
          employerProvidedServices = Some(56.34),
          incomeTaxPaidByDirector = Some(67.34),
          travelAndSubsistence = Some(56.89),
          vouchersAndCreditCards = Some(34.90),
          nonCash = Some(23.89)
        )
      )
    )
  )

  val latestExample: EmploymentBenefits = EmploymentBenefits(
    "2020-01-04T05:01:01Z",
    None,
    Some("CUSTOMER"),
    employment = Employment(
      Some(
        Benefits(
          accommodation = Some(455.67),
          assets = Some(435.54),
          assetTransfer = Some(24.58),
          beneficialLoan = Some(33.89),
          car = Some(3434.78),
          carFuel = Some(34.56),
          educationalServices = Some(445.67),
          entertaining = Some(434.45),
          expenses = Some(3444.32),
          medicalInsurance = Some(4542.47),
          telephone = Some(243.43),
          service = Some(45.67),
          taxableExpenses = Some(24.56),
          van = Some(56.29),
          vanFuel = Some(14.56),
          mileage = Some(34.23),
          nonQualifyingRelocationExpenses = Some(54.62),
          nurseryPlaces = Some(84.29),
          otherItems = Some(67.67),
          paymentsOnEmployeesBehalf = Some(67.23),
          personalIncidentalExpenses = Some(74.29),
          qualifyingRelocationExpenses = Some(78.24),
          employerProvidedProfessionalSubscriptions = Some(84.56),
          employerProvidedServices = Some(56.34),
          incomeTaxPaidByDirector = Some(67.34),
          travelAndSubsistence = Some(56.89),
          vouchersAndCreditCards = Some(34.90),
          nonCash = Some(23.89)
        )
      )
    )
  )
}

