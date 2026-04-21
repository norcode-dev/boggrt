# Boggrt

Boggrt is a containerized mock API server that returns configurable JSON responses based on request method, path, and optional body conditions. 
It is designed for local development, automated integration tests, CI pipelines, and Docker Compose setups where you need a predictable stand-in for external APIs.

## Quick reference

- Image: `norcodedev/boggrt`
- Default configuration directory: `/resources`
- Override configuration directory with: `BOGGRT_SOURCE`
- Input format: one or more `.json` files, each containing a single endpoint object or an array of endpoint objects
- Source and documentation: [GitHub repository](https://github.com/norcode-dev/boggrt)

## Why use Boggrt?

- Mock HTTP APIs without writing custom stub servers
- Match requests by `method` and `path`
- Add request-body conditions using nested fields, array indexes, and wildcards like `items[*].sku`
- Return custom JSON bodies with custom HTTP status codes
- Reuse the same mock files in standalone Docker runs, Testcontainers, and Docker Compose

## How it works

Boggrt loads endpoint definitions from the configured folder and evaluates each incoming request in this order:

1. Match request `method` and `path`
2. If conditions are configured, parse the request body as JSON and evaluate all conditions
3. Return the configured JSON response when everything matches

All configured conditions are combined with logical `AND`.

Behavior summary:

- Default success status is HTTP `200` when `responseStatus` is omitted
- No matching endpoint returns HTTP `404` with `Response not found.`
- A matching path with the wrong HTTP method returns HTTP `405 Method Not Allowed`
- If conditions are configured, a failed condition or invalid JSON body returns HTTP `404`
- Successful responses are returned as `application/json`

## Running the image

```bash
docker run --rm -p 8080:8080 \
  -v "$(pwd)/mocks:/resources" \
  norcodedev/boggrt
```

To load configuration from a different folder inside the container:

```bash
docker run --rm -p 8080:8080 \
  -e BOGGRT_SOURCE=/config \
  -v "$(pwd)/mocks:/config" \
  norcodedev/boggrt
```

## Example configuration

Simple endpoint:

```json
{
  "method": "GET",
  "path": "/health/mock",
  "response": {
    "status": "ok"
  }
}
```

Conditional endpoint:

```json
{
  "method": "POST",
  "path": "/orders/validate",
  "conditions": [
    { "field": "customer.lastName", "operator": "equals", "value": "Doe" },
    { "field": "items[*].sku", "operator": "contains", "value": "SKU-" }
  ],
  "responseStatus": 202,
  "response": {
    "status": "accepted",
    "code": "MOCK-OK"
  }
}
```

Place these files in the mounted configuration directory and call the matching routes with your HTTP client or test suite.

## Common use cases

- Stub third-party APIs during local development
- Run deterministic integration tests with Testcontainers
- Share the same mock definitions between CI jobs and Docker Compose environments
- Validate different request payloads with condition-based responses

## More information

For full configuration details, supported operators, and more examples, see the [GitHub repository](https://github.com/norcode-dev/boggrt).
