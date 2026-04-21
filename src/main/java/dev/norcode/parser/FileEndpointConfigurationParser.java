package dev.norcode.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.norcode.configuration.EndpointConfiguration;
import io.vertx.core.http.HttpMethod;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class FileEndpointConfigurationParser implements EndpointConfigurationParser<Path> {

  @Override
  public Set<EndpointConfiguration> parse(Set<Path> paths) {
    ObjectMapper objectMapper = new ObjectMapper();

    return paths.stream()
        .map(
            path -> {
              try {
                JsonNode jsonNode = objectMapper.readTree(path.toFile());
                return new EndpointConfiguration(
                    HttpMethod.valueOf(jsonNode.get("method").asText()),
                    jsonNode.get("path").asText(),
                    jsonNode.get("response").toPrettyString());
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            })
        .collect(Collectors.toSet());
  }
}
