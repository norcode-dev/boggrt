package dev.norcode.configuration;

import io.vertx.core.http.HttpMethod;

public record Configuration (HttpMethod method) {}
