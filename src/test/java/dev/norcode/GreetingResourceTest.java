package dev.norcode;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
class GreetingResourceTest {
    @Test
    void testHelloEndpoint() {
        given()
          .when().get("/hello2")
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

}