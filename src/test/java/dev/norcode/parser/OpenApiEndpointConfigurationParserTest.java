package dev.norcode.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    assertEquals(7, endpoints.size());

    assertTrue(hasEndpoint(endpoints, HttpMethod.GET, "/pets"));
    assertTrue(hasEndpoint(endpoints, HttpMethod.POST, "/pets"));
    assertTrue(hasEndpoint(endpoints, HttpMethod.GET, "/featured-pets"));
    assertTrue(hasEndpoint(endpoints, HttpMethod.GET, "/paged-pets"));
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
  void listResponseIncludesItemExampleOnceAndFillsWithGeneratedItems() throws Exception {
    Set<EndpointConfiguration> endpoints = parser.parse(Set.of(FIXTURE.toAbsolutePath()));

    EndpointConfiguration listPets = find(endpoints, HttpMethod.GET, "/pets");

    assertTrue(
        listPets.response().contains("Fido-the-example-pet"),
        "expected the item example in the array but was: " + listPets.response());

    com.fasterxml.jackson.databind.JsonNode body =
        new com.fasterxml.jackson.databind.ObjectMapper().readTree(listPets.response());
    assertTrue(body.isArray(), "expected an array body but was: " + listPets.response());
    assertTrue(
        body.size() >= 5 && body.size() <= 10,
        "expected 5..10 items but was: " + body.size());
    assertEquals(42, body.get(0).get("id").asInt(), "first element should be the example");
  }

  @Test
  void responseBodyHonorsMultiElementArrayExample() throws Exception {
    Set<EndpointConfiguration> endpoints = parser.parse(Set.of(FIXTURE.toAbsolutePath()));

    EndpointConfiguration featured = find(endpoints, HttpMethod.GET, "/featured-pets");

    com.fasterxml.jackson.databind.JsonNode body =
        new com.fasterxml.jackson.databind.ObjectMapper().readTree(featured.response());

    assertTrue(body.isArray(), "expected an array body but was: " + featured.response());
    assertEquals(3, body.size(), "expected exactly the example's 3 elements but was: " + featured.response());
    assertEquals("Rex-the-featured-pet", body.get(0).get("name").asText());
    assertEquals("Bella-the-featured-pet", body.get(1).get("name").asText());
    assertEquals("Max-the-featured-pet", body.get(2).get("name").asText());
  }

  @Test
  void responseBodyHonorsMediaTypeExampleVerbatim() throws Exception {
    // Regression for the Rohire /clients report: the list response carries a media-type-level example
    // with several fully-specified items. Every element (and the pagination block) must be returned
    // exactly as written — not just the first, with the rest mocked.
    Set<EndpointConfiguration> endpoints = parser.parse(Set.of(FIXTURE.toAbsolutePath()));

    EndpointConfiguration paged = find(endpoints, HttpMethod.GET, "/paged-pets");

    com.fasterxml.jackson.databind.JsonNode body =
        new com.fasterxml.jackson.databind.ObjectMapper().readTree(paged.response());

    com.fasterxml.jackson.databind.JsonNode data = body.get("data");
    assertTrue(data.isArray(), "expected a data array but was: " + paged.response());
    assertEquals(3, data.size(), "expected exactly the example's 3 items but was: " + paged.response());
    assertEquals("Alpha-pet", data.get(0).get("name").asText());
    assertEquals("Beta-pet", data.get(1).get("name").asText());
    assertEquals("Gamma-pet", data.get(2).get("name").asText());
    assertEquals(3, body.get("pagination").get("totalItems").asInt());
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
