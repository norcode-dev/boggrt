# Boggrt Configuration Specification

## Overview

This document defines the specification for configuring mock API endpoints that are served by the Boggrt mock server. Each endpoint configuration defines an HTTP route with an optional set of conditions that must be satisfied by incoming requests. If all conditions are met, the mock server returns the configured response. If any condition fails, the server returns a **404 Not Found** status.

## Configuration Format

Endpoint configurations are defined in JSON format. The configuration supports both single endpoint objects and arrays of endpoint objects.

### Single Endpoint Format

```json
{
  "method": "GET",
  "path": "/hello1",
  "conditions": [ ... ],
  "response": { ... }
}
```

### Multiple Endpoints Format

```json
[
  {
    "method": "GET",
    "path": "/hello2",
    "response": { ... }
  },
  {
    "method": "GET",
    "path": "/hello3",
    "response": { ... }
  }
]
```

## Schema Definition

### Root Configuration Object

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `method` | String | Yes | HTTP method (GET, POST, PUT, DELETE, etc.) |
| `path` | String | Yes | URL path for the endpoint |
| `conditions` | Array | No | Array of condition objects to validate incoming request data. All conditions must be met for the response to be returned; otherwise, a 404 is returned |
| `response` | Object | Yes | The mock response object to return when all conditions are satisfied |

### Condition Object

Conditions allow validation of incoming request data using various operators. Each condition evaluates a field from the incoming request against a specified operator and optional value. **All conditions must be satisfied for the mock response to be returned. If any condition fails, the server returns HTTP 404.**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `field` | String | Yes | JSON path to the field in the incoming request to validate |
| `operator` | String | Yes | Validation operator to apply |
| `value` | Any | No | Value to compare against (required for some operators) |

## Field Path Syntax

The `field` property supports JSONPath-like syntax for accessing nested properties and array elements in the incoming request:

### Simple Property Access
```json
"field": "request.lastName"
```
Accesses the `lastName` property in the incoming request body.

### Array Element Access
```json
"field": "request.options[0].title"
```
Accesses the `title` property of the first element in the `options` array from the request body.

### Wildcard Array Access
```json
"field": "request.options[*].title"
```
Applies the condition to the `title` property of **all** elements in the `options` array from the request body.

## Supported Operators

### Equality Operators

#### `equals`
Checks if the field value equals the specified value.

**Example:**
```json
{
  "field": "request.lastName",
  "operator": "equals",
  "value": "Luis"
}
```
This condition checks if the incoming request body contains `lastName` equal to "Luis".

### String Operators

#### `contains`
Checks if the field value (string or array) contains the specified value.

**Example:**
```json
{
  "field": "request.options[*].title",
  "operator": "contains",
  "value": "ABC"
}
```
When used with arrays and wildcards, checks if any element in the request contains the value.

### Collection Operators

#### `isEmpty`
Checks if the field (array or string) is empty in the incoming request.

**Example:**
```json
{
  "field": "request.options",
  "operator": "isEmpty"
}
```

#### `lengthGreaterThan`
Checks if the length of the field (array or string) in the incoming request is greater than the specified value.

**Example:**
```json
{
  "field": "request.options",
  "operator": "lengthGreaterThan",
  "value": 5
}
```

## Complete Examples

### Example 1: Endpoint with Request Conditions

This example defines a `GET /hello1` endpoint that only returns the mock response if all conditions are met in the incoming request. If any condition fails, the server returns HTTP 404.

```json
{
  "method": "GET",
  "path": "/hello1",
  "conditions": [
    {
      "field": "request.lastName",
      "operator": "equals",
      "value": "Luis"
    },
    {
      "field": "request.options",
      "operator": "isEmpty"
    },
    {
      "field": "request.options",
      "operator": "lengthGreaterThan",
      "value": 5
    },
    {
      "field": "request.options[*].title",
      "operator": "equals",
      "value": "ABC"
    },
    {
      "field": "request.options[0].title",
      "operator": "equals",
      "value": "ABC"
    },
    {
      "field": "request.options[*].title",
      "operator": "contains",
      "value": "ABC"
    }
  ],
  "response": {
    "firstName": "John",
    "lastName": "Doe",
    "age": 30,
    "options": [
      {
        "title": "title 1",
        "description": "description 1"
      },
      {
        "title": "title 2",
        "description": "description 2"
      }
    ]
  }
}
```

**Behavior:**
- The mock server will evaluate all conditions against the incoming request body
- Only if ALL conditions pass will it return the configured `response` with HTTP 200
- If ANY condition fails, it returns HTTP 404

### Example 2: Multiple Endpoints

```json
[
  {
    "method": "GET",
    "path": "/hello2",
    "response": {
      "firstName": "John",
      "lastName": "Doe",
      "age": 30,
      "options": [
        {
          "title": "title 1",
          "description": "description 1"
        },
        {
          "title": "title 2",
          "description": "description 2"
        }
      ]
    }
  },
  {
    "method": "GET",
    "path": "/hello3",
    "response": {
      "firstName": "Jane",
      "lastName": "Smith",
      "age": 25,
      "options": []
    }
  }
]
```

## Validation Rules

1. **Method**: Must be a valid HTTP method string
2. **Path**: Must start with `/` and define a unique endpoint path
3. **Conditions**: Optional array; if present, all conditions must pass for the response to be valid
4. **Response**: Can be any valid JSON object or primitive value
5. **Field Paths**: Must reference valid paths within the response object
6. **Operators**: Must be one of the supported operators listed above
7. **Values**: Required for operators that perform comparisons (`equals`, `contains`, `lengthGreaterThan`)

## Notes

- Conditions with array wildcards (`[*]`) apply the validation to all elements in the array
- Empty conditions arrays are allowed and will be treated as no conditions
- All conditions must be satisfied for the validation to pass
- The `response` object defines the actual data returned by the endpoint
