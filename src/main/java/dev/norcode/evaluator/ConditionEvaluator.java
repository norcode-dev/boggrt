package dev.norcode.evaluator;

import com.fasterxml.jackson.databind.JsonNode;
import dev.norcode.configuration.Condition;
import dev.norcode.configuration.ConditionOperator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConditionEvaluator {

  public static boolean isRequestInvalid(List<Condition> conditions, String requestBody) {
    if (conditions == null || conditions.isEmpty()) {
      return false;
    }

    Optional<JsonNode> requestJson = RequestParser.parseRequestBody(requestBody);
    if (requestJson.isEmpty()) {
      return true;
    }

    for (Condition condition : conditions) {
      if (!evaluate(condition, requestJson.get())) {
        log.debug("Condition failed: {}", condition);
        return true;
      }
    }

    return false;
  }

  private static boolean evaluate(Condition condition, JsonNode requestJson) {
    if (condition == null
        || condition.field() == null
        || condition.field().isBlank()
        || condition.operator() == null) {
      return false;
    }

    List<JsonNode> fieldValues = resolveField(condition.field(), requestJson);

    if (condition.field().contains("[*]")) {
      if (fieldValues.isEmpty()) {
        return false;
      }

      for (JsonNode fieldValue : fieldValues) {
        if (!evaluateSingle(condition.operator(), fieldValue, condition.value())) {
          return false;
        }
      }
      return true;
    }

    if (fieldValues.isEmpty()) {
      return false;
    }

    return evaluateSingle(condition.operator(), fieldValues.getFirst(), condition.value());
  }

  private static List<JsonNode> resolveField(String fieldPath, JsonNode requestJson) {
    List<JsonNode> currentNodes = new ArrayList<>();
    currentNodes.add(requestJson);

    for (String segment : fieldPath.split("\\.")) {
      List<JsonNode> nextNodes = new ArrayList<>();

      for (JsonNode currentNode : currentNodes) {
        nextNodes.addAll(resolveSegment(currentNode, segment));
      }

      currentNodes = nextNodes;
      if (currentNodes.isEmpty()) {
        return List.of();
      }
    }

    return currentNodes;
  }

  private static List<JsonNode> resolveSegment(JsonNode node, String segment) {
    if (segment.endsWith("]") && segment.contains("[")) {
      return resolveArraySegment(node, segment);
    }

    return resolveObjectField(node, segment);
  }

  private static List<JsonNode> resolveObjectField(JsonNode node, String fieldName) {
    JsonNode next = node.get(fieldName);
    if (next == null) {
      return List.of();
    }

    return List.of(next);
  }

  private static List<JsonNode> resolveArraySegment(JsonNode node, String segment) {
    int start = segment.indexOf('[');
    int end = segment.lastIndexOf(']');

    if (start < 0 || end != segment.length() - 1 || start >= end) {
      return List.of();
    }

    String fieldName = segment.substring(0, start);
    String indexExpression = segment.substring(start + 1, end);

    JsonNode arrayNode = fieldName.isEmpty() ? node : node.get(fieldName);
    if (arrayNode == null || !arrayNode.isArray()) {
      return List.of();
    }

    if ("*".equals(indexExpression)) {
      List<JsonNode> values = new ArrayList<>();
      arrayNode.forEach(values::add);
      return values;
    }

    try {
      int index = Integer.parseInt(indexExpression);
      if (index < 0 || !arrayNode.has(index)) {
        return List.of();
      }

      return List.of(arrayNode.get(index));
    } catch (NumberFormatException e) {
      return List.of();
    }
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
