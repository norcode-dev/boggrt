# Boggrt

Boggrt is a mock API server that serves responses based on configurable endpoints and conditions. The goal is to provide
a simple and flexible way to mock API responses for testing and development purposes.

It runs in a container, so you can run it as a standalone service, integrate it into your integration tests
(with `Testcontainers` for example), in your CI/CD pipelines, or even in your local development environment.

## Features

- Configure multiple mock API endpoints.
- Define specific conditions for incoming requests (method, path, body fields).
- Supports JSONPath-like syntax for request validation.
- Returns configured mock responses when conditions are met, otherwise returns 404.

## Configuration
Boggrt uses JSON configuration files to define the mock API endpoints and conditions.

> [!TIP]
> You can use one or more configuration files.

> [!IMPORTANT]  
> For detailed endpoint configuration options, see [Configuration Specification](spec.md).

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

### Running Boggrt: Standalone

To run Boggrt in standalone mode, you need to mount a volume with the configuration files pointing to the `/resources` folder.

```shell script
docker run -d ./src/main/resources:/resources --rm -p 8080:8080 norcodedev/boggrt
```

You can change the folder where the configuration files are loaded by setting the `BOGGRT_SOURCE` env variable.
```shell script
docker run -d --env BOGGRT_SOURCE=/json -v ./src/main/resources:/json --rm -p 8080:8080 norcodedev/boggrt
```

### Running Boggrt: Spring Boot + Testcontainers

The Demo application exposes an endpoint called `/demo/hello` wich calls an external API defined in `demo.external-url` property.

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
> The name of the configuration file does not matter and can be placed anywhere in the `resources` folder.

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
During the development you may want to mock the external API calls, you can do it easily with Docker Compose and Boggrt.

> [!NOTE]  
>The advantage of using Boggrt is that you can reuse the same configuration files used in the integration tests.

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