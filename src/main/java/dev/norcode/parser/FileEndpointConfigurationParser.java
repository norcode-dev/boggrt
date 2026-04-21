package dev.norcode.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.norcode.configuration.EndpointConfiguration;
import io.vertx.core.http.HttpMethod;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class FileEndpointConfigurationParser implements EndpointConfigurationParser<Path> {

  @Override
  public Set<EndpointConfiguration> parse(Set<Path> paths) {
    ObjectMapper objectMapper = new ObjectMapper();

    return paths.stream()
        .map(
            path -> {
              try {
                log.info("Parsing endpoint configuration from {}", path);
                JsonNode jsonNode = objectMapper.readTree(path.toFile());
                if (jsonNode.isArray()) {
                  return jsonNode
                      .valueStream()
                      .map(FileEndpointConfigurationParser::parseEndpointConfiguration)
                      .collect(Collectors.toSet());
                }
                return Set.of(parseEndpointConfiguration(jsonNode));
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            })
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
  }

  private static EndpointConfiguration parseEndpointConfiguration(JsonNode jsonNode) {
    return new EndpointConfiguration(
        HttpMethod.valueOf(jsonNode.get("method").asText()),
        jsonNode.get("path").asText(),
        jsonNode.get("response").toPrettyString());
  }
}
