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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;

@Slf4j
@ApplicationScoped
public class SchemaSampleGenerator {

  private static final JsonNodeFactory NODES = JsonNodeFactory.instance;
  private static final int MAX_DEPTH = 12;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final Faker faker = new Faker();

  public JsonNode generate(Schema<?> schema) {
    return generate(schema, 0, true);
  }

  /**
   * @param honorExamples when {@code true}, an explicit {@code example}/{@code default} on the
   *     schema is emitted verbatim. When {@code false}, those are ignored so that fresh, varied
   *     values are generated instead — used to fill array elements beyond the first.
   */
  private JsonNode generate(Schema<?> schema, int depth, boolean honorExamples) {
    if (schema == null || depth > MAX_DEPTH) {
      return NODES.nullNode();
    }

    // An object that declares properties is always built field-by-field (see generateObject), even
    // when it carries an object-level example. Honoring such an example wholesale would emit it
    // verbatim — including any empty placeholder arrays — and never descend into the properties.
    // This is what breaks composed list responses (allOf of a Page envelope + a `data` array): the
    // Page example's `data: []` would shadow the generated items. generateObject spreads the example
    // across scalar leaves instead, while still generating array/object properties.
    boolean objectWithProperties = schema.getProperties() != null && !schema.getProperties().isEmpty();

    if (honorExamples && !objectWithProperties) {
      JsonNode userSupplied = userSuppliedValue(schema);
      if (userSupplied != null) {
        return userSupplied;
      }
    }

    if (schema.getEnum() != null && !schema.getEnum().isEmpty()) {
      Object first = schema.getEnum().getFirst();
      return OBJECT_MAPPER.valueToTree(first);
    }

    String type = resolveType(schema);
    if (type == null) {
      return NODES.nullNode();
    }

    return switch (type) {
      case "string" -> NODES.textNode(generateString(schema));
      case "integer" -> NODES.numberNode(generateInteger(schema));
      case "number" -> NODES.numberNode(generateNumber(schema));
      case "boolean" -> NODES.booleanNode(faker.bool().bool());
      case "array" -> generateArray(schema, depth, honorExamples);
      case "object" -> generateObject(schema, depth, honorExamples);
      default -> NODES.nullNode();
    };
  }

  private JsonNode userSuppliedValue(Schema<?> schema) {
    Object example = schema.getExample();
    if (example != null) {
      return OBJECT_MAPPER.valueToTree(example);
    }
    Object defaultValue = schema.getDefault();
    if (defaultValue != null) {
      return OBJECT_MAPPER.valueToTree(defaultValue);
    }
    return null;
  }

  /**
   * Determines the effective type of the schema, falling back to inference from its shape when no
   * explicit {@code type}/{@code types} is declared.
   */
  private String resolveType(Schema<?> schema) {
    if (schema.getType() != null) {
      return schema.getType();
    }
    if (schema.getTypes() != null) {
      String declared =
          schema.getTypes().stream()
              .filter(t -> t != null && !"null".equals(t))
              .findFirst()
              .orElse(null);
      if (declared != null) {
        return declared;
      }
    }
    if (schema.getProperties() != null && !schema.getProperties().isEmpty()) {
      return "object";
    }
    if (schema.getItems() != null) {
      return "array";
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

  private ArrayNode generateArray(Schema<?> schema, int depth, boolean honorExamples) {
    ArrayNode array = NODES.arrayNode();
    Schema<?> items = schema.getItems();
    int size = arraySize(schema);
    for (int i = 0; i < size; i++) {
      // Emit the item example verbatim as the first element, then fill the rest with freshly
      // generated, varied items so the array isn't just the same object repeated.
      boolean honorThis = honorExamples && i == 0;
      array.add(generate(items, depth + 1, honorThis));
    }
    return array;
  }

  private int arraySize(Schema<?> schema) {
    if (schema.getMinItems() == null && schema.getMaxItems() == null) {
      return faker.number().numberBetween(5, 11);
    }
    int min = schema.getMinItems() != null ? schema.getMinItems() : 1;
    int max = schema.getMaxItems() != null ? schema.getMaxItems() : Math.max(min, 10);
    if (max < min) {
      max = min;
    }
    if (max == min) {
      return min;
    }
    return faker.number().numberBetween(min, max + 1);
  }

  @SuppressWarnings("rawtypes") // Schema.getProperties() returns a raw Map from the Swagger API.
  private ObjectNode generateObject(Schema<?> schema, int depth, boolean honorExamples) {
    ObjectNode node = NODES.objectNode();
    Map<String, Schema> properties = schema.getProperties();
    if (properties == null || properties.isEmpty()) {
      return node;
    }
    // When honoring examples, an object-level example supplies values for scalar properties that
    // have none of their own. Array and object properties are always generated from their schemas
    // so composed wrappers (e.g. a Page envelope + a `data` array) fill their collections instead of
    // echoing an empty placeholder array from the example.
    Map<String, Object> exampleMap = honorExamples ? exampleAsMap(schema.getExample()) : null;
    for (Map.Entry<String, Schema> entry : properties.entrySet()) {
      String name = entry.getKey();
      Schema<?> property = entry.getValue();
      JsonNode value;
      if (exampleMap != null && exampleMap.containsKey(name) && isScalar(property) && !hasOwnValue(property)) {
        value = OBJECT_MAPPER.valueToTree(exampleMap.get(name));
      } else {
        value = generate(property, depth + 1, honorExamples);
      }
      node.set(name, value);
    }
    return node;
  }

  /** A scalar is anything that is not an object or array — i.e. a leaf we can copy from an example. */
  private boolean isScalar(Schema<?> schema) {
    String type = resolveType(schema);
    return type != null && !"object".equals(type) && !"array".equals(type);
  }

  /** True when the property carries its own example/default, which {@link #generate} already honors. */
  private boolean hasOwnValue(Schema<?> schema) {
    return schema.getExample() != null || schema.getDefault() != null;
  }

  /**
   * Normalizes an object-level example to a property map. The example may arrive as a plain {@code
   * Map} (programmatic schemas) or as a Jackson {@code ObjectNode} (parsed specs), so both are
   * funneled through Jackson into a uniform map keyed by property name.
   */
  private Map<String, Object> exampleAsMap(Object example) {
    if (example == null) {
      return null;
    }
    JsonNode tree = (example instanceof JsonNode node) ? node : OBJECT_MAPPER.valueToTree(example);
    if (tree == null || !tree.isObject()) {
      return null;
    }
    Map<String, Object> result = new LinkedHashMap<>();
    tree.fields().forEachRemaining(field -> result.put(field.getKey(), field.getValue()));
    return result;
  }
}
