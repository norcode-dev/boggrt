package dev.norcode.configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractFileLoader implements ConfigurationLoader {

  protected final AppConfiguration appConfiguration;

  protected AbstractFileLoader(AppConfiguration appConfiguration) {
    this.appConfiguration = appConfiguration;
  }

  /** Folder to scan for configuration files. */
  protected abstract String folder();

  /** Description of what is being loaded, used for logging. */
  protected abstract String description();

  /** Predicate selecting which files to include. */
  protected abstract Predicate<Path> fileFilter();

  @Override
  public Set<Path> get() {
    String folder = folder();
    log.info("Loading {} from {}", description(), folder);

    Path folderPath = Paths.get(folder);
    if (!Files.exists(folderPath)) {
      log.info("Configuration folder {} does not exist; skipping import", folder);
      return Set.of();
    }

    try (Stream<Path> stream = Files.list(folderPath)) {
      return stream
          .filter(path -> !Files.isDirectory(path))
          .filter(fileFilter())
          .map(Path::toAbsolutePath)
          .collect(Collectors.toSet());
    } catch (IOException e) {
      log.error("Failed to load {}", description(), e);
      throw new LoaderException(e);
    }
  }
}
