package dev.norcode.evaluator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import dev.norcode.configuration.Condition;
import dev.norcode.configuration.ConditionOperator;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConditionEvaluator {

  private static final Configuration JSON_PATH_CONFIGURATION =
      Configuration.builder()
          .jsonProvider(new JacksonJsonNodeJsonProvider())
          .options(Option.ALWAYS_RETURN_LIST, Option.SUPPRESS_EXCEPTIONS)
          .build();

  public static boolean isRequestValid(List<Condition> conditions, String requestBody) {
    if (conditions == null || conditions.isEmpty()) {
      return true;
    }

    Optional<JsonNode> requestJson = RequestParser.parseRequestBody(requestBody);
    if (requestJson.isEmpty()) {
      return false;
    }

    for (Condition condition : conditions) {
      if (!evaluate(condition, requestBody)) {
        log.debug("Condition failed: {}", condition);
        return false;
      }
    }

    return true;
  }

  public static boolean isRequestInvalid(List<Condition> conditions, String requestBody) {
    return !isRequestValid(conditions, requestBody);
  }

  private static boolean evaluate(Condition condition, String requestBody) {
    if (condition == null
        || condition.field() == null
        || condition.field().isBlank()
        || condition.operator() == null) {
      return false;
    }

    ArrayNode nodes =
        JsonPath.using(JSON_PATH_CONFIGURATION)
            .parse(requestBody)
            .read("$.".concat(condition.field()));

    if (nodes.isEmpty()) {
      return false;
    }

    for (JsonNode node : nodes) {
      if (evaluateSingle(condition.operator(), node, condition.value())) {
        return true;
      }
    }
    return false;
  }

  private static boolean evaluateSingle(
      ConditionOperator operator, JsonNode fieldValue, Object expectedValue) {
    if (fieldValue == null || fieldValue.isNull()) {
      return false;
    }

    return switch (operator) {
      case EQUALS -> evaluateEquals(fieldValue, expectedValue);
      case CONTAINS -> evaluateContains(fieldValue, expectedValue);
      case GREATER_THAN -> evaluateGreaterThan(fieldValue, expectedValue);
      case LESS_THAN -> evaluateLessThan(fieldValue, expectedValue);
      case IS_EMPTY -> evaluateIsEmpty(fieldValue);
      case SIZE_EQUALS -> evaluateSizeEquals(fieldValue, expectedValue);
      case SIZE_GREATER_THAN -> evaluateSizeGreaterThan(fieldValue, expectedValue);
      case SIZE_LESS_THAN -> evaluateSizeLessThan(fieldValue, expectedValue);
      case EXISTS -> true;
    };
  }

  private static boolean evaluateEquals(JsonNode fieldValue, Object expectedValue) {
    if (fieldValue.isTextual()) {
      return expectedValue instanceof String && fieldValue.asText().equals(expectedValue);
    }

    if (fieldValue.isNumber()) {
      if (!(expectedValue instanceof Number expectedNumber)) {
        return false;
      }

      BigDecimal actual = fieldValue.decimalValue();
      BigDecimal expected = new BigDecimal(expectedNumber.toString());
      return actual.compareTo(expected) == 0;
    }

    if (fieldValue.isBoolean()) {
      return expectedValue instanceof Boolean && fieldValue.asBoolean() == (Boolean) expectedValue;
    }

    return false;
  }

  private static boolean evaluateContains(JsonNode fieldValue, Object expectedValue) {
    return fieldValue.isTextual()
        && expectedValue != null
        && fieldValue.asText().contains(String.valueOf(expectedValue));
  }

  private static boolean evaluateGreaterThan(JsonNode fieldValue, Object expectedValue) {
    if (!fieldValue.isNumber() || !(expectedValue instanceof Number expectedNumber)) {
      return false;
    }

    BigDecimal actual = fieldValue.decimalValue();
    BigDecimal expected = new BigDecimal(expectedNumber.toString());
    return actual.compareTo(expected) > 0;
  }

  private static boolean evaluateLessThan(JsonNode fieldValue, Object expectedValue) {
    if (!fieldValue.isNumber() || !(expectedValue instanceof Number expectedNumber)) {
      return false;
    }

    BigDecimal actual = fieldValue.decimalValue();
    BigDecimal expected = new BigDecimal(expectedNumber.toString());
    return actual.compareTo(expected) < 0;
  }

  private static boolean evaluateIsEmpty(JsonNode fieldValue) {
    if (fieldValue.isArray()) {
      return fieldValue.isEmpty();
    }

    if (fieldValue.isTextual()) {
      return fieldValue.asText().isEmpty();
    }

    return false;
  }

  private static boolean evaluateSizeEquals(JsonNode fieldValue, Object expectedValue) {
    Integer actualSize = getSupportedSize(fieldValue);
    Integer expectedSize = getExpectedSize(expectedValue);
    return actualSize != null && actualSize.equals(expectedSize);
  }

  private static boolean evaluateSizeGreaterThan(JsonNode fieldValue, Object expectedValue) {
    Integer actualSize = getSupportedSize(fieldValue);
    Integer expectedSize = getExpectedSize(expectedValue);
    return actualSize != null && expectedSize != null && actualSize > expectedSize;
  }

  private static boolean evaluateSizeLessThan(JsonNode fieldValue, Object expectedValue) {
    Integer actualSize = getSupportedSize(fieldValue);
    Integer expectedSize = getExpectedSize(expectedValue);
    return actualSize != null && expectedSize != null && actualSize < expectedSize;
  }

  private static Integer getSupportedSize(JsonNode fieldValue) {
    if (fieldValue.isArray()) {
      return fieldValue.size();
    }

    if (fieldValue.isTextual()) {
      return fieldValue.asText().length();
    }

    return null;
  }

  private static Integer getExpectedSize(Object expectedValue) {
    if (!(expectedValue instanceof Number numberValue)) {
      return null;
    }

    if (numberValue instanceof Double || numberValue instanceof Float) {
      double doubleValue = numberValue.doubleValue();
      if (doubleValue % 1 != 0) {
        return null;
      }
    }

    return numberValue.intValue();
  }
}
