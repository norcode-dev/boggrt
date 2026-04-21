package dev.norcode.configuration;

import io.vertx.core.http.HttpMethod;

public record EndpointConfiguration(HttpMethod method, String path, String response) {}
