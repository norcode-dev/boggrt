# Boggrt

Boggrt is a mock API server that serves responses based on configurable endpoints and conditions. The goal is to provide
a simple and flexible way to mock API responses for testing and development purposes.

It runs in a container, so you can run it as a standalone service or integrate it into your existing integration tests
with `Testcontainers` for example.

## Features

- Configure multiple mock API endpoints.
- Define specific conditions for incoming requests (method, path, body fields).
- Supports JSONPath-like syntax for request validation.
- Returns configured mock responses when conditions are met, otherwise returns 404.

## Usage

### Running Boggrt: Standalone

To run Boggrt in standalone mode, you need to mount a volume with the configuration files pointing to the `/resources` folder.

```shell script
docker run -d ./src/main/resources:/resources --rm -p 8080:8080 norcodedev/boggrt
```

You can change the folder where the configuration files are loaded by setting the `BOGGRT_SOURCE` env variable.
```shell script
docker run -d --env BOGGRT_SOURCE=/json -v ./src/main/resources:/json --rm -p 8080:8080 norcodedev/boggrt
```

> [!IMPORTANT]  
> For detailed endpoint configuration options, see [Configuration Specification](spec.md).

### Configuration Example: Single Endpoint Without Conditions

The following configuration will return an HTTP 200 response for all requests to `/health/mock`.

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

Result:

- HTTP `200`
- Body: `{"status":"ok"}`

### Configuration Example: Single Endpoint With Conditions

The following configuration will return an HTTP 202 response for all requests to `/orders/validate` where the request body
contains a `customer.lastName` field with the value `Doe`.

Configuration:

```json
{
  "method": "POST",
  "path": "/orders/validate",
  "conditions": [
    { "field": "customer.lastName", "operator": "equals", "value": "Doe" }
  ],
  "responseStatus": 202,
  "response": {
    "status": "accepted",
    "code": "MOCK-OK"
  }
}
```

Result:
- HTTP `202`
- Body: `{"status":"accepted","code":"MOCK-OK"}`