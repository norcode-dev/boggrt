# Boggrt Configuration Specification

## 1. Purpose

This document explains how Boggrt reads endpoint configurations and how requests are matched at runtime.

Boggrt serves a configured response when:

1. HTTP method and path match a configured endpoint.
2. All endpoint conditions pass (if conditions are configured).

If no endpoint matches, or any condition fails, Boggrt returns `404 Not Found`.

## 2. Where Configuration Is Loaded From

Boggrt reads endpoint definitions from `.json` files located in the directory specified by the `boggrt.endpoints-folder-path` Quarkus configuration key.
- Environment override: `BOGGRT_SOURCE`
- Default value: `src/main/resources/`

Each `.json` file can contain either:

- one endpoint object
- an array of endpoint objects

## 3. Runtime Matching Flow

For each incoming request:

1. Boggrt checks method + path against configured endpoints.
2. If the endpoint has no conditions (missing or empty array), Boggrt returns the configured response with HTTP `200`.
3. If conditions exist, Boggrt parses the request body as JSON and evaluates all conditions.
4. If every condition passes, Boggrt returns HTTP `200` with the configured response.
5. If any condition fails (or body JSON cannot be parsed), Boggrt returns HTTP `404` with body `Response not found.`

Successful responses are sent with `Content-Type: application/json`.

## 4. Endpoint Schema

### 4.1 Endpoint Object

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
  "response": {
    "status": "accepted"
  }
}
```

| Field | Type | Required | Rules |
|---|---|---|---|
| `method` | String | Yes | Must be a valid Vert.x `HttpMethod` value (for example `GET`, `POST`, `PUT`, `DELETE`). Use uppercase names. |
| `path` | String | Yes | Route path to match (for example `/orders/validate`). |
| `conditions` | Array\<Condition\> | No | If missing or empty, no condition checks are performed. |
| `response` | Any JSON value | Yes | Returned as the response body when endpoint matches. |

### 4.2 Condition Object

```json
{
  "field": "items[*].sku",
  "operator": "contains",
  "value": "SKU-"
}
```

| Field | Type | Required | Rules |
|---|---|---|---|
| `field` | String | Yes | Path expression resolved from the request JSON body root. |
| `operator` | String | Yes | One of the supported operators (case-insensitive). |
| `value` | String \| Number \| Boolean | Yes | Required for parsing in all operators, including `exists` and `isEmpty`. |

## 5. Field Path Syntax

Boggrt resolves `field` from the request body root.

- `customer.lastName` -> nested property
- `items[0].sku` -> indexed array element
- `items[*].sku` -> wildcard over all array elements

Important:

- `request.` is not a reserved prefix.
- Use `request.lastName` only if your request JSON really has a top-level `request` field.

Example:

If body is:

```json
{
  "lastName": "Doe",
  "items": [
    { "sku": "SKU-1" },
    { "sku": "SKU-2" }
  ]
}
```

then valid fields are:

- `lastName`
- `items[0].sku`
- `items[*].sku`

## 6. Operators

All conditions are combined with AND. Every condition must pass.

| Operator | Field Type Expected | Value Type | Passes When |
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

Wildcard behavior (`[*]`):

- All resolved elements must satisfy the operator.
- If wildcard resolves to no elements, the condition fails.

## 7. Practical Examples

### 7.1 Single Endpoint Without Conditions

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
- body: `{"status":"ok"}`

### 7.2 Endpoint With Conditions

Configuration:

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
  "response": {
    "status": "accepted",
    "code": "MOCK-OK"
  }
}
```

Matching request:

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

Result: HTTP `200` with configured response.

Non-matching request (`items[1].sku` does not contain `SKU-`):

```bash
curl -i \
  -X POST http://localhost:8080/orders/validate \
  -H "Content-Type: application/json" \
  -d '{
    "customer": { "lastName": "Doe" },
    "items": [{ "sku": "SKU-1" }, { "sku": "BAD" }],
    "traceId": "abc-123",
    "notes": ""
  }'
```

Result: HTTP `404` with `Response not found.`

### 7.3 Multiple Endpoints In One File

```json
[
  {
    "method": "GET",
    "path": "/hello2",
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
