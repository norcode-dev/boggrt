package dev.norcode.configuration;

import io.smallrye.common.annotation.Identifier;
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
@Identifier("json")
public class JsonFileLoader implements ConfigurationLoader {

  private final AppConfiguration appConfiguration;

  public JsonFileLoader(AppConfiguration appConfiguration) {
    this.appConfiguration = appConfiguration;
  }

  @Override
  public Set<Path> get() {
    String folder = appConfiguration.endpointsFolderPath();
    log.info("Loading endpoint configurations from {}", folder);

    Path folderPath = Paths.get(folder);
    if (!Files.exists(folderPath)) {
      log.info("Configuration folder {} does not exist; skipping import", folder);
      return Set.of();
    }

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
