package dev.norcode.resource.defaultsource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@TestProfile(DefaultSourceTestProfile.class)
class DefaultSourceResourceTest {

  @Test
  void testDefaultSourceLoadsEndpointsFromMainResourcesWhenBoggrtSourceIsNotSet() {
    given()
        .when()
        .get("/hello1")
        .then()
        .statusCode(200)
        .contentType("application/json")
        .body("firstName", is("John"))
        .body("lastName", is("Doe"));
  }
}
