# basm-admission-checker
Checks movements into prison match with admissions in NOMIS


called with:

Find Admission into Leeds prison on 19 Feb 2021
```http request
prison-movements/id/LEI?dateFrom=2021-02-19&dateTo=2021-02-19
```


Environment Variables:

```dotenv
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8081

PRISONER_SEARCH_ENDPOINT_URL=https://prisoner-offender-search.prison.service.justice.gov.uk
OFFENDER_SEARCH_ENDPOINT_URL=https://probation-offender-search.hmpps.service.justice.gov.uk
PRISON_ENDPOINT_URL=https://api.prison.service.justice.gov.uk
OAUTH_ENDPOINT_URL=https://sign-in.hmpps.service.justice.gov.uk/auth
BASM_ENDPOINT_URL=https://api.bookasecuremove.service.justice.gov.uk

hmpps-auth.client.client-id=********
hmpps-auth.client.client-secret=********
basm.client.client-id=********
basm.client.client-secret=*******

```
