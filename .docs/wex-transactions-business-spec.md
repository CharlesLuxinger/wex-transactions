# WEX Challenge — Business Specification

## Purpose
Define business requirements for storing purchase transactions in USD and retrieving converted values using Treasury reporting rates, translated to this project constraints.

## Business Scope
### In Scope
- Store a purchase transaction with:
  - description
  - transaction date
  - purchase amount (USD)
- Assign and persist a unique identifier for each stored purchase.
- Retrieve a previously stored purchase converted to a target currency.
- Use Treasury Reporting Rates of Exchange as the only currency source (treasury-only):
  - https://fiscaldata.treasury.gov/datasets/treasury-reporting-rates-exchange/treasury-reporting-rates-of-exchange
- Use the exchange rate active for the purchase date with historical fallback constraints.

### Out of Scope
- Task breakdown or implementation sequencing.
- API endpoint contracts.
- Performance/non-functional test automation requirements.
- Alternative exchange-rate providers.

## Business Rules
### 1) Store Purchase Transaction
A purchase transaction must be accepted and persisted with a generated unique identifier.

#### Field Rules
- Description:
  - Required.
  - Maximum length: 50 characters.
- Transaction date:
  - Required.
  - Must be a valid date format.
- Purchase amount (USD):
  - Required.
  - Must be positive.
  - Must be rounded to nearest cent (2 decimal places).
- Unique identifier:
  - Must uniquely identify the purchase.
  - Must be returned after successful storage.

### 2) Retrieve Purchase in Target Currency
The system must return stored purchase data converted to a specified target currency.

#### Conversion Rules
- Currency source policy: treasury-only.
- Conversion must use a rate with date **less than or equal** to the purchase date.
- Exact date match is not required.
- Eligible historical window: rate must exist within the prior 6 months (inclusive) from purchase date.
- If no eligible rate exists in this window, conversion must fail with business error.
- Converted amount must be rounded to 2 decimal places.

#### Returned Business Data
The converted retrieval must include:
- purchase identifier
- description
- transaction date
- original USD amount
- exchange rate used
- converted amount in target currency

## Error Rules

- Invalid description length (>50).
- Invalid transaction date format.
- Invalid purchase amount (missing, non-numeric, zero, or negative).
- Conversion unavailable (no rate <= purchase date within last 6 months).

## Quality and Compliance Constraints
- Documentation in this file remains business-level.
- Coverage policy to enforce during implementation and verification:
  - overall coverage threshold and changed-files threshold from CI
  - domain-100 rule from Gradle coverage verification is mandatory

## Acceptance Criteria
- Business specification clearly defines transaction storage rules and conversion rules.
- treasury-only policy is explicit.
- Six-month historical conversion eligibility rule is explicit.
- Minimal error taxonomy is explicit and limited.
- Coverage requirement includes domain-100 rule.
- Document contains no implementation/API contract details.
