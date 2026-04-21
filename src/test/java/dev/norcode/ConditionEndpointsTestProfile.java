package dev.norcode;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

public class ConditionEndpointsTestProfile implements QuarkusTestProfile {
  @Override
  public Map<String, String> getConfigOverrides() {
    return Map.of("boggrt.endpoints-folder-path", "src/test/resources/conditions");
  }
}
