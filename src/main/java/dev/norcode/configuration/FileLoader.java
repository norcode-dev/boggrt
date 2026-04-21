package dev.norcode.configuration;

import io.vertx.core.http.HttpMethod;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FileLoader implements ConfigurationLoader {
  @Override
  public Configuration get() {
    return new Configuration(HttpMethod.GET);
  }
}
