# basm-admission-checker
Checks movements into prison match with admissions in NOMIS and records existing in Delius and Nomis

called with:

Find Admission into Leeds prison on 19 Feb 2021
```http request
prison-movements/id/LEI?dateFrom=2021-02-19&dateTo=2021-02-19
```

### Steps:

1. Get all movements IN to prison on the date specified
2. Get all movements IN to prison from Book a secure move API
3. Look in probation search to find matches by name, dob, PNC, CRO
4. Look in prisoner search to find matches by name, dob, prisoner number, PNC and CRO
5. Report where there a missing BaSM records and missing NOMIS admissions

Some mismatches are:
1. PNC invalid in BASM
2. DOB is incorrect or obviously wrong (e.g 01.01.1900)
3. Prisoners to be moved to one prison but end up in other at the end of the day
4. Not all admission movements are captured in BaSM

## Record Structure

```
{
  prisonId": "LEI",
  "movements": [
    {
    "foundInBasm": true,
    "foundInNomis": true,
    "foundInDelius": false,
    "matchedAdmission": true,
    "nomisRecords": []
    "basmRecord": {}
    "deliusRecord": [],
    }
  ]
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
