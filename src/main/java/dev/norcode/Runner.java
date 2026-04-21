package dev.norcode;

import dev.norcode.configuration.ConfigurationLoader;
import dev.norcode.configuration.EndpointConfiguration;
import dev.norcode.parser.EndpointConfigurationParser;
import io.quarkus.runtime.StartupEvent;
import io.vertx.ext.web.Router;
import jakarta.enterprise.event.Observes;
import java.nio.file.Path;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Runner {

  private final ConfigurationLoader configurationLoader;
  private final EndpointConfigurationParser<Path> endpointConfigurationParser;

  public Runner(
      ConfigurationLoader configurationLoader,
      EndpointConfigurationParser<Path> endpointConfigurationParser) {
    this.configurationLoader = configurationLoader;
    this.endpointConfigurationParser = endpointConfigurationParser;
  }

  void installRoute(@Observes StartupEvent startupEvent, Router router) {

    log.info("Installing routes");
    Set<Path> paths = configurationLoader.get();
    Set<EndpointConfiguration> endpointConfiguration = endpointConfigurationParser.parse(paths);
    router
        .route()
        .path("/hello")
        .method(endpointConfiguration.stream().findFirst().get().method())
        .handler(
            routingContext ->
                routingContext
                    .response()
                    .putHeader("content-type", "application/json")
                    .end(endpointConfiguration.stream().findFirst().get().response()));
  }
}
