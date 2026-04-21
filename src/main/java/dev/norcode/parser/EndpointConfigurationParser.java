package dev.norcode.parser;

import dev.norcode.configuration.EndpointConfiguration;
import java.util.Set;

public interface EndpointConfigurationParser<T> {
  Set<EndpointConfiguration> parse(Set<T> params);
}
