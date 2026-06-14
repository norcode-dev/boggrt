# Boggrt

Boggrt is a mock API server that serves responses based on configurable endpoints and conditions. The goal is to provide
a simple and flexible way to mock API responses for testing and development purposes.

It runs in a container, so you can run it as a standalone service, integrate it into your integration tests
(with `Testcontainers` for example), in your CI/CD pipelines, or even in your local development environment.

## Features

- Configure multiple mock API endpoints.
- Define specific conditions for incoming requests (method, path, body fields).
- Supports nested field paths, array indexes, and wildcards such as `items[*].sku` for request validation.
- Returns configured mock responses when requests match, returns `404` when no endpoint matches or conditions fail, and returns `405` when the path exists but the HTTP method does not match.
- Import OpenAPI 3.x specifications and serve generated sample responses (powered by Datafaker).

## Quick Start

The easiest way to get started with Boggrt is to use the Docker image available on [Docker Hub](https://hub.docker.com/r/norcodedev/boggrt).

```shell
docker run -d -v ./src/main/resources:/resources --rm -p 8080:8080 norcodedev/boggrt
```

Provide one or more JSON configuration files in the mounted `/resources` folder and Boggrt will serve the configured mock endpoints.

## Configuration
Boggrt uses JSON configuration files to define the mock API endpoints and conditions.

> [!TIP]
> You can use one or more configuration files.

> [!IMPORTANT]  
> For detailed endpoint configuration options, see [Configuration Specification](spec.md).

### Configuration Basics

- Boggrt loads endpoint definitions from `.json` files in `/resources` by default.
- You can override the source directory with the `BOGGRT_SOURCE` environment variable.
- Boggrt also loads OpenAPI 3.x specifications (`.yaml`, `.yml`, `.json`) from `/openapi` by default (override with `BOGGRT_OPENAPI_SOURCE`). Each operation becomes a mock endpoint whose response body is generated from the schema. The OpenAPI folder is optional — if missing, only JSON endpoints are served.
- Each `.json` file may contain a single endpoint object or an array of endpoint objects.
- Each endpoint matches by `method` and `path`, and optional `conditions` are combined with logical `AND`.
- Field paths are resolved from the request JSON root using dot notation, array indexes, and `[*]` wildcards.
- Supported operators are `equals`, `contains`, `greaterThan`, `lessThan`, `isEmpty`, `sizeEquals`, `sizeGreaterThan`, `sizeLessThan`, and `exists`.
- When the same `(method, path)` is declared in both a JSON file and an OpenAPI spec, **the JSON file wins** and a warning is logged.

### Configuration Example: Single Endpoint Without Conditions

The following configuration will return an `HTTP 200` response for all requests to `/health/mock`.

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

The following configuration will return an `HTTP 202` response for all requests to `/orders/validate` where the request body
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

## Usage

### Example Project

For a complete example showing how to use Boggrt in a real project, see
[boggrt-example](https://github.com/norcode-dev/boggrt-example).

It demonstrates how to integrate Boggrt for local development and testing scenarios.

### Running Boggrt: Standalone

To run Boggrt in standalone mode, you need to mount a volume with the configuration files pointing to the `/resources` folder.

```shell
docker run -d -v ./src/main/resources:/resources --rm -p 8080:8080 norcodedev/boggrt
```

You can change the folder where the configuration files are loaded by setting the `BOGGRT_SOURCE` env variable.
```shell
docker run -d --env BOGGRT_SOURCE=/json -v ./src/main/resources:/json --rm -p 8080:8080 norcodedev/boggrt
```

To also import OpenAPI specs from a separate folder, mount that folder to `/openapi` (or override `BOGGRT_OPENAPI_SOURCE`):
```shell
docker run -d \
  -v ./src/main/resources:/resources \
  -v ./openapi-specs:/openapi \
  --rm -p 8080:8080 norcodedev/boggrt
```
Each operation in every spec becomes a mock endpoint. Response bodies are generated from the response schema — fields with `example` or `default` keep those values, everything else is filled with Datafaker. Path parameters like `/pets/{petId}` are reachable via the same URL (`GET /pets/123`).

### Running Boggrt: Spring Boot + Testcontainers

The Demo application exposes an endpoint called `/demo/hello` which calls an external API defined in `demo.external-url` property.

The following example shows how to integrate Boggrt in a Spring Boot application that uses `Testcontainers` for integration testing.

1. Create a configuration file in `src/test/resources/test-data` called `example.json` with the following content:
```json
{
  "method": "GET",
  "path": "/api/v2/facts",
  "response": {
    "data": [
      {
        "id": "c4e46f7c-d3e0-44e7-872e-773bd03c19a6",
        "type": "fact",
        "attributes": {
          "body": "The average dog lives 10 to 14 years."
        }
      }
    ]
  }
}
```
> [!NOTE]
> The configuration file name does not matter and it can be placed anywhere in the mounted folder copied to `/resources` (for this example, `src/test/resources/test-data`).

> [!TIP]
> You can find more information about the configuration options in the previous section.

2. Configure the container to mount the configuration files and override the `demo.external-url` property using `@DynamicPropertySource`.
```Java
class TestcontainersConfiguration {

  static GenericContainer<?> boggrt =
      new GenericContainer<>(DockerImageName.parse("norcodedev/boggrt"))
          .withExposedPorts(8080)
          .withCopyToContainer(MountableFile.forClasspathResource("test-data"), "/resources");

  @DynamicPropertySource
  static void demoProperties(DynamicPropertyRegistry registry) {
    boggrt.start();

    String url = "http://" + boggrt.getHost() + ":" + boggrt.getMappedPort(8080);
    registry.add("demo.external-url", () -> url);
  }
}
```

3. Create a test class that extends `TestcontainersConfiguration` and uses `RestTestClient`.
```Java
class DemoControllerIntegrationTest extends TestcontainersConfiguration {

  @Autowired RestTestClient restTestClient;

  @Test
  void simpleTest() {
    restTestClient
        .get()
        .uri("/demo/hello")
        .accept()
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(FactsResponse.class)
        .value(
            factsResponse -> {
              assertNotNull(factsResponse);
              assertNotNull(factsResponse.data());
              assertNotNull(factsResponse.data().getFirst().type());
              assertEquals("fact", factsResponse.data().getFirst().type());
              assertNotNull(factsResponse.data().getFirst().attributes());
              assertNotNull(factsResponse.data().getFirst().attributes().body());
              assertEquals(
                  "The average dog lives 10 to 14 years.",
                  factsResponse.data().getFirst().attributes().body());
            });
  }
}
```

### Running Boggrt: Spring Boot + Docker Compose
During development you may want to mock external API calls. You can do it with Docker Compose and Boggrt.

> [!NOTE]  
> The advantage of using Boggrt is that you can reuse the same configuration files used in the integration tests.

The following example shows how to automatically run Docker Compose and Boggrt when starting the application.

1. Create a Docker Compose file with the following content:
```yaml
services:
  boggrt:
    image: norcodedev/boggrt
    volumes:
      - ./src/test/resources/test-data:/resources
    ports:
      - "9080:8080"
```

2. Add the following configuration to your local Spring Boot configuration:
```properties
demo.external-url=http://localhost:9080/
spring.docker.compose.enabled=true
spring.docker.compose.lifecycle-management=start_and_stop
```
