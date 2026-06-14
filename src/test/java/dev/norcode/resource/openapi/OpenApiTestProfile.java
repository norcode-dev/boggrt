package dev.norcode.resource.openapi;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

public class OpenApiTestProfile implements QuarkusTestProfile {
  @Override
  public Map<String, String> getConfigOverrides() {
    return Map.of(
        "boggrt.endpoints-folder-path", "src/test/resources/openapi-empty",
        "boggrt.openapi-folder-path", "src/test/resources/openapi");
  }
}
