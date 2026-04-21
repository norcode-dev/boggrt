package dev.norcode.resource.noconditions;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
@TestProfile(NoConditionsEndpointsTestProfile.class)
class NoConditionsResourceTest {
    @Test
    void testHello1EndpointFromSingleConfig() {
        given()
                .when().get("/hello1")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("firstName", is("John"))
                .body("lastName", is("Doe"))
                .body("age", is(30))
                .body("options", hasSize(2))
                .body("options[0].title", is("title 1"))
                .body("options[0].description", is("description 1"))
                .body("options[1].title", is("title 2"))
                .body("options[1].description", is("description 2"));
    }

    @Test
    void testHello2EndpointUsesConfiguredResponseStatus() {
        given()
          .when().get("/hello2")
          .then()
             .statusCode(201)
                .contentType("application/json")
                .body("firstName", is("John"))
                .body("lastName", is("Doe"))
                .body("age", is(30))
                .body("options", hasSize(2))
                .body("options[0].title", is("title 1"))
                .body("options[0].description", is("description 1"))
                .body("options[1].title", is("title 2"))
                .body("options[1].description", is("description 2"));
    }

    @Test
    void testHello3EndpointDefaultsToStatus200WhenResponseStatusMissing() {
        given()
                .when().get("/hello3")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("firstName", is("Jane"))
                .body("lastName", is("Doe"))
                .body("age", is(30))
                .body("options", hasSize(1))
                .body("options[0].title", is("title 1"))
                .body("options[0].subTitle", is("subTitle 1"));
    }

    @Test
    void testUnknownPathReturnsNotFound() {
        given()
                .when().get("/does-not-exist")
                .then()
                .statusCode(404);
    }

    @Test
    void testMethodMismatchReturnsMethodNotAllowed() {
        given()
                .when().post("/hello2")
                .then()
                .statusCode(405);
    }
}
