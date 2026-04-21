package dev.norcode.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.norcode.configuration.Condition;
import dev.norcode.configuration.ConditionOperator;
import dev.norcode.configuration.EndpointConfiguration;
import io.vertx.core.http.HttpMethod;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class FileEndpointConfigurationParser implements EndpointConfigurationParser<Path> {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public Set<EndpointConfiguration> parse(Set<Path> paths) {

    if (paths.isEmpty()) {
      log.error("No configuration files found");
    }

    return paths.stream()
        .map(
            path -> {
              try {
                return readEndpointConfigurationsFromFile(path);
              } catch (IOException e) {
                var errorMessage = "Failed to parse endpoint configuration from " + path;
                log.error(errorMessage, e);
                throw new ParserException(errorMessage, e);
              }
            })
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
  }

  private Set<EndpointConfiguration> readEndpointConfigurationsFromFile(Path path)
      throws IOException {

    log.info("Parsing endpoint configuration from {}", path);

    JsonNode jsonNode = objectMapper.readTree(path.toFile());
    if (jsonNode.isArray()) {
      return jsonNode
          .valueStream()
          .map(this::parseEndpointConfiguration)
          .collect(Collectors.toSet());
    }
    return Set.of(parseEndpointConfiguration(jsonNode));
  }

  private EndpointConfiguration parseEndpointConfiguration(JsonNode jsonNode) {
    List<Condition> conditions = extractConditions(jsonNode);

    return new EndpointConfiguration(
        HttpMethod.valueOf(jsonNode.get("method").asText()),
        jsonNode.get("path").asText(),
        conditions,
        jsonNode.get("response").toPrettyString());
  }

  private List<Condition> extractConditions(JsonNode jsonNode) {

    if (!jsonNode.hasNonNull("conditions") || jsonNode.get("conditions").isEmpty()) {
      return Collections.emptyList();
    }

    if (!jsonNode.get("conditions").isArray()) {
      throw new ParserException("Invalid conditions format. Conditions must be an array.");
    }

    return StreamSupport.stream(jsonNode.get("conditions").spliterator(), false)
        .map(this::parseCondition)
        .collect(Collectors.toList());
  }

  private Condition parseCondition(JsonNode conditionNode) {

    if (!conditionNode.has("field")) {
      throw new ParserException("Condition must have a field");
    }

    String field = conditionNode.get("field").asText();

    if (!conditionNode.has("operator")) {
      throw new ParserException("Condition must have an operator");
    }

    String operatorValue = conditionNode.get("operator").asText();
    ConditionOperator operator = ConditionOperator.fromValue(operatorValue);

    Object value = parseConditionValue(conditionNode);

    return new Condition(field, operator, value);
  }

  private static Object parseConditionValue(JsonNode conditionNode) {
    if (!conditionNode.has("value")) {
      throw new ParserException("Condition must have a value");
    }

    JsonNode valueNode = conditionNode.get("value");

    if (valueNode.isTextual()) {
      return valueNode.asText();
    }

    if (valueNode.isNumber()) {
      if (valueNode.isIntegralNumber()) {
        return valueNode.asLong();
      } else {
        return valueNode.asDouble();
      }
    }

    if (valueNode.isBoolean()) {
      return valueNode.asBoolean();
    }

    throw new ParserException("Invalid value for condition");
  }
}
