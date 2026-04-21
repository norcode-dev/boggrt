package dev.norcode.configuration;

import java.nio.file.Path;
import java.util.Set;

public interface ConfigurationLoader {
  Set<Path> get();
}
