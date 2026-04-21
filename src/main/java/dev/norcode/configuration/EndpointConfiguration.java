package dev.norcode.configuration;

import io.vertx.core.http.HttpMethod;
import java.util.List;
import java.util.Optional;

public record EndpointConfiguration(
    HttpMethod method,
    String path,
    List<Condition> conditions,
    Optional<Integer> responseStatus,
    String response) {

    private static final int DEFAULT_RESPONSE_STATUS = 200;

    public boolean hasValidConditions() {
        return conditions != null && !conditions.isEmpty();
    }

    public int resolvedResponseStatus() {
        return responseStatus.orElse(DEFAULT_RESPONSE_STATUS);
    }
}
