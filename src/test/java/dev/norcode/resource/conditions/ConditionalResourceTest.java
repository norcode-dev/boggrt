package dev.norcode.resource.conditions;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
@TestProfile(ConditionEndpointsTestProfile.class)
class ConditionalResourceTest {
  @Test
  void testOrdersValidateUsesConfiguredResponseStatusWhenAllConditionsMatch() {
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
        .statusCode(202)
        .contentType("application/json")
        .body("status", is("accepted"))
        .body("code", is("MOCK-OK"));
  }

  @Test
  void shouldValidateArrayOrderWithValidRequest() {
    given()
            .contentType("application/json")
            .body(
                    """
                    {
                      "customer": { "lastName": "Doe" },
                      "items": [{ "sku": "SKU-001" }, { "sku": "SKU-002" }],
                      "traceId": "abc-123",
                      "notes": ""
                    }
                    """)
            .when()
            .post("/orders/validate/array-order")
            .then()
            .statusCode(202)
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
              "items": [{ "sku": "SKU" }, { "sku": "BAD" }],
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
        .statusCode(400)
        .body(not(is("Response not found.")));
  }

  @Test
  void testCaseInsensitiveOperatorName() {
    given()
        .contentType("application/json")
        .body("""
            { "name": "test" }
            """)
        .when()
        .post("/ops/case-insensitive")
        .then()
        .statusCode(200)
        .contentType("application/json")
        .body("status", is("ok"));
  }

  @Test
  void testResponseCanBeString() {
    given()
        .when()
        .get("/response/string")
        .then()
        .statusCode(200)
        .contentType("application/json")
        .body(is("\"hello\""));
  }

  @Test
  void testResponseCanBeNumber() {
    given()
        .when()
        .get("/response/number")
        .then()
        .statusCode(200)
        .contentType("application/json")
        .body(is("42"));
  }

  @Test
  void testResponseCanBeBoolean() {
    given()
        .when()
        .get("/response/boolean")
        .then()
        .statusCode(200)
        .contentType("application/json")
        .body(is("true"));
  }

  @Test
  void testResponseCanBeArray() {
    given()
        .when()
        .get("/response/array")
        .then()
        .statusCode(200)
        .contentType("application/json")
        .body("", hasSize(3))
        .body("[0]", is(1))
        .body("[1]", is(2))
        .body("[2]", is(3));
  }

  @Test
  void testNumbersRangeDefaultsToStatus200WhenResponseStatusMissing() {
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
