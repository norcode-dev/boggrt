package dev.norcode.configuration;

public record Condition(String field, ConditionOperator operator, Object value) {}
