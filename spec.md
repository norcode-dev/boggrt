# Boggrt - Configuration Specification

This document defines how Boggrt loads endpoint configuration and matches incoming requests.

## Quick Overview

Boggrt returns a configured response when both of these are true:

1. Request `method` and `path` match an endpoint.
2. Endpoint `conditions` pass (if conditions are configured).

If an endpoint matches and `responseStatus` is omitted, Boggrt returns HTTP `200`.

If no endpoint matches, Boggrt returns HTTP `404 Not Found` with body `Response not found.`.

If method and path match but at least one condition fails, Boggrt returns HTTP `404 Not Found` with body `Response not found.`.

If the path exists but the HTTP method does not match, Boggrt returns HTTP `405 Method Not Allowed`.

Successful responses are sent with `Content-Type: application/json`.

## 1. Configuration Source

Boggrt reads endpoint definitions from two independent folders:

1. **JSON endpoint configuration** — `.json` files in `boggrt.endpoints-folder-path`.
   - Environment override: `BOGGRT_SOURCE`
   - Default directory: `/resources`
2. **OpenAPI specifications** — `.yaml`, `.yml`, or `.json` OpenAPI 3.x documents in `boggrt.openapi-folder-path`.
   - Environment override: `BOGGRT_OPENAPI_SOURCE`
   - Default directory: `/openapi`
   - Missing folder is tolerated (no routes registered from OpenAPI).

Each JSON endpoint file may contain:

- one endpoint object
- an array of endpoint objects

For OpenAPI specifications, see section [7. OpenAPI Import](#7-openapi-import).

## 2. Runtime Matching Flow

For each incoming request:

1. Identify an endpoint where the method and path match.
2. If the endpoint has no conditions (missing or empty), return the configured response with HTTP status responseStatus (defaults to 200 if not specified).
3. If conditions are present, parse the request body as JSON.
4. Evaluate all configured conditions.
5. If all conditions pass, return the endpoint’s response with HTTP status responseStatus (defaults to 200 if not specified).
6. If JSON parsing fails or any condition does not pass, return HTTP 404 with the message Response not found.

## 3. Endpoint Schema

### 3.1 Endpoint Object

```json
{
  "method": "POST",
  "path": "/orders/validate",
  "conditions": [
    {
      "field": "customer.lastName",
      "operator": "equals",
      "value": "Doe"
    }
  ],
  "responseStatus": 202,
  "response": {
    "status": "accepted"
  }
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `method` | String | Yes | Must be a valid `HttpMethod` (for example `GET`, `POST`, `PUT`, `DELETE`). Use uppercase. |
| `path` | String | Yes | Route path to match (for example `/orders/validate`). |
| `conditions` | Array\<Condition\> | No | If missing or empty, no condition checks are performed. |
| `responseStatus` | Number | No | Status code returned when endpoint matches and conditions pass. Defaults to `200` when omitted. |
| `response` | Any JSON value | Yes | Response body returned when endpoint matches. |

### 3.2 Condition Object

```json
{
  "field": "items[*].sku",
  "operator": "contains",
  "value": "SKU-"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `field` | String | Yes | Path expression resolved from the request JSON root. |
| `operator` | String | Yes | One of the supported operators (case-insensitive). |
| `value` | String \| Number \| Boolean | Yes | Required by schema for all operators, including `exists` and `isEmpty`. |

## 4. Field Path Syntax

Boggrt resolves `field` from the request JSON body root.

Common patterns:

- `customer.lastName` -> nested property
- `items[0].sku` -> array element by index
- `items[*].sku` -> wildcard over array elements (condition passes if any element matches)

Notes:

- `request.` is not a reserved prefix.
- Use `request.lastName` only if the body really has a top-level `request` field.

Example request body:

```json
{
  "lastName": "Doe",
  "items": [
    { "sku": "SKU-1" },
    { "sku": "SKU-2" }
  ]
}
```

Valid paths for that body include:

- `lastName`
- `items[0].sku`
- `items[*].sku`

## 5. Operators

All conditions are combined with logical `AND`, so every configured condition must pass.

| Operator | Expected Field Type | Value Type | Passes When |
|---|---|---|---|
| `equals` | string, number, boolean | same logical type | Field equals `value` |
| `contains` | string | string (recommended) | Field contains `value` as substring |
| `greaterThan` | number | number | Field > `value` |
| `lessThan` | number | number | Field < `value` |
| `isEmpty` | array or string | any (required but ignored) | Array length is 0, or string length is 0 |
| `sizeEquals` | array or string | number | Size/length == `value` |
| `sizeGreaterThan` | array or string | number | Size/length > `value` |
| `sizeLessThan` | array or string | number | Size/length < `value` |
| `exists` | any non-null field | any (required but ignored) | Field path resolves to a non-null value |

Wildcard (`[*]`) rule:

- A wildcard condition passes when at least one resolved element satisfies the operator.
- If wildcard resolution returns no elements, the condition fails.

## 6. Practical Examples

### 6.1 Single Endpoint Without Conditions

Configuration:

```json
{
  "method": "GET",
  "path": "/health/mock",
  "response": {
    "status": "ok"
  }
}
```

Request:

```bash
curl -i http://localhost:8080/health/mock
```

Result:

- HTTP `200`
- Body: `{"status":"ok"}`

### 6.2 Endpoint With Conditions

#### Configuration:

```json
{
  "method": "POST",
  "path": "/orders/validate",
  "conditions": [
    { "field": "customer.lastName", "operator": "equals", "value": "Doe" },
    { "field": "items", "operator": "sizeGreaterThan", "value": 1 },
    { "field": "items[*].sku", "operator": "contains", "value": "SKU-" },
    { "field": "traceId", "operator": "exists", "value": true },
    { "field": "notes", "operator": "isEmpty", "value": true }
  ],
  "responseStatus": 202,
  "response": {
    "status": "accepted",
    "code": "MOCK-OK"
  }
}
```

#### Matching request:

```bash
curl -i \
  -X POST http://localhost:8080/orders/validate \
  -H "Content-Type: application/json" \
  -d '{
    "customer": { "lastName": "Doe" },
    "items": [{ "sku": "SKU-1" }, { "sku": "SKU-2" }],
    "traceId": "abc-123",
    "notes": ""
  }'
```

Result: HTTP `202` with configured response body.

#### Non-matching request (no `items[*].sku` contains `SKU-`):

```bash
curl -i \
  -X POST http://localhost:8080/orders/validate \
  -H "Content-Type: application/json" \
  -d '{
    "customer": { "lastName": "Doe" },
    "items": [{ "sku": "BAD-1" }, { "sku": "BAD-2" }],
    "traceId": "abc-123",
    "notes": ""
  }'
```

Result: HTTP `404` with `Response not found.`.

### 6.3 Multiple Endpoints in One File

```json
[
  {
    "method": "GET",
    "path": "/hello2",
    "responseStatus": 201,
    "response": {
      "firstName": "John",
      "lastName": "Doe"
    }
  },
  {
    "method": "GET",
    "path": "/hello3",
    "response": {
      "firstName": "Jane",
      "lastName": "Smith"
    }
  }
]
```

## 7. OpenAPI Import

Boggrt can seed mock endpoints directly from OpenAPI specifications. Specs are loaded from `boggrt.openapi-folder-path` (env override `BOGGRT_OPENAPI_SOURCE`, default `/openapi`).

### 7.1 Supported file types

- `.yaml`, `.yml`, `.json`
- OpenAPI 3.x documents (3.0, 3.1, and 3.2 where the underlying parser supports it)

### 7.2 Generated endpoints

For each `(path, operation)` declared in the spec, Boggrt generates an `EndpointConfiguration`:

| Field | Source |
|---|---|
| `method` | The operation key (`get`, `post`, ...) |
| `path` | The spec path with `{name}` translated to Vert.x style `:name` (e.g. `/pets/{petId}` → `/pets/:petId`). |
| `responseStatus` | The smallest 2xx status declared in `responses`. Falls back to `200`. |
| `response` | A JSON body generated from the response's `application/json` schema. |
| `conditions` | Always empty — OpenAPI does not express request-match conditions. Add a JSON endpoint file to layer conditions on top. |

### 7.3 Response generation

The body is generated by walking the response schema and emitting a value per node:

- If a schema declares `example`, that value is emitted verbatim.
- Otherwise, if a schema declares `default`, that value is emitted verbatim.
- Otherwise, the first `enum` value is used (if any).
- Otherwise, a random value is generated via Datafaker, with format-aware choices for `email`, `uuid`, `date`, `date-time`, `uri`, `hostname`, `ipv4`, `ipv6`, `byte`, `password`.

Integer and number bounds (`minimum`, `maximum`) and array bounds (`minItems`, `maxItems`) are honored. References (`$ref`) are resolved by the parser before generation.

### 7.4 Collision policy

When the same `(method, path)` is declared in both a JSON endpoint file and an OpenAPI spec, **the JSON entry wins** and a WARNING is logged at startup so the override is visible. This makes it easy to import an entire spec for shape and then hand-tune individual endpoints (custom response, conditions, alternate status).

