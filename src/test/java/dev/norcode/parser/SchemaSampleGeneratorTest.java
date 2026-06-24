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
    // The first element honors the item example; the rest are freshly generated.
    assertEquals("item", node.get(0).asText());
  }

  @Test
  void includesItemExampleOnceThenFillsWithGeneratedItems() {
    ArraySchema schema = new ArraySchema();
    ObjectSchema items = new ObjectSchema();
    StringSchema id = new StringSchema();
    id.setFormat("uuid");
    items.addProperty("id", id);
    StringSchema name = new StringSchema();
    items.addProperty("name", name);
    items.setExample(java.util.Map.of("id", "fixed-id", "name", "Example"));
    schema.setItems(items);

    JsonNode node = generator.generate(schema);

    assertTrue(node.isArray());
    assertTrue(node.size() >= 5 && node.size() <= 10, "expected 5..10 items but was: " + node.size());
    assertEquals("fixed-id", node.get(0).get("id").asText());
    boolean someDiffer = false;
    for (int i = 1; i < node.size(); i++) {
      if (!"fixed-id".equals(node.get(i).get("id").asText())) {
        someDiffer = true;
        break;
      }
    }
    assertTrue(someDiffer, "filler items should differ from the example, but all were identical");
  }

  @Test
  void respectsArrayLevelExampleWithMultipleElements() {
    // An explicit example on the array schema itself is honored verbatim — all of its elements.
    ArraySchema schema = new ArraySchema();
    schema.setItems(new StringSchema());
    schema.setExample(List.of("a", "b", "c"));

    JsonNode node = generator.generate(schema);

    assertTrue(node.isArray());
    assertEquals(3, node.size());
    assertEquals("a", node.get(0).asText());
    assertEquals("b", node.get(1).asText());
    assertEquals("c", node.get(2).asText());
  }

  @Test
  void generatesItemsWhenArrayLevelExampleIsEmpty() {
    // An empty array example is a placeholder, not a real sample response: the array is generated.
    ArraySchema schema = new ArraySchema();
    StringSchema items = new StringSchema();
    items.setExample("item");
    schema.setItems(items);
    schema.setExample(List.of());

    JsonNode node = generator.generate(schema);

    assertTrue(node.isArray());
    assertTrue(node.size() >= 5 && node.size() <= 8, "expected 5..8 items but was: " + node.size());
  }

  @Test
  void respectsArrayOfObjectsExampleWithMultipleElements() {
    ArraySchema schema = new ArraySchema();
    ObjectSchema item = new ObjectSchema();
    item.addProperty("id", new StringSchema());
    schema.setItems(item);
    schema.setExample(
        List.of(java.util.Map.of("id", "first"), java.util.Map.of("id", "second")));

    JsonNode node = generator.generate(schema);

    assertEquals(2, node.size());
    assertEquals("first", node.get(0).get("id").asText());
    assertEquals("second", node.get(1).get("id").asText());
  }

  @Test
  void respectsPopulatedArrayExampleNestedInObjectExample() {
    // A wrapper (e.g. a Page envelope) whose object-level example provides a *populated* data array
    // should honor that array verbatim rather than generating its own items.
    ObjectSchema wrapper = new ObjectSchema();
    ArraySchema data = new ArraySchema();
    ObjectSchema item = new ObjectSchema();
    item.addProperty("id", new StringSchema());
    data.setItems(item);
    wrapper.addProperty("data", data);
    wrapper.setExample(
        java.util.Map.of(
            "data",
            List.of(java.util.Map.of("id", "x1"), java.util.Map.of("id", "x2"))));

    JsonNode node = generator.generate(wrapper);

    assertEquals(2, node.get("data").size());
    assertEquals("x1", node.get("data").get(0).get("id").asText());
    assertEquals("x2", node.get("data").get(1).get("id").asText());
  }

  @Test
  void fillsArrayPropertyEvenWhenObjectExampleHasEmptyArray() {
    // Mirrors a paginated list response (allOf Page + data): the merged object carries an
    // object-level example whose `data` is empty, but `data` is a real array of items. The empty
    // example array must not shadow generation.
    ObjectSchema wrapper = new ObjectSchema();

    ObjectSchema item = new ObjectSchema();
    StringSchema id = new StringSchema();
    item.addProperty("id", id);
    item.setExample(java.util.Map.of("id", "client-1"));
    ArraySchema data = new ArraySchema();
    data.setItems(item);
    wrapper.addProperty("data", data);

    ObjectSchema pagination = new ObjectSchema();
    IntegerSchema totalItems = new IntegerSchema();
    pagination.addProperty("totalItems", totalItems);
    pagination.setExample(java.util.Map.of("totalItems", 137));
    wrapper.addProperty("pagination", pagination);

    wrapper.setExample(
        java.util.Map.of("data", java.util.List.of(), "pagination", java.util.Map.of("totalItems", 137)));

    JsonNode node = generator.generate(wrapper);

    assertTrue(node.get("data").isArray());
    assertTrue(
        node.get("data").size() >= 5 && node.get("data").size() <= 10,
        "expected data filled with 5..10 items but was: " + node.get("data").size());
    assertEquals("client-1", node.get("data").get(0).get("id").asText());
    assertEquals(137, node.get("pagination").get("totalItems").asInt());
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
