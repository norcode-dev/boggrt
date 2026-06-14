package dev.norcode.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.models.media.Schema;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;

@Slf4j
@ApplicationScoped
public class SchemaSampleGenerator {

  private static final JsonNodeFactory NODES = JsonNodeFactory.instance;
  private static final int MAX_DEPTH = 12;

  private final Faker faker = new Faker();
  private final ObjectMapper objectMapper = new ObjectMapper();

  public JsonNode generate(Schema<?> schema) {
    return generate(schema, 0);
  }

  private JsonNode generate(Schema<?> schema, int depth) {
    if (schema == null || depth > MAX_DEPTH) {
      return NODES.nullNode();
    }

    JsonNode userSupplied = userSuppliedValue(schema);
    if (userSupplied != null) {
      return userSupplied;
    }

    if (schema.getEnum() != null && !schema.getEnum().isEmpty()) {
      Object first = schema.getEnum().getFirst();
      return objectMapper.valueToTree(first);
    }

    String type = resolveType(schema);

    if (type == null) {
      if (schema.getProperties() != null && !schema.getProperties().isEmpty()) {
        type = "object";
      } else if (schema.getItems() != null) {
        type = "array";
      }
    }

    if (type == null) {
      return NODES.nullNode();
    }

    return switch (type) {
      case "string" -> NODES.textNode(generateString(schema));
      case "integer" -> NODES.numberNode(generateInteger(schema));
      case "number" -> NODES.numberNode(generateNumber(schema));
      case "boolean" -> NODES.booleanNode(faker.bool().bool());
      case "array" -> generateArray(schema, depth);
      case "object" -> generateObject(schema, depth);
      default -> NODES.nullNode();
    };
  }

  private JsonNode userSuppliedValue(Schema<?> schema) {
    Object example = schema.getExample();
    if (example != null) {
      return objectMapper.valueToTree(example);
    }
    Object defaultValue = schema.getDefault();
    if (defaultValue != null) {
      return objectMapper.valueToTree(defaultValue);
    }
    return null;
  }

  private String resolveType(Schema<?> schema) {
    if (schema.getType() != null) {
      return schema.getType();
    }
    if (schema.getTypes() != null) {
      return schema.getTypes().stream()
          .filter(t -> t != null && !"null".equals(t))
          .findFirst()
          .orElse(null);
    }
    return null;
  }

  private String generateString(Schema<?> schema) {
    String format = schema.getFormat();
    if (format == null) {
      return faker.lorem().word();
    }
    return switch (format) {
      case "email" -> faker.internet().emailAddress();
      case "uuid" -> UUID.randomUUID().toString();
      case "date" -> LocalDate.now().toString();
      case "date-time" -> Instant.now().toString();
      case "uri", "url" -> faker.internet().url();
      case "hostname" -> faker.internet().domainName();
      case "ipv4" -> faker.internet().ipV4Address();
      case "ipv6" -> faker.internet().ipV6Address();
      case "byte" -> java.util.Base64.getEncoder().encodeToString(faker.lorem().word().getBytes());
      case "password" -> faker.credentials().password();
      default -> faker.lorem().word();
    };
  }

  private long generateInteger(Schema<?> schema) {
    int min = schema.getMinimum() != null ? schema.getMinimum().intValue() : 1;
    int max = schema.getMaximum() != null ? schema.getMaximum().intValue() : 1000;
    if (max <= min) {
      max = min + 1;
    }
    return faker.number().numberBetween(min, max);
  }

  private double generateNumber(Schema<?> schema) {
    double min = schema.getMinimum() != null ? schema.getMinimum().doubleValue() : 0d;
    double max = schema.getMaximum() != null ? schema.getMaximum().doubleValue() : 1000d;
    if (max <= min) {
      max = min + 1d;
    }
    double range = max - min;
    return Math.round((min + faker.random().nextDouble() * range) * 100d) / 100d;
  }

  private ArrayNode generateArray(Schema<?> schema, int depth) {
    ArrayNode array = NODES.arrayNode();
    Schema<?> items = schema.getItems();
    int size = arraySize(schema);
    for (int i = 0; i < size; i++) {
      array.add(generate(items, depth + 1));
    }
    return array;
  }

  private int arraySize(Schema<?> schema) {
    int min = schema.getMinItems() != null ? schema.getMinItems() : 1;
    int max = schema.getMaxItems() != null ? schema.getMaxItems() : Math.max(min, 3);
    if (max < min) {
      max = min;
    }
    if (max == min) {
      return min;
    }
    return faker.number().numberBetween(min, max + 1);
  }

  private ObjectNode generateObject(Schema<?> schema, int depth) {
    ObjectNode node = NODES.objectNode();
    Map<String, Schema> properties = schema.getProperties();
    if (properties == null || properties.isEmpty()) {
      return node;
    }
    for (Map.Entry<String, Schema> entry : properties.entrySet()) {
      node.set(entry.getKey(), generate(entry.getValue(), depth + 1));
    }
    return node;
  }
}
