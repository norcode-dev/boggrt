package dev.norcode.configuration;

import io.vertx.core.http.HttpMethod;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class FileLoader implements ConfigurationLoader {

  private final AppConfiguration appConfiguration;

  public FileLoader(AppConfiguration appConfiguration) {
    this.appConfiguration = appConfiguration;
  }

  @Override
  public EndpointConfiguration get() {
    log.info("Loading endpoint configuration from {}", appConfiguration.endpointsFolderPath());
    return new EndpointConfiguration(HttpMethod.GET);
  }
}
