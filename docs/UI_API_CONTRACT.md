# Office UI API contract

Use this document to rebuild the company-laptop UI against `global-recon-service`. The UI talks **only** to this service. Vendor APIs (Aladdin, BNY, SSB, and so on) are never called from the browser.

Base URL (local): `http://localhost:8080`

Every `/api/**` call must send:

```
X-User-Email: first.last@company.com
```

The value is trimmed and lowercased. It is the identity for ownership and collection ACL. There is no session cookie and no JWT in this service.

Interactive OpenAPI: `http://localhost:8080/swagger-ui.html`

---

## Product surfaces

There are two products in one API.

1. **Workspace (lab)** — upload or ingest two datasets, discover a mapping, approve a plan, run one reconciliation, inspect breaks.
2. **Collections (production recon type)** — after an approved plan whose datasets both came from configured sources, save a named recon type, attach a fund crosswalk, and fan out T-1/T-2/calendar cycles.

Do not mix them in the UI: a collection is created from an approved plan; a cycle does not re-run discovery.

---

## Conventions

### Errors

JSON body:

```json
{ "error": "INVALID_REQUEST", "message": "human readable" }
```

| HTTP | `error` | When |
|------|---------|------|
| 401 | `UNAUTHENTICATED` | Missing/invalid `X-User-Email` |
| 400 | `INVALID_REQUEST` | Validation, disabled source, unknown param, cycle already running |
| 404 | `NOT_FOUND` | Missing id, or collection the caller cannot see (edit of others' collections is also 404) |
| 422 | `DISCOVERY_FAILED` | LLM mapping discovery failed |
| 502 | `SOURCE_FETCH_FAILED` | Configured vendor HTTP failed |
| 500 | `INTERNAL_ERROR` | Unexpected |

### Spring `Page` (list endpoints that paginate)

```json
{
  "content": [ ],
  "totalElements": 0,
  "totalPages": 0,
  "size": 20,
  "number": 0,
  "first": true,
  "last": true,
  "empty": true
}
```

Query params: `page` (0-based), `size`. Extra Spring fields (`pageable`, `sort`) may also be present; ignore them.

### Instants and dates

- Timestamps: ISO-8601 instants, e.g. `"2026-10-04T21:09:00Z"`
- Collection `asOfDate`: ISO date `"2026-10-03"`
- Default T-1 / T-2 uses timezone `Asia/Kolkata` (`recon.retention.zone`)

### Job polling

Any `202 Accepted` response is a `ReconJob`. Poll `GET /api/v1/jobs/{jobId}` until `status` is `COMPLETED` or `FAILED`.

| `type` | When `COMPLETED`, `resultId` is |
|--------|----------------------------------|
| `MAPPING_DISCOVERY` | recon plan id |
| `RECON_RUN` | recon run id |
| `COLLECTION_CYCLE` | collection cycle id |
| `COLLECTION_ITEM` | collection item id (internal; UI usually polls the cycle, not each item job) |

Item fetch failures mark the **item** `FAILED` but the item **job** still completes. Cycle `status` becomes `PARTIAL` or `FAILED`.

---

## Enums

```
DatasetFormat:        CSV | JSON | XLSX
DatasetStatus:        UPLOADING | NORMALIZED | PROFILED | FAILED
DatasetSourceKind:    FILE | SOURCE
DataType:             STRING | INTEGER | DECIMAL | DATE | DATETIME | BOOLEAN
ReconPlanStatus:      DRAFT | APPROVED | DISCOVERY_FAILED
MatchType:            EXACT | CASE_INSENSITIVE | DATE_NORMALIZED | NUMERIC_TOLERANCE
ReconRunStatus:       PENDING | RUNNING | COMPLETED | FAILED
ReconStatus:          MATCHED | BREAK | ONLY_IN_LEFT | ONLY_IN_RIGHT | DUPLICATE_LEFT | DUPLICATE_RIGHT | AMBIGUOUS
JobType:              MAPPING_DISCOVERY | RECON_RUN | COLLECTION_CYCLE | COLLECTION_ITEM
JobStatus:            QUEUED | RUNNING | COMPLETED | FAILED
SourceAuthType:       NONE | BEARER | HEADER
CollectionDatePolicy: T1 | T2 | CALENDAR
CollectionMemberRole: OWNER | VIEWER
CollectionCycleStatus: QUEUED | RUNNING | COMPLETED | FAILED | PARTIAL
CollectionItemFetchStatus: PENDING | OK | FAILED
```

The engine, not the LLM, decides `MATCHED` / `BREAK`. The UI must not invent those statuses.

---

## 1. Sources (configured APIs)

Catalog is server-side. UI lists sources, collects params from `params[]`, then calls ingest. Never show `baseUrl` or secrets.

### `GET /api/v1/sources`

`200` `SourceView[]`

### `GET /api/v1/sources/{sourceId}`

`200` `SourceView`

### `POST /api/v1/sources/ingest` → `201`

Request:

```json
{
  "sourceId": "src-aladdin-positions",
  "name": "Aladdin FUNDX 2026-10-03",
  "recordPath": "positions",
  "params": {
    "fundId": "FUNDX",
    "asOfDate": "2026-10-03"
  }
}
```

| Field | Required | Notes |
|-------|----------|--------|
| sourceId | yes | Enabled catalog id |
| name | no | Dataset display name |
| recordPath | no | Overrides source default JSON array path |
| params | no | Only names listed in `SourceView.params`. Required params must be non-blank |

`201` body: `Dataset` (see datasets). `sourceKind` is `SOURCE`. Typical size is 100–1200 positions; ingest still uses the 10k chunk pipeline.

### `SourceView`

```json
{
  "id": "src-aladdin-positions",
  "name": "Aladdin positions",
  "vendor": "ALADDIN",
  "path": "/v1/funds/{fundId}/positions",
  "httpMethod": "GET",
  "recordPath": "positions",
  "authType": "BEARER",
  "params": [
    {
      "name": "fundId",
      "label": "Fund id",
      "required": true,
      "kind": "STRING",
      "roleHint": "IDENTITY"
    },
    {
      "name": "asOfDate",
      "label": "As of date",
      "required": true,
      "kind": "DATE",
      "roleHint": "DATE"
    }
  ]
}
```

`roleHint` is a UI hint (`IDENTITY`, `DATE`, `CONSTANT`). Path tokens `{fundId}` are filled from `params`; leftover params become query string.

Company laptop: rows in table `source` must already exist (ops seed). The UI does not create sources.

---

## 2. Datasets

### `POST /api/v1/datasets` `multipart/form-data` → `201`

Parts/params:

| Name | In | Required |
|------|----|----------|
| file | part | yes (CSV, JSON, XLSX) |
| name | query | no |
| notes | query | no, LLM ingest notes, max 4000 |
| recordPath | query | no, e.g. `tradeList` or `[].order` |

`201` `Dataset`

### `GET /api/v1/datasets`

`200` `Dataset[]` owned by the caller, newest first.

### `GET /api/v1/datasets/{datasetId}`

`200` `Dataset`

### `GET /api/v1/datasets/{datasetId}/profile`

`200` `DatasetProfile`

### `GET /api/v1/datasets/{datasetId}/records?page=0&size=20`

`200` `Page<DatasetRecordView>`

### `Dataset`

```json
{
  "id": "ds_...",
  "name": "Aladdin FUNDX 2026-10-03",
  "originalFilename": "aladdin.json",
  "format": "JSON",
  "status": "PROFILED",
  "rowCount": 412,
  "errorMessage": null,
  "ownerEmail": "you@company.com",
  "ingestionNotes": null,
  "recordPath": "positions",
  "sourceKind": "SOURCE",
  "sourceId": "src-aladdin-positions",
  "sourceParams": { "fundId": "FUNDX", "asOfDate": "2026-10-03" },
  "createdAt": "2026-10-04T18:00:00Z",
  "updatedAt": "2026-10-04T18:00:01Z"
}
```

File uploads have `sourceKind: "FILE"`, `sourceId: null`, empty `sourceParams`.

Wait until `status` is `PROFILED` (or `FAILED`) before discovery. Upload is synchronous in this service.

### `DatasetProfile`

```json
{
  "datasetId": "ds_...",
  "rowCount": 412,
  "columns": [
    {
      "columnName": "cusip",
      "type": "STRING",
      "nullCount": 0,
      "nullPercentage": 0,
      "distinctCount": 400,
      "uniqueRatio": 0.97,
      "sampleValues": ["912828"],
      "minimum": null,
      "maximum": null,
      "commonPatterns": []
    }
  ]
}
```

### `DatasetRecordView`

```json
{ "id": "drec_...", "rowIndex": 0, "payload": { "cusip": "912828", "mv": 1000000 } }
```

Payload datasets older than 7 days are purged nightly at 02:00 Asia/Kolkata, except datasets still referenced by a recon plan.

---

## 3. Mapping discovery and plans

### `POST /api/v1/recon-plans/discover` → `202`

```json
{
  "leftDatasetId": "ds_left",
  "rightDatasetId": "ds_right",
  "notes": "Match on cusip; ignore cash"
}
```

`notes` optional, max 4000.

`202` `ReconJob`. Poll until complete, then `GET /api/v1/recon-plans/{resultId}`.

### `GET /api/v1/recon-plans/{planId}`

`200` `ReconPlan`

### `PUT /api/v1/recon-plans/{planId}`

Request `UpdateReconPlanRequest`:

```json
{
  "leftDatasetId": "ds_left",
  "rightDatasetId": "ds_right",
  "userNotes": "Use ticker if cusip missing",
  "approve": false,
  "keyMappings": [
    { "leftField": "cusip", "rightField": "Cusip", "confidence": 0.99 }
  ],
  "fieldMappings": [
    {
      "leftField": "mv",
      "rightField": "MarketValue",
      "matchType": "NUMERIC_TOLERANCE",
      "tolerance": 0.01,
      "confidence": 0.9,
      "included": true
    }
  ]
}
```

`approve: true` approves in the same save. `200` `ReconPlan`

### `POST /api/v1/recon-plans/{planId}/approve`

`200` `ReconPlan` with `status: APPROVED`

### `ReconPlan`

```json
{
  "id": "plan_...",
  "leftDatasetId": "ds_left",
  "rightDatasetId": "ds_right",
  "ownerEmail": "you@company.com",
  "userNotes": "...",
  "status": "APPROVED",
  "overallConfidence": 0.91,
  "warnings": "[\"cash rows ignored\"]",
  "createdAt": "2026-10-04T18:01:00Z",
  "approvedAt": "2026-10-04T18:05:00Z",
  "keyMappings": [
    { "id": "kmap_...", "leftField": "cusip", "rightField": "Cusip", "confidence": 0.99, "sortOrder": 0 }
  ],
  "fieldMappings": [
    {
      "id": "fmap_...",
      "leftField": "mv",
      "rightField": "MarketValue",
      "matchType": "NUMERIC_TOLERANCE",
      "tolerance": 0.01,
      "confidence": 0.9,
      "sortOrder": 0,
      "included": true
    }
  ]
}
```

`warnings` is a JSON **string**, not an array. `plan` nested on mappings is omitted.

A collection can be created only when the plan is `APPROVED` and **both** datasets have `sourceKind: SOURCE`. If either side was a file upload, keep the user on workspace recon only.

---

## 4. Workspace recon runs

### `POST /api/v1/recon-runs` → `202`

```json
{ "reconPlanId": "plan_..." }
```

Uses the plan's original left/right datasets. `202` `ReconJob`; `resultId` is the run id after completion.

### `GET /api/v1/recon-runs/{runId}`

`200` `ReconRun`

### `GET /api/v1/recon-runs/{runId}/summary`

`200` map of counts:

```json
{
  "MATCHED": 400,
  "BREAK": 8,
  "ONLY_IN_LEFT": 2,
  "ONLY_IN_RIGHT": 1,
  "DUPLICATE_LEFT": 0,
  "DUPLICATE_RIGHT": 0,
  "AMBIGUOUS": 0,
  "PROCESSED": 411
}
```

### `GET /api/v1/recon-runs/{runId}/results?status=BREAK&page=0&size=50`

`status` optional `ReconStatus`. `200` `Page<ReconResult>`

### `ReconRun`

```json
{
  "id": "run_...",
  "reconPlanId": "plan_...",
  "leftDatasetId": "ds_left",
  "rightDatasetId": "ds_right",
  "ownerEmail": "you@company.com",
  "status": "COMPLETED",
  "matchedCount": 400,
  "breakCount": 8,
  "onlyInLeftCount": 2,
  "onlyInRightCount": 1,
  "duplicateLeftCount": 0,
  "duplicateRightCount": 0,
  "ambiguousCount": 0,
  "processedCount": 411,
  "name": null,
  "savedAt": null,
  "errorMessage": null,
  "startedAt": "2026-10-04T18:06:00Z",
  "completedAt": "2026-10-04T18:06:04Z"
}
```

### `ReconResult`

```json
{
  "id": "rr_...",
  "runId": "run_...",
  "status": "BREAK",
  "leftRecordId": "drec_l",
  "rightRecordId": "drec_r",
  "reconKey": "912828",
  "leftPayload": { "cusip": "912828", "mv": 100 },
  "rightPayload": { "Cusip": "912828", "MarketValue": 101 },
  "differences": [
    {
      "leftField": "mv",
      "rightField": "MarketValue",
      "leftValue": "100",
      "rightValue": "101"
    }
  ]
}
```

---

## 5. Saved comparisons

Workspace convenience: name a completed run.

### `POST /api/v1/comparisons`

```json
{ "runId": "run_...", "name": "October Aladdin vs BNY" }
```

`200` `ComparisonSummary`

### `GET /api/v1/comparisons?saved=true&page=0&size=50`

`saved` optional boolean. `200` `Page<ComparisonSummary>`

```json
{
  "runId": "run_...",
  "name": "October Aladdin vs BNY",
  "status": "COMPLETED",
  "reconPlanId": "plan_...",
  "leftDatasetId": "ds_left",
  "rightDatasetId": "ds_right",
  "leftDatasetName": "Aladdin",
  "rightDatasetName": "BNY",
  "matchedCount": 400,
  "breakCount": 8,
  "onlyInLeftCount": 2,
  "onlyInRightCount": 1,
  "startedAt": "...",
  "completedAt": "...",
  "savedAt": "..."
}
```

---

## 6. Jobs

### `GET /api/v1/jobs/{jobId}`

`200` `ReconJob` (caller must own the job)

### `GET /api/v1/jobs?status=RUNNING&page=0&size=50`

`status` optional. `200` `Page<ReconJob>`

### `ReconJob`

```json
{
  "id": "job_...",
  "type": "COLLECTION_CYCLE",
  "status": "RUNNING",
  "leftDatasetId": null,
  "rightDatasetId": null,
  "reconPlanId": null,
  "resultId": "ccyc_...",
  "ownerEmail": "you@company.com",
  "notes": null,
  "errorMessage": null,
  "createdAt": "...",
  "startedAt": "...",
  "completedAt": null,
  "collectionCycleId": "ccyc_...",
  "collectionItemId": null
}
```

---

## 7. Collections (recon types)

Owner can create/edit/run. Members (`VIEWER`) can list/get cycles and item results, not mutate.

### `POST /api/v1/collections` → `201`

```json
{
  "name": "Aladdin vs BNY positions",
  "planId": "plan_...",
  "leftIdentityParam": "fundId",
  "rightIdentityParam": "accountId",
  "leftDateParam": "asOfDate",
  "rightDateParam": "reportDate",
  "datePolicy": "T1",
  "scheduleCron": "0 0 6 * * *",
  "constantParams": { "region": "US" },
  "memberEmails": ["analyst@company.com"],
  "pairs": [
    { "leftValue": "FUNDX", "rightValue": "BNY-001" },
    { "leftValue": "FUNDY", "rightValue": "BNY-002" }
  ]
}
```

| Field | Required | Notes |
|-------|----------|--------|
| name, planId | yes | Plan must be APPROVED; both datasets SOURCE |
| left/right IdentityParam, DateParam | yes | Must be param names on that side's source |
| datePolicy | no | default `T1` |
| scheduleCron | no | stored for later scheduler; UI can display it |
| constantParams | no | merged into every fetch |
| memberEmails | no | viewers; owner is always added |
| pairs | yes | at least one; left values unique, right values unique |

`201` `ReconCollection`

### `GET /api/v1/collections`

`200` collections the user owns or is a member of.

### `GET /api/v1/collections/{collectionId}`

`200` `CollectionDetail`

### `PUT /api/v1/collections/{collectionId}`

Owner only. Omitted fields are left unchanged. `memberEmails: []` removes all viewers. `memberEmails` omitted does not change members.

```json
{
  "name": "Aladdin vs BNY positions",
  "datePolicy": "T2",
  "scheduleCron": "0 0 6 * * *",
  "memberEmails": ["analyst@company.com"]
}
```

`200` `ReconCollection`

### `PUT /api/v1/collections/{collectionId}/pairs`

Owner only. Replaces the whole crosswalk.

```json
[ { "leftValue": "FUNDX", "rightValue": "BNY-001" } ]
```

`200` `ReconCollection`

### `POST /api/v1/collections/{collectionId}/cycles` → `202`

Body optional:

```json
{ "asOfDate": "2026-10-03" }
```

- `T1`: default yesterday in `Asia/Kolkata` unless `asOfDate` sent
- `T2`: default minus 2 days unless `asOfDate` sent
- `CALENDAR`: `asOfDate` required

If a cycle for that date is already `RUNNING`, `400`. Re-running a finished date re-queues incomplete/failed items.

`202` `ReconJob` (`type: COLLECTION_CYCLE`). Then poll cycle:

### `GET /api/v1/collections/{collectionId}/cycles?page=0&size=20`

`200` `Page<CollectionCycle>` newest date first.

### `GET /api/v1/collections/{collectionId}/cycles/{cycleId}`

`200` `CollectionCycleDetail` (`cycle` + `items`)

### `GET /api/v1/collections/{id}/cycles/{cycleId}/items/{itemId}/summary`

Same map as recon-run summary. `400` if the item has no run yet.

### `GET /api/v1/collections/{id}/cycles/{cycleId}/items/{itemId}/results?status=BREAK&page=0&size=50`

Same `Page<ReconResult>` as workspace. Members may call this.

### `ReconCollection`

```json
{
  "id": "col_...",
  "name": "Aladdin vs BNY positions",
  "ownerEmail": "you@company.com",
  "planId": "plan_...",
  "leftSourceId": "src-aladdin-positions",
  "rightSourceId": "src-bny-positions",
  "leftIdentityParam": "fundId",
  "rightIdentityParam": "accountId",
  "leftDateParam": "asOfDate",
  "rightDateParam": "reportDate",
  "constantParams": { "region": "US" },
  "datePolicy": "T1",
  "scheduleCron": "0 0 6 * * *",
  "createdAt": "...",
  "updatedAt": "..."
}
```

### `CollectionDetail`

```json
{
  "collection": { },
  "members": [
    { "id": "cm_...", "collectionId": "col_...", "email": "you@company.com", "role": "OWNER" }
  ],
  "pairs": [
    { "id": "cp_...", "collectionId": "col_...", "leftValue": "FUNDX", "rightValue": "BNY-001", "sortOrder": 0 }
  ],
  "lastCycle": { }
}
```

`lastCycle` may be `null`.

### `CollectionCycle`

```json
{
  "id": "ccyc_...",
  "collectionId": "col_...",
  "asOfDate": "2026-10-03",
  "status": "PARTIAL",
  "jobId": "job_...",
  "itemCount": 40,
  "failedFetchCount": 1,
  "breakFundCount": 3,
  "startedAt": "...",
  "completedAt": "..."
}
```

`breakFundCount` = items with `breakCount > 0`.

### `CollectionItem`

```json
{
  "id": "citem_...",
  "cycleId": "ccyc_...",
  "pairId": "cp_...",
  "leftValue": "FUNDX",
  "rightValue": "BNY-001",
  "leftDatasetId": "ds_...",
  "rightDatasetId": "ds_...",
  "reconRunId": "run_...",
  "fetchStatus": "OK",
  "reconStatus": "COMPLETED",
  "matchedCount": 400,
  "breakCount": 2,
  "onlyInLeftCount": 0,
  "onlyInRightCount": 0,
  "errorMessage": null
}
```

Suggested cycle table columns: pair, `fetchStatus`, `reconStatus`, matched/break, error. Click a row with `reconRunId` to load item results.

---

## UI screens to build on the company laptop

Mirror existing office structure; wire these routes.

1. **Sources** — `GET /sources`, form from `params`, `POST /sources/ingest`, then dataset profile.
2. **Upload** — `POST /datasets` for file-based lab work.
3. **Discover** — pick two PROFILED datasets, `POST /recon-plans/discover`, poll job, mapping editor, save/approve.
4. **Run once** — `POST /recon-runs`, poll, summary + filterable results.
5. **Collections list** — `GET /collections`.
6. **Create collection** — only if both plan datasets are `SOURCE`. Crosswalk grid = `pairs`. Param names from `GET /sources/{id}`.
7. **Collection detail** — members, pairs, last cycle, Run now.
8. **Cycle** — poll `GET .../cycles/{id}` while `QUEUED`/`RUNNING`. Fund grid from `items`.
9. **Item breaks** — summary + results with `status=BREAK`.

Do not add a vendor proxy in the UI. Do not send Gemini keys from the UI.

---

## Company laptop / production config (ops, not UI)

When you pull this repo onto the company machine:

1. Point datasource at the office DB (SQL Server script: `src/main/resources/db/schema-sqlserver.sql`).
2. Seed `source` rows (base URL, path template, `param_schema_json`, `secret_ref`, `auth_type`).
3. Put vendor tokens in env / Spring properties named by `secret_ref`. Never commit them.
4. Set `recon.sources.allow-http: false` and `recon.sources.allowed-host-suffixes` to vendor host suffixes (e.g. `blackrock.com`).
5. Keep `X-User-Email` as the same identity header the office UI already uses, or put a gateway in front that injects it.

Retention: `recon.retention` cron `0 0 2 * * *` Asia/Kolkata, keep 7 days of dataset payloads.
