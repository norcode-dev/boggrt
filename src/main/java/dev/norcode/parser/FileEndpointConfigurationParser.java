package dev.norcode.parser;

import dev.norcode.configuration.EndpointConfiguration;
import io.vertx.core.http.HttpMethod;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.file.Path;
import java.util.Set;

@ApplicationScoped
public class FileEndpointConfigurationParser implements EndpointConfigurationParser<Path> {

  @Override
  public EndpointConfiguration parse(Set<Path> paths) {
    paths.forEach(System.out::println);
    return new EndpointConfiguration(HttpMethod.GET);
  }
}
