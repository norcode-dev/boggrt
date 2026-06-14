package dev.norcode.resource.openapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(OpenApiTestProfile.class)
class OpenApiResourceTest {

  @Test
  void listsPetsFromOpenApiSpec() {
    given()
        .when()
        .get("/pets")
        .then()
        .statusCode(200)
        .contentType("application/json")
        .body("$", notNullValue());
  }

  @Test
  void createsPetWithSpecifiedResponseStatus() {
    given().when().post("/pets").then().statusCode(201).contentType("application/json");
  }

  @Test
  void translatesPathParametersToVertxStyle() {
    given()
        .when()
        .get("/pets/anything")
        .then()
        .statusCode(200)
        .contentType("application/json");
  }

  @Test
  void supportsDeepPathsWithMultipleParameters() {
    given()
        .when()
        .get("/owners/abc/pets/xyz/notes")
        .then()
        .statusCode(200)
        .contentType("application/json")
        .body("note", is("fixed-example-value"));
  }

  @Test
  void honorsSchemaDefaultValue() {
    given()
        .when()
        .get("/admin/ping")
        .then()
        .statusCode(200)
        .contentType("application/json")
        .body("status", equalTo("ok"));
  }

  @Test
  void returnsNotFoundForUnknownPath() {
    given().when().get("/missing").then().statusCode(404);
  }
}
