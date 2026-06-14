package dev.norcode.configuration;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "boggrt")
public interface AppConfiguration {
    String endpointsFolderPath();

    String openapiFolderPath();
}
