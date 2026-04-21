package dev.norcode.configuration;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class FileLoader implements ConfigurationLoader {

  private final AppConfiguration appConfiguration;

  public FileLoader(AppConfiguration appConfiguration) {
    this.appConfiguration = appConfiguration;
  }

  @Override
  public Set<Path> get() {
    log.info("Loading endpoint configurations from {}", appConfiguration.endpointsFolderPath());

    try (Stream<Path> stream = Files.list(Paths.get(appConfiguration.endpointsFolderPath()))) {
      return stream
          .filter(path -> !Files.isDirectory(path))
          .filter(path -> path.getFileName().toString().endsWith(".json"))
          .map(Path::toAbsolutePath)
          .collect(Collectors.toSet());
    } catch (IOException e) {
      log.error("Failed to load endpoint configuration", e);
      throw new LoaderException(e);
    }
  }
}
