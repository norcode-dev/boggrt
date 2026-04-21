package dev.norcode;

import dev.norcode.configuration.ConfigurationLoader;
import dev.norcode.configuration.EndpointConfiguration;
import dev.norcode.parser.EndpointConfigurationParser;
import dev.norcode.router.RouterConfiguration;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import java.nio.file.Path;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Runner {

  private final ConfigurationLoader configurationLoader;
  private final EndpointConfigurationParser<Path> endpointConfigurationParser;
  private final RouterConfiguration routerConfiguration;

  public Runner(
      ConfigurationLoader configurationLoader,
      EndpointConfigurationParser<Path> endpointConfigurationParser,
      RouterConfiguration routerConfiguration) {
    this.configurationLoader = configurationLoader;
    this.endpointConfigurationParser = endpointConfigurationParser;
    this.routerConfiguration = routerConfiguration;
  }

  void installRoute(@Observes StartupEvent startupEvent) {

    log.info("Installing routes");
    Set<Path> paths = configurationLoader.get();
    Set<EndpointConfiguration> endpointConfiguration = endpointConfigurationParser.parse(paths);

    if (endpointConfiguration.isEmpty()) {
      log.error("No endpoint configuration found");
      //TODO throw exception when no endpoint configuration is found
    }

    log.info("Found {} endpoint configuration(s)", endpointConfiguration.size());

    routerConfiguration.configure(endpointConfiguration);
  }
}
