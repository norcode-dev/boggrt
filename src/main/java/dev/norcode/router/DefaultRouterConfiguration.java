package dev.norcode.router;

import dev.norcode.configuration.EndpointConfiguration;
import io.vertx.ext.web.Router;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
@ApplicationScoped
public class DefaultRouterConfiguration implements RouterConfiguration {
  private final Router router;

  DefaultRouterConfiguration(Router router) {
    this.router = router;
  }

  @Override
  public void configure(Set<EndpointConfiguration> endpointConfiguration) {
    if (endpointConfiguration.isEmpty()) {
      log.error("No endpoint configuration found");
      // TODO throw exception when no endpoint configuration is found
    }

    log.info("Endpoint configurations found: {}", endpointConfiguration.size());

    endpointConfiguration.forEach(
        configuration -> {
          log.info("Configuring: {}", configuration.path());
          router
              .route()
              .path(configuration.path())
              .method(configuration.method())
              .handler(
                  routingContext ->
                      routingContext
                          .response()
                          .putHeader("content-type", "application/json")
                          .end(configuration.response()));
        });
  }
}
