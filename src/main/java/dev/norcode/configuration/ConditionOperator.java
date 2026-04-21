package dev.norcode.configuration;

public enum ConditionOperator {
  EQUALS("equals"),
  CONTAINS("contains"),
  GREATER_THAN("greaterThan"),
  LESS_THAN("lessThan"),
  IS_EMPTY("isEmpty"),
  SIZE_EQUALS("sizeEquals"),
  SIZE_GREATER_THAN("sizeGreaterThan"),
  SIZE_LESS_THAN("sizeLessThan"),
  EXISTS("exists");

  private final String value;

  ConditionOperator(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static ConditionOperator fromValue(String value) {
    for (ConditionOperator operator : ConditionOperator.values()) {
      if (operator.value.equals(value)) {
        return operator;
      }
    }
    throw new IllegalArgumentException("Unknown operator: " + value);
  }
}
