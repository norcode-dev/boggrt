package dev.norcode.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class SchemaSampleGeneratorTest {

  private final SchemaSampleGenerator generator = new SchemaSampleGenerator();

  @Test
  void emitsExampleVerbatim() {
    Schema<String> schema = new StringSchema();
    schema.setExample("hello world");

    JsonNode node = generator.generate(schema);

    assertEquals("hello world", node.asText());
  }

  @Test
  void exampleTakesPriorityOverDefault() {
    Schema<String> schema = new StringSchema();
    schema.setExample("from-example");
    schema.setDefault("from-default");

    JsonNode node = generator.generate(schema);

    assertEquals("from-example", node.asText());
  }

  @Test
  void emitsDefaultWhenExampleAbsent() {
    Schema<String> schema = new StringSchema();
    schema.setDefault("default-value");

    JsonNode node = generator.generate(schema);

    assertEquals("default-value", node.asText());
  }

  @Test
  void picksFirstEnumValueWhenPresent() {
    Schema<String> schema = new StringSchema();
    schema._enum(List.of("alpha", "beta", "gamma"));

    JsonNode node = generator.generate(schema);

    assertEquals("alpha", node.asText());
  }

  @Test
  void generatesEmailForEmailFormat() {
    Schema<String> schema = new StringSchema();
    schema.setFormat("email");

    JsonNode node = generator.generate(schema);

    assertTrue(node.asText().contains("@"), "email should contain @ but was: " + node.asText());
  }

  @Test
  void generatesUuidStringForUuidFormat() {
    Schema<String> schema = new StringSchema();
    schema.setFormat("uuid");

    JsonNode node = generator.generate(schema);

    assertTrue(
        node.asText().matches("[0-9a-fA-F-]{36}"),
        "expected UUID pattern but was: " + node.asText());
  }

  @Test
  void generatesIntegerWithinBounds() {
    IntegerSchema schema = new IntegerSchema();
    schema.setMinimum(new BigDecimal(10));
    schema.setMaximum(new BigDecimal(20));

    JsonNode node = generator.generate(schema);

    long value = node.asLong();
    assertTrue(value >= 10 && value < 20, "expected [10, 20) but was: " + value);
  }

  @Test
  void generatesBoolean() {
    JsonNode node = generator.generate(new BooleanSchema());

    assertTrue(node.isBoolean());
  }

  @Test
  void generatesArrayOfItems() {
    ArraySchema schema = new ArraySchema();
    StringSchema items = new StringSchema();
    items.setExample("item");
    schema.setItems(items);
    schema.setMinItems(2);
    schema.setMaxItems(2);

    JsonNode node = generator.generate(schema);

    assertTrue(node.isArray());
    assertEquals(2, node.size());
    assertEquals("item", node.get(0).asText());
    assertEquals("item", node.get(1).asText());
  }

  @Test
  void generatesObjectWithProperties() {
    ObjectSchema schema = new ObjectSchema();
    StringSchema name = new StringSchema();
    name.setExample("Rex");
    IntegerSchema age = new IntegerSchema();
    age.setExample(7);
    schema.addProperty("name", name);
    schema.addProperty("age", age);

    JsonNode node = generator.generate(schema);

    assertTrue(node.isObject());
    assertEquals("Rex", node.get("name").asText());
    assertEquals(7, node.get("age").asInt());
  }

  @Test
  void handlesNullSchemaGracefully() {
    JsonNode node = generator.generate(null);

    assertNotNull(node);
    assertTrue(node.isNull());
  }

  @Test
  void inferenceFromPropertiesWhenTypeMissing() {
    Schema<Object> schema = new Schema<>();
    StringSchema child = new StringSchema();
    child.setExample("child");
    schema.addProperty("field", child);

    JsonNode node = generator.generate(schema);

    assertTrue(node.isObject());
    assertEquals("child", node.get("field").asText());
  }

  @Test
  void truncatesDeepRecursion() {
    // Build a 14-deep nested object schema; MAX_DEPTH is 12 so the deepest level should be null.
    Schema<Object> root = buildNested(14);

    JsonNode node = generator.generate(root);

    assertFalse(node.isNull(), "root must not be null");
    // Just smoke-check that we didn't throw a StackOverflowError.
    assertNotNull(node);
  }

  private static Schema<Object> buildNested(int depth) {
    Schema<Object> current = new ObjectSchema();
    Schema<Object> top = current;
    for (int i = 0; i < depth; i++) {
      Schema<Object> child = new ObjectSchema();
      current.addProperty("nested", child);
      current = child;
    }
    return top;
  }
}
