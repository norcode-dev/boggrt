package dev.norcode.router;

import com.fasterxml.jackson.databind.JsonNode;
import dev.norcode.configuration.EndpointConfiguration;
import dev.norcode.evaluator.ConditionEvaluator;
import dev.norcode.evaluator.RequestParser;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class DefaultRouterConfiguration implements RouterConfiguration {
  private final Router router;

  DefaultRouterConfiguration(Router router) {
    this.router = router;
  }

  @Override
  public void configure(Set<EndpointConfiguration> endpointConfigurations) {
    if (endpointConfigurations.isEmpty()) {
      log.error("No endpoint configurations found");
      throw new RouterException(new IllegalArgumentException("No endpoint configurations found"));
    }

    log.info("Endpoint configurations found: {}", endpointConfigurations.size());

    endpointConfigurations.forEach(
        configuration -> {
          log.info("Configuring: {}", configuration.path());
          router
              .route()
              .path(configuration.path())
              .method(configuration.method())
              .handler(BodyHandler.create())
              .handler(
                  routingContext -> {
                    try {
                      handleConfiguredRoute(configuration, routingContext);
                    } catch (Exception e) {
                      log.error("Failed to handle configured route", e);
                      routingContext.fail(400);
                    }
                  });
        });
  }

  private void handleConfiguredRoute(
      EndpointConfiguration configuration, RoutingContext routingContext) {

    if (configuration.hasValidConditions()) {
      String requestBody = routingContext.body() != null ? routingContext.body().asString() : "";
      Optional<JsonNode> requestJson = RequestParser.parseRequestBody(requestBody);

      if (requestJson.isEmpty()) {
        log.debug("Invalid request body for {} {}", configuration.method(), configuration.path());
        routingContext.response().setStatusCode(400).end("Invalid request body.");
        return;
      }

      if (!ConditionEvaluator.isRequestValid(configuration.conditions(), requestJson.get())) {
        log.debug("Conditions not met for {} {}", configuration.method(), configuration.path());
        routingContext.response().setStatusCode(404).end("Response not found.");
        return;
      }
    }

    routingContext
        .response()
        .setStatusCode(configuration.resolvedResponseStatus())
        .putHeader("content-type", "application/json")
        .end(configuration.response());
  }
}
