package dev.norcode.configuration;

import io.vertx.core.http.HttpMethod;
import java.util.List;

public record EndpointConfiguration(
    HttpMethod method, String path, List<Condition> conditions, String response) {

    public boolean hasValidConditions() {
        return conditions != null && !conditions.isEmpty();
    }
}
