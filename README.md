# income-tax-benefits

This is where we make API calls from users viewing and making changes to the employment benefits and state benefits
sections of their income tax return.

## Running the service locally

You will need to have the following:

- Installed [MongoDB](https://www.mongodb.com/docs/manual/installation/)
- Installed/configured [service manager 2](https://github.com/hmrc/sm2).

The service manager profile for this service is:

    sm2 --start INCOME_TAX_BENEFITS

Run the following command to start the remaining services locally:

    sudo mongod (If not already running)
    sm2 --start INCOME_TAX_SUBMISSION_ALL

This service runs on port: `localhost:9319`

### Employment benefits endpoints:

- **GET /income-tax/nino/:nino/sources/:employmentId** (Gets either hmrc benefits or customer benefits data for a
  particular employment. Also referred to as benefits in kind.)

### State benefits endpoints:

- **GET /state-benefits/nino/:nino/taxYear/:taxYear** (Gets all state benefits data or optionally data for specific
  benefit)

- **POST /state-benefits/nino/:nino/taxYear/:taxYear** (Creates a new customer state benefit)

- **PUT /state-benefits/nino/:nino/taxYear/:taxYear/benefitId/:benefitId** (Updates a state benefit)

- **PUT /state-benefits/override/nino/:nino/taxYear/:taxYear/benefitId/:benefitId** (Updates a customer state benefit)

- **DELETE /state-benefits/nino/:nino/taxYear/:taxYear/benefitId/:benefitId** (Deletes a state benefit)

- **DELETE /state-benefits/override/nino/:nino/taxYear/:taxYear/benefitId/:benefitId** (Deletes customer added state
  benefit)

- **PUT /state-benefits/nino/:nino/taxYear/:taxYear/benefitId/:benefitId/ignoreBenefit/:ignoreBenefit** (Ignores a hmrc
  state benefit)

- **DELETE /state-benefits/nino/:nino/taxYear/:taxYear/ignore/benefitId/:benefitId** (Unignores a hmrc state benefit)

### Downstream services

All benefits data is retrieved / updated via one of two downstream systems.

- DES (Data Exchange Service)
- IF (Integration Framework)

### Employment Sources (HMRC-Held and Customer Data)

Employment data can come from different sources: HMRC-Held and Customer. HMRC-Held data is employment data that HMRC
have for the user within the tax year, prior to any updates made by the user. The employment data displayed in-year is
HMRC-Held. This is the same for benefits data, you can have hmrc held benefits and/or customer benefits data.

Customer data is provided by the user. At the end of the tax year, users can view any existing employment data and make
changes (create, update and delete).

<details>
<summary>Click here to see an example of a user with HMRC-Held and Customer data (JSON)</summary>

```json
{
  "employment": [
    {
      "taxYear": 2022,
      "hmrcEmployments": [
        {
          "employmentId": "00000000-0000-1000-8000-000000000000",
          "employerName": "Vera Lynn",
          "employerRef": "123/12345",
          "payrollId": "123345657",
          "startDate": "2020-06-17",
          "cessationDate": "2020-06-17",
          "dateIgnored": "2020-06-17T10:53:38Z",
          "employmentData": {
            "submittedOn": "2020-01-04T05:01:01Z",
            "source": "HMRC-HELD",
            "employment": {
              "employmentSequenceNumber": "1002",
              "payrollId": "123456789999",
              "companyDirector": false,
              "closeCompany": true,
              "directorshipCeasedDate": "2020-02-12",
              "startDate": "2019-04-21",
              "cessationDate": "2020-03-11",
              "occPen": false,
              "disguisedRemuneration": false,
              "employer": {
                "employerRef": "223/AB12399",
                "employerName": "maggie"
              },
              "pay": {
                "taxablePayToDate": 34234.15,
                "totalTaxToDate": 6782.92,
                "payFrequency": "CALENDAR MONTHLY",
                "paymentDate": "2020-04-23",
                "taxWeekNo": 32
              },
              "deductions": {
                "studentLoans": {
                  "uglDeductionAmount": 13343.45,
                  "pglDeductionAmount": 24242.56
                }
              },
              "benefitsInKind": {
                "accommodation": 100,
                "assets": 100,
                "assetTransfer": 100,
                "beneficialLoan": 100,
                "car": 100,
                "carFuel": 100,
                "educationalServices": 100,
                "entertaining": 100,
                "expenses": 100,
                "medicalInsurance": 100,
                "telephone": 100,
                "service": 100,
                "taxableExpenses": 100,
                "van": 100,
                "vanFuel": 100,
                "mileage": 100,
                "nonQualifyingRelocationExpenses": 100,
                "nurseryPlaces": 100,
                "otherItems": 100,
                "paymentsOnEmployeesBehalf": 100,
                "personalIncidentalExpenses": 100,
                "qualifyingRelocationExpenses": 100,
                "employerProvidedProfessionalSubscriptions": 100,
                "employerProvidedServices": 100,
                "incomeTaxPaidByDirector": 100,
                "travelAndSubsistence": 100,
                "vouchersAndCreditCards": 100,
                "nonCash": 100
              }
            }
          }
        }
      ],
      "customerEmployments": [
        {
          "employmentId": "00000000-0000-1000-8000-000000000002",
          "employerName": "Vera Lynn",
          "employerRef": "123/12345",
          "payrollId": "123345657",
          "startDate": "2020-06-17",
          "cessationDate": "2020-06-17",
          "submittedOn": "2020-06-17T10:53:38Z",
          "employmentData": {
            "submittedOn": "2020-02-04T05:01:01Z",
            "employment": {
              "employmentSequenceNumber": "1002",
              "payrollId": "123456789999",
              "companyDirector": false,
              "closeCompany": true,
              "directorshipCeasedDate": "2020-02-12",
              "startDate": "2019-04-21",
              "cessationDate": "2020-03-11",
              "occPen": false,
              "disguisedRemuneration": false,
              "employer": {
                "employerRef": "223/AB12399",
                "employerName": "maggie"
              },
              "pay": {
                "taxablePayToDate": 34234.15,
                "totalTaxToDate": 6782.92,
                "payFrequency": "CALENDAR MONTHLY",
                "paymentDate": "2020-04-23",
                "taxWeekNo": 32
              },
              "deductions": {
                "studentLoans": {
                  "uglDeductionAmount": 13343.45,
                  "pglDeductionAmount": 24242.56
                }
              },
              "benefitsInKind": {
                "accommodation": 100,
                "assets": 100,
                "assetTransfer": 100,
                "beneficialLoan": 100,
                "car": 100,
                "carFuel": 100,
                "educationalServices": 100,
                "entertaining": 100,
                "expenses": 100,
                "medicalInsurance": 100,
                "telephone": 100,
                "service": 100,
                "taxableExpenses": 100,
                "van": 100,
                "vanFuel": 100,
                "mileage": 100,
                "nonQualifyingRelocationExpenses": 100,
                "nurseryPlaces": 100,
                "otherItems": 100,
                "paymentsOnEmployeesBehalf": 100,
                "personalIncidentalExpenses": 100,
                "qualifyingRelocationExpenses": 100,
                "employerProvidedProfessionalSubscriptions": 100,
                "employerProvidedServices": 100,
                "incomeTaxPaidByDirector": 100,
                "travelAndSubsistence": 100,
                "vouchersAndCreditCards": 100,
                "nonCash": 100
              }
            }
          }
        }
      ],
      "employmentExpenses": {
        "submittedOn": "2022-12-12T12:12:12Z",
        "dateIgnored": "2022-12-11T12:12:12Z",
        "source": "HMRC-HELD",
        "totalExpenses": 100,
        "expenses": {
          "businessTravelCosts": 100,
          "jobExpenses": 100,
          "flatRateJobExpenses": 100,
          "professionalSubscriptions": 100,
          "hotelAndMealExpenses": 100,
          "otherAndCapitalAllowances": 100,
          "vehicleExpenses": 100,
          "mileageAllowanceRelief": 100
        }
      }
    }
  ]
}
```

</details>

### State benefits (HMRC-Held and Customer Data)

State benefits data can come from different sources: HMRC-Held and Customer. HMRC-Held data is state benefits data that
HMRC have for the user within the tax year, prior to any updates made by the user.

Customer data is provided by the user.

<details>
<summary>Click here to see an example of a user with HMRC-Held and Customer state benefits data (JSON)</summary>

```json
{
  "stateBenefits": {
    "incapacityBenefit": [
      {
        "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c934",
        "startDate": "2019-11-13",
        "dateIgnored": "2019-04-11T16:22:00Z",
        "submittedOn": "2020-09-11T17:23:00Z",
        "endDate": "2020-08-23",
        "amount": 1212.34,
        "taxPaid": 22323.23
      }
    ],
    "statePension": {
      "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c934",
      "startDate": "2019-11-13",
      "dateIgnored": "2019-04-11T16:22:00Z",
      "submittedOn": "2020-09-11T17:23:00Z",
      "endDate": "2020-08-23",
      "amount": 1212.34,
      "taxPaid": 22323.23
    },
    "statePensionLumpSum": {
      "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c934",
      "startDate": "2019-11-13",
      "dateIgnored": "2019-04-11T16:22:00Z",
      "submittedOn": "2020-09-11T17:23:00Z",
      "endDate": "2020-08-23",
      "amount": 1212.34,
      "taxPaid": 22323.23
    },
    "employmentSupportAllowance": [
      {
        "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c934",
        "startDate": "2019-11-13",
        "dateIgnored": "2019-04-11T16:22:00Z",
        "submittedOn": "2020-09-11T17:23:00Z",
        "endDate": "2020-08-23",
        "amount": 1212.34,
        "taxPaid": 22323.23
      }
    ],
    "jobSeekersAllowance": [
      {
        "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c934",
        "startDate": "2019-11-13",
        "dateIgnored": "2019-04-11T16:22:00Z",
        "submittedOn": "2020-09-11T17:23:00Z",
        "endDate": "2020-08-23",
        "amount": 1212.34,
        "taxPaid": 22323.23
      }
    ],
    "bereavementAllowance": {
      "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c934",
      "startDate": "2019-11-13",
      "dateIgnored": "2019-04-11T16:22:00Z",
      "submittedOn": "2020-09-11T17:23:00Z",
      "endDate": "2020-08-23",
      "amount": 1212.34,
      "taxPaid": 22323.23
    },
    "otherStateBenefits": {
      "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c934",
      "startDate": "2019-11-13",
      "dateIgnored": "2019-04-11T16:22:00Z",
      "submittedOn": "2020-09-11T17:23:00Z",
      "endDate": "2020-08-23",
      "amount": 1212.34,
      "taxPaid": 22323.23
    }
  },
  "customerAddedStateBenefits": {
    "incapacityBenefit": [
      {
        "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c934",
        "startDate": "2018-07-17",
        "submittedOn": "2020-11-17T19:23:00Z",
        "endDate": "2020-09-23",
        "amount": 45646.78,
        "taxPaid": 4544.34
      }
    ],
    "statePension": {
      "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c934",
      "startDate": "2018-07-17",
      "submittedOn": "2020-11-17T19:23:00Z",
      "endDate": "2020-09-23",
      "amount": 45646.78,
      "taxPaid": 4544.34
    },
    "statePensionLumpSum": {
      "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c934",
      "startDate": "2018-07-17",
      "submittedOn": "2020-11-17T19:23:00Z",
      "endDate": "2020-09-23",
      "amount": 45646.78,
      "taxPaid": 4544.34
    },
    "employmentSupportAllowance": [
      {
        "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c934",
        "startDate": "2018-07-17",
        "submittedOn": "2020-11-17T19:23:00Z",
        "endDate": "2020-09-23",
        "amount": 45646.78,
        "taxPaid": 4544.34
      }
    ],
    "jobSeekersAllowance": [
      {
        "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c934",
        "startDate": "2018-07-17",
        "submittedOn": "2020-11-17T19:23:00Z",
        "endDate": "2020-09-23",
        "amount": 45646.78,
        "taxPaid": 4544.34
      }
    ],
    "bereavementAllowance": {
      "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c934",
      "startDate": "2018-07-17",
      "submittedOn": "2020-11-17T19:23:00Z",
      "endDate": "2020-09-23",
      "amount": 45646.78,
      "taxPaid": 4544.34
    },
    "otherStateBenefits": {
      "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c934",
      "startDate": "2018-07-17",
      "submittedOn": "2020-11-17T19:23:00Z",
      "endDate": "2020-09-23",
      "amount": 45646.78,
      "taxPaid": 4544.34
    }
  }
}
```

</details>

## Ninos with stub data for employment

### In-Year

| Nino      | Employment data                                                  | Source    |
|-----------|------------------------------------------------------------------|-----------|
| AA133742A | Single employment - Employment details, benefits and expenses    | HMRC-Held |
| BB444444A | Multiple employments - Employment details, benefits and expenses | HMRC-Held |

### End of Year

| Nino      | Employment data                                                  | Source              
|-----------|------------------------------------------------------------------|---------------------|
| AA133742A | Single employment - Employment details and benefits              | HMRC-Held, Customer |
| BB444444A | Multiple employments - Employment details, benefits and expenses | HMRC-Held, Customer |

### License

This code is open source software licensed under
the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
