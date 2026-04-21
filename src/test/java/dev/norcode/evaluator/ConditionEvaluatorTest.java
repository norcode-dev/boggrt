package dev.norcode.evaluator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.norcode.configuration.Condition;
import dev.norcode.configuration.ConditionOperator;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConditionEvaluatorTest {

  @Test
  void shouldNotInvalidateRequestWhenConditionsAreNullOrEmpty() {
    assertFalse(ConditionEvaluator.isRequestInvalid(null, "{\"any\":\"value\"}"));
    assertFalse(ConditionEvaluator.isRequestInvalid(List.of(), "{\"any\":\"value\"}"));
  }

  @Test
  void shouldInvalidateRequestWhenBodyIsNullBlankOrInvalidJsonAndConditionsExist() {
    List<Condition> conditions = List.of(condition("traceId", ConditionOperator.EXISTS, true));

    assertTrue(ConditionEvaluator.isRequestInvalid(conditions, null));
    assertTrue(ConditionEvaluator.isRequestInvalid(conditions, "   "));
    assertTrue(ConditionEvaluator.isRequestInvalid(conditions, "not-json"));
  }

  @Test
  void shouldEvaluateEqualsForStringNumberAndBoolean() {
    assertFalse(
        ConditionEvaluator.isRequestInvalid(
            List.of(condition("customer.lastName", ConditionOperator.EQUALS, "Doe")),
            """
            { "customer": { "lastName": "Doe" } }
            """));

    assertFalse(
        ConditionEvaluator.isRequestInvalid(
            List.of(condition("amount", ConditionOperator.EQUALS, 150.0)),
            """
            { "amount": 150 }
            """));

    assertFalse(
        ConditionEvaluator.isRequestInvalid(
            List.of(condition("active", ConditionOperator.EQUALS, true)),
            """
            { "active": true }
            """));

    assertTrue(
        ConditionEvaluator.isRequestInvalid(
            List.of(condition("amount", ConditionOperator.EQUALS, "150")),
            """
            { "amount": 150 }
            """));
  }

  @Test
  void shouldEvaluateContainsOnStringFields() {
    assertFalse(
        ConditionEvaluator.isRequestInvalid(
            List.of(condition("items[0].sku", ConditionOperator.CONTAINS, "SKU-")),
            """
            { "items": [{ "sku": "SKU-1" }] }
            """));

    assertTrue(
        ConditionEvaluator.isRequestInvalid(
            List.of(condition("items[0].sku", ConditionOperator.CONTAINS, "SKU-")),
            """
            { "items": [{ "sku": "BAD" }] }
            """));
  }

  @Test
  void shouldEvaluateGreaterThanAndLessThanForNumbers() {
    assertFalse(
        ConditionEvaluator.isRequestInvalid(
            List.of(
                condition("amount", ConditionOperator.GREATER_THAN, 100),
                condition("amount", ConditionOperator.LESS_THAN, 200)),
            """
            { "amount": 150 }
            """));

    assertTrue(
        ConditionEvaluator.isRequestInvalid(
            List.of(
                condition("amount", ConditionOperator.GREATER_THAN, 100),
                condition("amount", ConditionOperator.LESS_THAN, 200)),
            """
            { "amount": 90 }
            """));
  }

  @Test
  void shouldEvaluateIsEmptyForStringAndArray() {
    assertFalse(
        ConditionEvaluator.isRequestInvalid(
            List.of(condition("notes", ConditionOperator.IS_EMPTY, true)),
            """
            { "notes": "" }
            """));

    assertFalse(
        ConditionEvaluator.isRequestInvalid(
            List.of(condition("items", ConditionOperator.IS_EMPTY, true)),
            """
            { "items": [] }
            """));

    assertTrue(
        ConditionEvaluator.isRequestInvalid(
            List.of(condition("notes", ConditionOperator.IS_EMPTY, true)),
            """
            { "notes": "not-empty" }
            """));
  }

  @Test
  void shouldEvaluateSizeOperatorsForArrayAndString() {
    assertFalse(
        ConditionEvaluator.isRequestInvalid(
            List.of(condition("items", ConditionOperator.SIZE_EQUALS, 3)),
            """
            { "items": [1, 2, 3] }
            """));

    assertFalse(
        ConditionEvaluator.isRequestInvalid(
            List.of(condition("items", ConditionOperator.SIZE_GREATER_THAN, 1)),
            """
            { "items": [1, 2] }
            """));

    assertFalse(
        ConditionEvaluator.isRequestInvalid(
            List.of(condition("token", ConditionOperator.SIZE_LESS_THAN, 5)),
            """
            { "token": "abc" }
            """));

    assertTrue(
        ConditionEvaluator.isRequestInvalid(
            List.of(condition("items", ConditionOperator.SIZE_LESS_THAN, 2)),
            """
            { "items": [1, 2, 3] }
            """));
  }

  @Test
  void shouldEvaluateExistsOnlyWhenFieldResolvesToNonNullValue() {
    assertFalse(
        ConditionEvaluator.isRequestInvalid(
            List.of(condition("traceId", ConditionOperator.EXISTS, true)),
            """
            { "traceId": "abc-123" }
            """));

    assertTrue(
        ConditionEvaluator.isRequestInvalid(
            List.of(condition("traceId", ConditionOperator.EXISTS, true)),
            """
            { "traceId": null }
            """));

    assertTrue(
        ConditionEvaluator.isRequestInvalid(
            List.of(condition("traceId", ConditionOperator.EXISTS, true)),
            """
            { "other": "value" }
            """));
  }

  @Test
  void shouldResolveNestedAndIndexedPathsFromRootBody() {
    assertFalse(
        ConditionEvaluator.isRequestInvalid(
            List.of(
                condition("customer.lastName", ConditionOperator.EQUALS, "Doe"),
                condition("items[1].sku", ConditionOperator.EQUALS, "SKU-2")),
            """
            {
              "customer": { "lastName": "Doe" },
              "items": [{ "sku": "SKU-1" }, { "sku": "SKU-2" }]
            }
            """));

    assertTrue(
        ConditionEvaluator.isRequestInvalid(
            List.of(condition("items[2].sku", ConditionOperator.EQUALS, "SKU-3")),
            """
            { "items": [{ "sku": "SKU-1" }, { "sku": "SKU-2" }] }
            """));
  }

  @Test
  void shouldTreatRequestPrefixAsRegularFieldName() {
    assertTrue(
        ConditionEvaluator.isRequestInvalid(
            List.of(condition("request.lastName", ConditionOperator.EQUALS, "Doe")),
            """
            { "lastName": "Doe" }
            """));

    assertFalse(
        ConditionEvaluator.isRequestInvalid(
            List.of(condition("request.lastName", ConditionOperator.EQUALS, "Doe")),
            """
            { "request": { "lastName": "Doe" } }
            """));
  }

  @Test
  void shouldRequireAllWildcardElementsToMatchOperator() {
    assertFalse(
        ConditionEvaluator.isRequestInvalid(
            List.of(condition("items[*].sku", ConditionOperator.CONTAINS, "SKU-")),
            """
            { "items": [{ "sku": "SKU-1" }, { "sku": "SKU-2" }] }
            """));

    assertTrue(
        ConditionEvaluator.isRequestInvalid(
            List.of(condition("items[*].sku", ConditionOperator.CONTAINS, "SKU-")),
            """
            { "items": [{ "sku": "SKU-1" }, { "sku": "BAD" }] }
            """));
  }

  @Test
  void shouldFailWildcardConditionWhenItResolvesToNoElements() {
    assertTrue(
        ConditionEvaluator.isRequestInvalid(
            List.of(condition("items[*].sku", ConditionOperator.CONTAINS, "SKU-")),
            """
            { "items": [] }
            """));
  }

  @Test
  void shouldCombineConditionsWithAndLogic() {
    List<Condition> conditions =
        List.of(
            condition("customer.lastName", ConditionOperator.EQUALS, "Doe"),
            condition("amount", ConditionOperator.GREATER_THAN, 100),
            condition("amount", ConditionOperator.LESS_THAN, 200));

    assertFalse(
        ConditionEvaluator.isRequestInvalid(
            conditions,
            """
            {
              "customer": { "lastName": "Doe" },
              "amount": 150
            }
            """));

    assertTrue(
        ConditionEvaluator.isRequestInvalid(
            conditions,
            """
            {
              "customer": { "lastName": "Doe" },
              "amount": 90
            }
            """));
  }

  @Test
  void shouldInvalidateRequestForMalformedConditionDefinitions() {
    assertTrue(
        ConditionEvaluator.isRequestInvalid(
            List.of(new Condition(null, ConditionOperator.EQUALS, "Doe")),
            """
            { "customer": { "lastName": "Doe" } }
            """));

    assertTrue(
        ConditionEvaluator.isRequestInvalid(
            List.of(new Condition("customer.lastName", null, "Doe")),
            """
            { "customer": { "lastName": "Doe" } }
            """));
  }

  private static Condition condition(String field, ConditionOperator operator, Object value) {
    return new Condition(field, operator, value);
  }
}
