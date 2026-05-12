# GitHub Copilot Instructions

This is a Scala/Play Framework frontend microservice in the HMRC income tax self-employment domain.
It uses the GOV.UK Design System via `play-frontend-hmrc` and follows HMRC's standard frontend patterns.

---

## Project Overview

- **Language**: Scala 3.3.7
- **Framework**: Play Framework 3.0 (via `bootstrap-frontend-play-30`)
- **Frontend**: `play-frontend-hmrc-play-30` (wraps GOV.UK Frontend + HMRC Frontend)
- **Build tool**: SBT
- **Persistence**: MongoDB via `hmrc-mongo-play-30`
- **Auth**: `uk.gov.hmrc.auth.core` via bootstrap
- **HTTP client**: `HttpClientV2`
- **FP**: Cats (`EitherT`, `cats-core`)
- **JSON**: Play JSON (`play.api.libs.json`)

The application runs on port `10901`.  
Integration tests live in `it/test/`, unit tests in `test/`.

---

## General Scala Practices

### Style and Formatting

- All code **must** be formatted with Scalafmt before committing. Run: `sbt scalafmtAll scalafmtSbt`
- Scalac options enforce warnings-as-errors on CI; write code that compiles without warnings locally too.
- Prefer `val` over `var`. Never use mutable state unless forced by a framework boundary.
- Prefer expression-oriented code. Avoid `return`.
- Use `case class` for data-holding types (Scala 3 case classes are final by default). Use `sealed trait` or `enum` for sum types.
- Use `object` companions for JSON `Format`/`Reads`/`Writes`, enumeratum instances, and factory methods.
- Model domain concepts as opaque types or value classes. Never pass raw `String` where a domain type exists.
- Prefer `Option` over `null`. Prefer `Either[Error, A]` over throwing exceptions.
- Use `cats.data.EitherT` for async error-handling chains. The type alias `ApiResultT[A]` = `EitherT[Future, ServiceError, A]` is already defined in `models.domain`.
- Use `for`-comprehensions over nested `map`/`flatMap` when there are two or more steps.
- Annotate `given`/`using` parameters explicitly; avoid over-relying on implicit/given resolution that is difficult to follow.
- Keep functions small and single-purpose. Prefer composition over inheritance.
- Suffix `Impl` on concrete implementations of traits (e.g. `FrontendAppConfigImpl`).
- All files **must** include the Apache 2.0 licence header.

### Imports

- Do not use wildcard imports except for:
  - `models._` / `models.common._` in routes files (configured in `build.sbt`)
  - Twirl template imports (configured in `build.sbt`)
  - Play test DSL: `play.api.test.Helpers._`
- Use `as` for import renames: `import Foo.{Bar as Baz}` (not `=>`).
- Group imports: standard library, then third-party, then project-internal. Let Scalafmt/IntelliJ organise them.
- Some implicit values need explicit imports under Scala 3 (e.g. `import play.api.libs.ws.writeableOf_JsValue` for JSON POST bodies).

### Error Handling

- Never throw exceptions in business logic. Use `EitherT` / `Future[Either[..]]`.
- Map connector errors to `ServiceError` subtypes at the service boundary.
- The `ErrorHandler` renders standard HMRC error pages. Do not return raw 500s from controllers.

### Async

- All controller actions and service methods that touch external I/O return `Future[_]`.
- Always thread an `ExecutionContext` and `HeaderCarrier` implicitly rather than explicitly unless Play requires it.
- Never use `Await.result` in production code.

---

## GOV.UK / HMRC Frontend Patterns

### Layout

Every page uses `templates/Layout.scala.html`, which wraps `GovukLayout` with standard HMRC header, footer, timeout dialog, back link, language toggle, and the "report a technical problem" link.

```html
@layout(pageTitle = titleNoForm(messages("myPage.title"))) {
  @heading(messages("myPage.heading"))
  ...content...
}
```

### Component Library

Use the pre-built Twirl components in `views/components/`. The following are available and should be **preferred** over constructing GOV.UK components inline:

| Component file | Purpose |
|---|---|
| `Heading.scala.html` | H1 page heading |
| `Heading2.scala.html` | H2 sub-heading |
| `HeadingWithHint.scala.html` | H1 with a hint paragraph |
| `Button.scala.html` | Primary submit button |
| `SubmitButton.scala.html` | Form submit with CSRF |
| `TwoRadios.scala.html` | Yes/No radio group |
| `YesNoWithHeadingAndHint.scala.html` | Yes/No with heading and hint |
| `ConditionalRadio.scala.html` | Radios with conditional reveal |
| `CheckboxesWithExclusive.scala.html` | Checkboxes with an exclusive "none" option |
| `ErrorSummarySection.scala.html` | GOV.UK error summary |
| `PageCYA.scala.html` | Check-your-answers summary card |
| `PageCYASubmit.scala.html` | CYA page with submit action |
| `SingleAmountContent.scala.html` | Single currency amount input |
| `FoldableDetails.scala.html` | `<details>` / summary disclosure |
| `Link.scala.html` | Styled anchor |
| `CaptionWithTaxYear.scala.html` | Tax year caption above H1 |

All GOV.UK Frontend components (`GovukInput`, `GovukRadios`, `GovukCheckboxes`, etc.) from `uk.gov.hmrc.govukfrontend.views.html.components._` are available globally in Twirl templates via `build.sbt` template imports.

### Page Titles

Follow the GOV.UK pattern: `"<Question or page name> - <Service name> - GOV.UK"`.
Use the `ViewUtils` helpers (`titleNoForm`, `title`) already imported globally.

### Forms and Validation

- All forms use Play's `Form` API with `play-conditional-form-mapping-play-30`.
- Form providers live in `app/forms/` and extend `FormProvider` conventions used elsewhere in the project.
- Bind forms with `form.bindFromRequest()`. On failure render the view with `BadRequest`; on success redirect.
- Always display `ErrorSummarySection` at the top of a form page when a form has errors.
- Error messages are defined in `conf/messages.en` (and `messages.cy` for Welsh). Keys follow the pattern `<pageName>.error.<fieldName>.<reason>`.

### i18n

- All user-facing strings must have entries in both `conf/messages.en` and `conf/messages.cy`.
- Retrieve messages via `implicit messages: Messages` — never hardcode English strings in templates.
- Use `messages("key")` not `Messages("key")`.

### Routes

- Route files are in `conf/`. The main file is `app.routes`; journey-specific routes are in `appExpenses.routes`, `appCapitalAllowances.routes`, `appAdjustments.routes`, `appNICs.routes`.
- Always use reverse routing (`routes.MyController.onPageLoad(...)`) rather than string URLs.
- Route parameters import `models._` and `models.common._` (configured in `build.sbt`).

### Navigation

- Page-to-page navigation is encapsulated in Navigator classes (e.g. `TravelAndAccommodationNavigator`).
- Add a `next` method to each `Page` object that delegates to the relevant Navigator.
- Navigator routes are partial functions over `Page` cases. Add new pages to the relevant Navigator.

---

## Scala 3 Specifics

This project was migrated from Scala 2.13 to Scala 3.3.7 (LTS). Keep these Scala 3 conventions in mind:

### Syntax

- **Lambda type annotations** must use parenthesised parameters: `{ (x: Type) => ... }` not `{ x: Type => ... }`.
- **Import renames** use `as` instead of `=>`: `import Foo.{Bar as Baz}`.
- **`enum` is a reserved word** — do not use it as a parameter or variable name. Use `ev` for `Enumerable` evidence parameters.
- **No-arg method calls** in routes and reverse routing require explicit `()`: `Controller.action()` not `Controller.action`.
- **`given`/`using`** is preferred for new code, but existing `implicit` parameters are fine and used throughout the codebase.

### Forms and `unapply`

Scala 3 case class `unapply` returns the case class itself, not `Option[TupleN]`. For Play `Form` mappings with multi-field case classes, use:

```scala
// Scala 3 — use Tuple.fromProductTyped for the unbind function
mapping(
  "field1" -> text,
  "field2" -> number
)(MyCaseClass.apply)(m => Some(Tuple.fromProductTyped(m)))
```

For `Writes` with `unlift`, replace `unlift(MyClass.unapply)` with an explicit lambda:

```scala
// Scala 3
(__ \ "field").write[String]
)(obj => (obj.field1, obj.field2))
```

### Implicits and Givens

- Some implicit conversions need explicit imports in Scala 3 due to stricter resolution, e.g. `import play.api.libs.ws.writeableOf_JsValue`.
- Use `Matchers.shouldBe` instead of `convertToAnyShouldWrapper` in ScalaTest imports.
- When using `EitherT`, you may need explicit type parameters `[Future, ServiceError]` to avoid ambiguous given errors.

### `@nowarn` Annotations

Use `@nowarn("msg=...")` (message-based) instead of `@nowarn("cat=...")` (category-based). Example: `@nowarn("msg=unused")`.

---

## Application Architecture

### Layers

```
HTTP Request
    ↓
Action Composition  (app/controllers/actions/)
    ↓
Controller          (app/controllers/journeys/<area>/)
    ↓
Service             (app/services/)
    ↓
Connector           (app/connectors/)
    ↓
Backend API / Auth
```

### Controllers

- Extend `FrontendBaseController` and mix in `I18nSupport`.
- Use action composition: `(identify andThen getData)` or `(identify andThen getData andThen requireData)`.
- Inject dependencies via constructor injection with `@Inject()`.
- Keep controllers thin — delegate all business logic to services.
- Return `Action[AnyContent]` from `onPageLoad` and `onSubmit`.
- On form submission:
  - `BadRequest` + re-render view on validation failure.
  - `Redirect` to next page via Navigator on success.

### Services

- Defined as traits with `Impl` suffixed concrete classes.
- Services coordinate between connectors, repositories, and domain logic.
- Use `ApiResultT[A]` (`EitherT[Future, ServiceError, A]`) for all async operations that can fail.
- Do not perform HTTP calls directly — delegate to Connectors.

### Connectors

- One connector per downstream service.
- Use `HttpClientV2` for all HTTP calls.
- Map HTTP responses to domain types using `HttpReads`/custom `HttpParser` implementations in `connectors/httpParser/`.
- Thread `HeaderCarrier` implicitly.

### Pages and User Answers

- `Page` objects in `app/pages/` represent individual form questions.
- `UserAnswers` (stored in MongoDB via `SessionRepository`) hold per-session state keyed by `businessId`.
- Use `userAnswers.set(page, value, Some(businessId))` and `userAnswers.get(page, Some(businessId))`.
- Pages that clear downstream answers when changed should implement `cleanUp` or use `clearDependentPages`.

---

## Testing

Run all tests:

```bash
sbt clean test it/test
```

Run with coverage:

```bash
sbt clean coverage test it/test coverageReport
```

Run a single test class:

```bash
sbt "testOnly controllers.journeys.expenses.travelAndAccommodation.SimplifiedExpensesControllerSpec"
```

---

## Unit Tests (`test/`)

### Frameworks and Libraries

| Library | Use |
|---|---|
| ScalaTest (`AnyFreeSpec` / `AnyWordSpec`) | Test structure and assertions |
| `Matchers` (must-style) | `result mustBe`, `result mustEqual` |
| Mockito (`mockito-core` 5.12, `scalatestplus-mockito-5-12`) | Mocking traits/classes |
| `base.MockitoCompat` (`ArgumentMatchersSugar`, `IdiomaticMockitoCompat`) | Scala 3 compatible mocking DSL |
| ScalaCheck | Property-based testing |
| Play test helpers (`FakeRequest`, `route`, `status`, `contentAsString`) | Controller testing |

### Base Classes

Always extend the appropriate base:

- `SpecBase` — general unit tests; provides `FakeRequest`, `UserAnswers` helpers, `EmptyUserAnswers`, implicit `ExecutionContext`, etc.
- `ControllerSpec` — controller tests; adds `MockitoSugar`, `ArgumentMatchersSugar`, `IdiomaticMockitoCompat`, `TableDrivenPropertyChecks`, and pre-built `TestScenario`/`TestStubbedScenario` case classes.

```scala
class MyServiceSpec extends SpecBase with ControllerTestScenarioSpec {
  // use mockService, mockTimeMachine, emptyUserAnswers, etc.
}
```

### Mocking with Mockito

This project uses `mockito-core` 5.12 with `scalatestplus-mockito` and a custom compatibility layer in `test/base/MockitoCompat.scala` that provides Scala 3–friendly DSL replacements for the removed `mockito-scala` library.

**Standard Mockito style** (preferred for explicit stubs):

```scala
import org.mockito.Mockito.{when, verify}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.scalatestplus.mockito.MockitoSugar

val mockConnector: SelfEmploymentConnector = mock[SelfEmploymentConnector]

when(mockConnector.getBusinesses(eqTo(nino), eqTo(mtditid))(any(), any()))
  .thenReturn(EitherT.rightT[Future, ServiceError](Seq(businessData)))
```

**Idiomatic DSL style** (concise, for base specs and simple stubs):

```scala
import base.IdiomaticMockitoCompat.StubbingOps
import org.mockito.ArgumentMatchers.{any => anyMatcher}

// Use `returns` extension method from MockitoCompat
mockService.persistAnswer(anyMatcher(), anyMatcher(), anyMatcher(), anyMatcher())(anyMatcher()) returns pageAnswers.asFuture
```

**`ArgumentMatchersSugar` from `base.MockitoCompat`** provides `*` and `*[T]` matchers as a drop-in for the removed `mockito-scala` DSL. Use `anyMatcher()` (aliased from `org.mockito.ArgumentMatchers.any`) when `*` is ambiguous.

> **Do not** use `org.mockito.IdiomaticMockito` or `org.mockito.ArgumentMatchersSugar` — these are from the removed `mockito-scala` library. Always import from `base.MockitoCompat` (`base.ArgumentMatchersSugar`, `base.IdiomaticMockitoCompat`).

### Service Unit Tests

**Every public method on every service must have at least one unit test.**

Structure tests using ScalaTest's `FreeSpec` nested DSL:

```scala
class MyServiceSpec extends SpecBase {

  "MyService" - {
    "methodName" - {
      "should return X when Y" in { ... }
      "should return error when connector fails" in { ... }
    }
  }
}
```

Test both the happy path and all meaningful error paths:

```scala
"getJourneyStatus" - {
  "should return the journey status on success" in {
    mockConnector.getJourneyState(...) returns EitherT.rightT(JourneyNameAndStatus(...))
    val result = service.getJourneyStatus(ctx).value.futureValue
    result mustBe Right(JourneyStatus.Completed)
  }

  "should return ServiceError when the connector fails" in {
    mockConnector.getJourneyState(...) returns EitherT.leftT(ConnectorResponseError(...))
    val result = service.getJourneyStatus(ctx).value.futureValue
    result mustBe Left(ConnectorResponseError(...))
  }
}
```

Use the `StubSessionRepository` and `SelfEmploymentServiceStub` stubs from `test/stubs/` when testing classes that depend on repositories or services — only use Mockito when fine-grained call verification is needed.

### Controller Unit Tests

Use `TestScenario` / `TestStubbedScenario` from `ControllerSpec`:

```scala
class MyControllerSpec extends ControllerSpec {

  "onPageLoad" - {
    "should return OK for an Individual" in {
      val scenario = TestStubbedScenario(
        userType = Individual,
        answers  = Some(emptyUserAnswers)
      )
      import scenario.application
      val request = FakeRequest(GET, routes.MyController.onPageLoad(taxYear, businessId, NormalMode).url)
      val result  = route(application, request).value
      status(result) mustBe OK
    }
  }
}
```

- Test both `Individual` and `Agent` user types using the `userTypeCases` table.
- Test unauthorised access paths.
- Test form validation failures (submit with invalid data → `BAD_REQUEST`).

---

## Integration Tests (`it/test/`)

Integration tests are **black-box HTTP tests**: they spin up a real Play server, stub all downstream HTTP calls with WireMock, and make real HTTP requests via `WSClient`.

### Key Infrastructure

| Class/trait | Purpose |
|---|---|
| `WiremockSpec` | Starts/stops `WireMockServer` on port `11111`; overrides service config to point all connectors at WireMock; runs one server per suite (`GuiceOneServerPerSuite`) |
| `IntegrationBaseSpec` | Extends `PlaySpec` + `GuiceOneServerPerSuite`; provides `buildClient`, `DbHelper`, common test data |
| `AuthStub` | WireMock stubs for the auth `/auth/authorise` endpoint |
| `AnswersApiStub` | WireMock stubs for the answers API (GET/PUT/DELETE) |
| `SelfEmploymentApiStub` | WireMock stubs for the self-employment backend |
| `SessionCookieHelper` | Bakes a signed session cookie for the test request |

### Structure

Every integration test class should:

1. Extend both `WiremockSpec` and `IntegrationBaseSpec`.
2. Test at least: the happy path for authenticated Individual, authenticated Agent, and unauthenticated (expect redirect to login).
3. Use `buildClient(url)` or `buildClient(url, isAgent = true)` for requests.
4. Use `DbHelper.insertEmpty()` (or `DbHelper.insertOne(...)`) to seed MongoDB state.
5. Use `AuthStub.authorised()` / `AuthStub.agentAuthorised()` / `AuthStub.unauthorisedOtherEnrolment()` to control auth.
6. Stub all downstream API calls before making the HTTP request.
7. Assert on `result.status` and, where appropriate, response body / `Location` header.

### Mandatory Coverage Rule

> **Every route defined in the `conf/*.routes` files must have at least one integration test** that:
> - Makes a real HTTP request (GET or POST) to the route URL.
> - Has all outbound HTTP calls stubbed via WireMock.
> - Asserts the HTTP response status code.
> - Runs under an authenticated session.

### Example Integration Test

```scala
package controllers.journeys.expenses.travelAndAccommodation

import base.IntegrationBaseSpec
import helpers.{AnswersApiStub, AuthStub, WiremockSpec}
import models.NormalMode
import models.common.Journey.ExpensesVehicleDetails
import models.common.JourneyAnswersContext
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.Helpers._

class MyJourneyControllerISpec extends WiremockSpec with IntegrationBaseSpec {

  lazy val url: String = routes.MyJourneyController.onPageLoad(taxYear, businessId, index, NormalMode).url
  val ctx: JourneyAnswersContext = JourneyAnswersContext(taxYear, nino, businessId, mtditid, ExpensesVehicleDetails)

  "GET /my-journey-page" when {
    "the user is an Individual" must {
      "return OK with the correct view" in {
        AuthStub.authorised()
        AnswersApiStub.getIndex(ctx, index = 1)(OK, Some(Json.toJson(testVehicleDetails)))
        DbHelper.insertEmpty()

        val result = await(buildClient(url).get())

        result.status mustBe OK
        result.header(HeaderNames.LOCATION) mustBe None
      }
    }

    "the user is an Agent" must {
      "return OK with the correct view" in {
        AuthStub.agentAuthorised()
        AnswersApiStub.getIndex(ctx, index = 1)(OK, Some(Json.toJson(testVehicleDetails)))
        DbHelper.insertEmpty()

        val result = await(buildClient(url, isAgent = true).get())

        result.status mustBe OK
      }
    }

    "the user is unauthorised" must {
      "redirect to the login page" in {
        AuthStub.unauthorisedOtherEnrolment()
        DbHelper.insertEmpty()

        val result = await(buildClient(url).get())

        result.status mustBe SEE_OTHER
        result.header(HeaderNames.LOCATION).exists(_.contains("gg-sign-in")) mustBe true
      }
    }
  }

  "POST /my-journey-page" when {
    "the user submits a valid form" must {
      "redirect to the next page" in {
        AuthStub.authorised()
        AnswersApiStub.getIndex(ctx, index = 1)(OK, Some(Json.toJson(testVehicleDetails)))
        AnswersApiStub.replaceIndex(ctx, Json.toJson(testVehicleDetails), index = 1)(OK)
        DbHelper.insertEmpty()

        val result = await(
          buildClient(routes.MyJourneyController.onSubmit(taxYear, businessId, index, NormalMode).url)
            .post(Map("value" -> "true"))
        )

        result.status mustBe SEE_OTHER
      }
    }

    "the user submits an invalid form" must {
      "return BAD_REQUEST" in {
        AuthStub.authorised()
        AnswersApiStub.getIndex(ctx, index = 1)(OK, Some(Json.toJson(testVehicleDetails)))
        DbHelper.insertEmpty()

        val result = await(
          buildClient(routes.MyJourneyController.onSubmit(taxYear, businessId, index, NormalMode).url)
            .post(Map.empty[String, String])
        )

        result.status mustBe BAD_REQUEST
      }
    }
  }
}
```

### WireMock Tips

- All connected services route through port `11111` in tests (configured in `WiremockSpec.servicesToUrlConfig`).
- Use the stub helpers in `it/test/helpers/` rather than calling `WireMock.stubFor` directly.
- `AuthStub` stubs `/auth/authorise`. Ensure it is called **before** `buildClient(...)`.
- Clean up via `DbHelper.teardown` happens automatically in `afterEach` via `IntegrationBaseSpec`.
- Use `AnswersApiStub.replaceIndex` / `AnswersApiStub.replaceAnswers` to stub PUT calls on form submission.

---

## Adding a New Page

Follow this checklist when adding a new journey page:

1. **Model** — add `case class`/`sealed trait` in `app/models/journeys/<area>/`. Add `Format` in the companion object.
2. **Page object** — add `object MyPage extends QuestionPage[MyType]` in `app/pages/<area>/`.
3. **Form provider** — add `class MyFormProvider` in `app/forms/<area>/`. Unit test it in `test/forms/<area>/`.
4. **View** — add `MyView.scala.html` in `app/views/journeys/<area>/`. Use `Layout`, component library, and `ErrorSummarySection`.
5. **Controller** — add `MyController` in `app/controllers/journeys/<area>/`. Thin; delegate to service/`answersService`.
6. **Navigator** — add a case for `MyPage` in the relevant Navigator.
7. **Route** — add `GET`/`POST` entries to the appropriate `conf/app*.routes` file.
8. **Messages** — add all string keys to both `conf/messages.en` and `conf/messages.cy`.
9. **Unit tests** — add controller unit tests in `test/controllers/journeys/<area>/MyControllerSpec.scala`.
10. **Integration test** — add `it/test/controllers/journeys/<area>/MyControllerISpec.scala` covering GET, POST valid, POST invalid, and unauthorised.

---

## Common Pitfalls

- **Do not** use `scala.concurrent.blocking` or `Thread.sleep` in tests — use `ScalaFutures.whenReady` or `.futureValue`.
- **Do not** hardcode URLs in tests — always use reverse routing.
- **Do not** call `app.injector.instanceOf[...]` in unit tests — use `applicationBuilder().build()` or `TestScenario`.
- **Do not** forget the CSRF token in integration test POST requests — `buildClient` adds `"Csrf-Token" -> "nocheck"` automatically.
- **Do not** test private methods directly — test observable behaviour via the public API.
- **Do not** return `Future.failed(exception)` from controllers — use `ErrorHandler` and proper `EitherT` chains.
- **Do not** import from `org.mockito.IdiomaticMockito` or `org.mockito.ArgumentMatchersSugar` — these are from the removed `mockito-scala` library. Use `base.IdiomaticMockitoCompat` and `base.ArgumentMatchersSugar` instead.
- **Do not** use `unlift(MyClass.unapply)` in Play JSON `Writes` — Scala 3 `unapply` has a different return type. Use an explicit lambda instead.
- **Do not** use `.unapply` as the unbind function in `Form` mappings — use `m => Some(Tuple.fromProductTyped(m))`.
- **Do not** use `enum` as a variable/parameter name — it is a reserved keyword in Scala 3.
- **Always** use parenthesised lambda parameters for type-annotated closures: `{ (x: Type) => ... }`.
- **Always** use explicit `()` for no-arg method calls in routes and reverse routing.
- **Always** stub Auth before making requests in integration tests — failing to do so results in a 500 from the auth action.
- **Always** clear WireMock stubs between tests — `WiremockSpec` does this in `afterEach`.
