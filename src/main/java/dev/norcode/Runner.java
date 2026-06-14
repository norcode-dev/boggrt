package dev.norcode;

import dev.norcode.configuration.ConfigurationLoader;
import dev.norcode.configuration.EndpointConfiguration;
import dev.norcode.parser.EndpointConfigurationParser;
import dev.norcode.router.RouterConfiguration;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.common.annotation.Identifier;
import io.vertx.core.http.HttpMethod;
import jakarta.enterprise.event.Observes;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Runner {

  private final ConfigurationLoader jsonConfigurationLoader;
  private final ConfigurationLoader openApiConfigurationLoader;
  private final EndpointConfigurationParser<Path> jsonEndpointParser;
  private final EndpointConfigurationParser<Path> openApiEndpointParser;
  private final RouterConfiguration routerConfiguration;

  public Runner(
      @Identifier("json") ConfigurationLoader jsonConfigurationLoader,
      @Identifier("openapi") ConfigurationLoader openApiConfigurationLoader,
      @Identifier("json") EndpointConfigurationParser<Path> jsonEndpointParser,
      @Identifier("openapi") EndpointConfigurationParser<Path> openApiEndpointParser,
      RouterConfiguration routerConfiguration) {
    this.jsonConfigurationLoader = jsonConfigurationLoader;
    this.openApiConfigurationLoader = openApiConfigurationLoader;
    this.jsonEndpointParser = jsonEndpointParser;
    this.openApiEndpointParser = openApiEndpointParser;
    this.routerConfiguration = routerConfiguration;
  }

  void installRoutes(@Observes StartupEvent startupEvent) {
    Set<EndpointConfiguration> openApiEndpoints =
        openApiEndpointParser.parse(openApiConfigurationLoader.get());
    Set<EndpointConfiguration> jsonEndpoints =
        jsonEndpointParser.parse(jsonConfigurationLoader.get());

    routerConfiguration.configure(merge(openApiEndpoints, jsonEndpoints));
  }

  private Set<EndpointConfiguration> merge(
      Set<EndpointConfiguration> openApiEndpoints, Set<EndpointConfiguration> jsonEndpoints) {

    Map<EndpointKey, EndpointConfiguration> merged = new HashMap<>();
    for (EndpointConfiguration endpoint : openApiEndpoints) {
      merged.put(EndpointKey.of(endpoint), endpoint);
    }
    for (EndpointConfiguration endpoint : jsonEndpoints) {
      EndpointKey key = EndpointKey.of(endpoint);
      if (merged.containsKey(key)) {
        log.warn(
            "Endpoint override: JSON config replaces OpenAPI-generated mock for {} {}",
            endpoint.method(),
            endpoint.path());
      }
      merged.put(key, endpoint);
    }
    return new HashSet<>(merged.values());
  }

  private record EndpointKey(HttpMethod method, String path) {
    static EndpointKey of(EndpointConfiguration endpoint) {
      return new EndpointKey(endpoint.method(), endpoint.path());
    }
  }
}
