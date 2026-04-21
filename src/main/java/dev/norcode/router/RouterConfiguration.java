package dev.norcode.router;

import dev.norcode.configuration.EndpointConfiguration;

import java.util.Set;

public interface RouterConfiguration {
  void configure(Set<EndpointConfiguration> endpointConfiguration);
}
