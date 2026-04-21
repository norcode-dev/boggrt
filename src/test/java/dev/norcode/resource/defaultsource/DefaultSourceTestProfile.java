package dev.norcode.resource.defaultsource;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

public class DefaultSourceTestProfile implements QuarkusTestProfile {
  @Override
  public Map<String, String> getConfigOverrides() {
    return Map.of();
  }
}
