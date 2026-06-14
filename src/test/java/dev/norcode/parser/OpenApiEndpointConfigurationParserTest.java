package dev.norcode.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.norcode.configuration.EndpointConfiguration;
import io.vertx.core.http.HttpMethod;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OpenApiEndpointConfigurationParserTest {

  private static final Path FIXTURE = Paths.get("src/test/resources/openapi/petstore.yaml");

  private final OpenApiEndpointConfigurationParser parser =
      new OpenApiEndpointConfigurationParser(new SchemaSampleGenerator());

  @Test
  void translatesCurlyBracesToVertxColonStyle() {
    assertEquals("/pets/:petId", OpenApiEndpointConfigurationParser.translatePathParameters("/pets/{petId}"));
    assertEquals(
        "/owners/:ownerId/pets/:petId/notes",
        OpenApiEndpointConfigurationParser.translatePathParameters(
            "/owners/{ownerId}/pets/{petId}/notes"));
    assertEquals("/pets", OpenApiEndpointConfigurationParser.translatePathParameters("/pets"));
  }

  @Test
  void parsesAllOperationsFromFixture() {
    Set<EndpointConfiguration> endpoints = parser.parse(Set.of(FIXTURE.toAbsolutePath()));

    assertEquals(5, endpoints.size());

    assertTrue(hasEndpoint(endpoints, HttpMethod.GET, "/pets"));
    assertTrue(hasEndpoint(endpoints, HttpMethod.POST, "/pets"));
    assertTrue(hasEndpoint(endpoints, HttpMethod.GET, "/pets/:petId"));
    assertTrue(hasEndpoint(endpoints, HttpMethod.GET, "/owners/:ownerId/pets/:petId/notes"));
    assertTrue(hasEndpoint(endpoints, HttpMethod.GET, "/admin/ping"));
  }

  @Test
  void selectsSmallest2xxStatus() {
    Set<EndpointConfiguration> endpoints = parser.parse(Set.of(FIXTURE.toAbsolutePath()));

    EndpointConfiguration createPet = find(endpoints, HttpMethod.POST, "/pets");
    assertEquals(Optional.of(201), createPet.responseStatus());

    EndpointConfiguration listPets = find(endpoints, HttpMethod.GET, "/pets");
    assertEquals(Optional.of(200), listPets.responseStatus());
  }

  @Test
  void responsesAreEmptyOfConditions() {
    Set<EndpointConfiguration> endpoints = parser.parse(Set.of(FIXTURE.toAbsolutePath()));

    for (EndpointConfiguration endpoint : endpoints) {
      assertNotNull(endpoint.conditions());
      assertTrue(endpoint.conditions().isEmpty(), "OpenAPI endpoints must not declare conditions");
    }
  }

  @Test
  void responseBodyHonorsSchemaExample() {
    Set<EndpointConfiguration> endpoints = parser.parse(Set.of(FIXTURE.toAbsolutePath()));

    EndpointConfiguration notes =
        find(endpoints, HttpMethod.GET, "/owners/:ownerId/pets/:petId/notes");

    assertTrue(
        notes.response().contains("fixed-example-value"),
        "expected example value in body but was: " + notes.response());
  }

  @Test
  void responseBodyHonorsSchemaDefault() {
    Set<EndpointConfiguration> endpoints = parser.parse(Set.of(FIXTURE.toAbsolutePath()));

    EndpointConfiguration ping = find(endpoints, HttpMethod.GET, "/admin/ping");

    assertTrue(
        ping.response().contains("\"ok\""),
        "expected default value in body but was: " + ping.response());
  }

  @Test
  void parsingEmptyInputReturnsEmpty() {
    Set<EndpointConfiguration> endpoints = parser.parse(Set.of());

    assertNotNull(endpoints);
    assertTrue(endpoints.isEmpty());
  }

  private static boolean hasEndpoint(
      Set<EndpointConfiguration> endpoints, HttpMethod method, String path) {
    return endpoints.stream()
        .anyMatch(e -> e.method() == method && e.path().equals(path));
  }

  private static EndpointConfiguration find(
      Set<EndpointConfiguration> endpoints, HttpMethod method, String path) {
    return endpoints.stream()
        .filter(e -> e.method() == method && e.path().equals(path))
        .findFirst()
        .orElseThrow(
            () -> new AssertionError("Endpoint not found: " + method + " " + path));
  }
}
