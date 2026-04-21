package dev.norcode;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@TestProfile(ConditionEndpointsTestProfile.class)
class ConditionalResourceTest {
  @Test
  void testOrdersValidateReturnsConfiguredResponseWhenAllConditionsMatch() {
    given()
        .contentType("application/json")
        .body(
            """
            {
              "customer": { "lastName": "Doe" },
              "items": [{ "sku": "SKU-1" }, { "sku": "SKU-2" }],
              "traceId": "abc-123",
              "notes": ""
            }
            """)
        .when()
        .post("/orders/validate")
        .then()
        .statusCode(200)
        .contentType("application/json")
        .body("status", is("accepted"))
        .body("code", is("MOCK-OK"));
  }

  @Test
  void testOrdersValidateReturnsNotFoundWhenConditionFails() {
    given()
        .contentType("application/json")
        .body(
            """
            {
              "customer": { "lastName": "Doe" },
              "items": [{ "sku": "SKU-1" }, { "sku": "BAD" }],
              "traceId": "abc-123",
              "notes": ""
            }
            """)
        .when()
        .post("/orders/validate")
        .then()
        .statusCode(404)
        .body(is("Response not found."));
  }

  @Test
  void testOrdersValidateReturnsNotFoundWhenBodyIsInvalidJson() {
    given()
        .contentType("application/json")
        .body("this is not json")
        .when()
        .post("/orders/validate")
        .then()
        .statusCode(404)
        .body(is("Response not found."));
  }

  @Test
  void testNumbersRangeReturnsConfiguredResponseForValidAmount() {
    given()
        .contentType("application/json")
        .body(
            """
            {
              "amount": 150
            }
            """)
        .when()
        .post("/numbers/range")
        .then()
        .statusCode(200)
        .contentType("application/json")
        .body("status", is("range-ok"));
  }

  @Test
  void testNumbersRangeReturnsNotFoundForOutOfRangeAmount() {
    given()
        .contentType("application/json")
        .body(
            """
            {
              "amount": 90
            }
            """)
        .when()
        .post("/numbers/range")
        .then()
        .statusCode(404)
        .body(is("Response not found."));
  }

  @Test
  void testItemsCountExactUsesSizeEqualsOperator() {
    given()
        .contentType("application/json")
        .body(
            """
            {
              "items": [1, 2, 3]
            }
            """)
        .when()
        .post("/items/count-exact")
        .then()
        .statusCode(200)
        .contentType("application/json")
        .body("status", is("count-ok"));
  }

  @Test
  void testItemsCountExactReturnsNotFoundWhenSizeDoesNotMatch() {
    given()
        .contentType("application/json")
        .body(
            """
            {
              "items": [1]
            }
            """)
        .when()
        .post("/items/count-exact")
        .then()
        .statusCode(404)
        .body(is("Response not found."));
  }

  @Test
  void testItemsCountMaxUsesSizeLessThanOperator() {
    given()
        .contentType("application/json")
        .body(
            """
            {
              "items": [1, 2]
            }
            """)
        .when()
        .post("/items/count-max")
        .then()
        .statusCode(200)
        .contentType("application/json")
        .body("status", is("count-max-ok"));
  }

  @Test
  void testItemsCountMaxReturnsNotFoundWhenSizeIsTooLarge() {
    given()
        .contentType("application/json")
        .body(
            """
            {
              "items": [1, 2, 3]
            }
            """)
        .when()
        .post("/items/count-max")
        .then()
        .statusCode(404)
        .body(is("Response not found."));
  }
}
