package dev.norcode.parser;

import com.fasterxml.jackson.databind.JsonNode;
import dev.norcode.configuration.EndpointConfiguration;
import io.smallrye.common.annotation.Identifier;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.vertx.core.http.HttpMethod;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@Identifier("openapi")
public class OpenApiEndpointConfigurationParser implements EndpointConfigurationParser<Path> {

  private static final Pattern PATH_PARAM = Pattern.compile("\\{([^}]+)}");
  private static final String JSON_MEDIA_TYPE = "application/json";

  private final SchemaSampleGenerator schemaSampleGenerator;

  public OpenApiEndpointConfigurationParser(SchemaSampleGenerator schemaSampleGenerator) {
    this.schemaSampleGenerator = schemaSampleGenerator;
  }

  @Override
  public Set<EndpointConfiguration> parse(Set<Path> paths) {
    if (paths.isEmpty()) {
      return Set.of();
    }

    Set<EndpointConfiguration> result = new HashSet<>();
    for (Path path : paths) {
      result.addAll(parseSpec(path));
    }
    return result;
  }

  private Set<EndpointConfiguration> parseSpec(Path specPath) {
    log.info("Parsing OpenAPI specification from {}", specPath);

    ParseOptions options = new ParseOptions();
    options.setResolve(true);
    options.setResolveFully(true);

    SwaggerParseResult result =
        new OpenAPIV3Parser().readLocation(specPath.toUri().toString(), null, options);

    if (result.getMessages() != null) {
      result.getMessages().forEach(msg -> log.warn("OpenAPI parser ({}): {}", specPath, msg));
    }

    OpenAPI openAPI = result.getOpenAPI();
    if (openAPI == null || openAPI.getPaths() == null) {
      log.warn("No paths found in OpenAPI specification {}", specPath);
      return Set.of();
    }

    Set<EndpointConfiguration> endpoints = new HashSet<>();
    for (Map.Entry<String, PathItem> pathEntry : openAPI.getPaths().entrySet()) {
      String routePath = translatePathParameters(pathEntry.getKey());
      pathEntry
          .getValue()
          .readOperationsMap()
          .forEach(
              (method, operation) ->
                  addEndpoint(endpoints, routePath, toVertxMethod(method), operation));
    }
    return endpoints;
  }

  private static HttpMethod toVertxMethod(PathItem.HttpMethod method) {
    return HttpMethod.valueOf(method.name());
  }

  private void addEndpoint(
      Set<EndpointConfiguration> endpoints,
      String routePath,
      HttpMethod method,
      Operation operation) {
    if (operation == null) {
      return;
    }
    ApiResponse response = pickResponse(operation.getResponses());
    Optional<Integer> status = pickStatus(operation.getResponses());
    String body = renderResponseBody(response);
    endpoints.add(
        new EndpointConfiguration(method, routePath, Collections.emptyList(), status, body));
  }

  private ApiResponse pickResponse(ApiResponses responses) {
    if (responses == null || responses.isEmpty()) {
      return null;
    }
    return responses.entrySet().stream()
        .filter(e -> isSuccessStatus(e.getKey()))
        .min(Comparator.comparingInt(e -> statusOrMax(e.getKey())))
        .map(Map.Entry::getValue)
        .orElseGet(() -> responses.values().iterator().next());
  }

  private Optional<Integer> pickStatus(ApiResponses responses) {
    if (responses == null || responses.isEmpty()) {
      return Optional.empty();
    }
    return responses.keySet().stream()
        .filter(this::isSuccessStatus)
        .map(this::parseStatus)
        .flatMap(Optional::stream)
        .min(Integer::compareTo);
  }

  private boolean isSuccessStatus(String key) {
    return key != null && key.length() == 3 && key.startsWith("2");
  }

  private Optional<Integer> parseStatus(String key) {
    try {
      return Optional.of(Integer.parseInt(key));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }

  private int statusOrMax(String key) {
    return parseStatus(key).orElse(Integer.MAX_VALUE);
  }

  private String renderResponseBody(ApiResponse response) {
    Schema<?> schema = extractJsonSchema(response);
    if (schema == null) {
      return "{}";
    }
    JsonNode node = schemaSampleGenerator.generate(schema);
    return node.toPrettyString();
  }

  private Schema<?> extractJsonSchema(ApiResponse response) {
    if (response == null) {
      return null;
    }
    MediaType mediaType = findJsonMediaType(response.getContent());
    return mediaType != null ? mediaType.getSchema() : null;
  }

  private MediaType findJsonMediaType(Content content) {
    if (content == null || content.isEmpty()) {
      return null;
    }
    MediaType exact = content.get(JSON_MEDIA_TYPE);
    if (exact != null) {
      return exact;
    }
    return content.entrySet().stream()
        .filter(e -> e.getKey() != null && e.getKey().contains("json"))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElse(null);
  }

  static String translatePathParameters(String openApiPath) {
    if (openApiPath == null) {
      return null;
    }
    return PATH_PARAM.matcher(openApiPath).replaceAll(":$1");
  }
}
