package dev.norcode.evaluator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class RequestParser {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public static Optional<JsonNode> parseRequestBody(String requestBody) {
    if (requestBody == null || requestBody.isBlank()) {
      return Optional.empty();
    }

    try {
      return OBJECT_MAPPER.readTree(requestBody).asOptional();
    } catch (Exception e) {
      log.debug("Could not parse request body as JSON", e);
      return Optional.empty();
    }
  }
}
