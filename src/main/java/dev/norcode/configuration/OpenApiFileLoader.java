package dev.norcode.configuration;

import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@Identifier("openapi")
public class OpenApiFileLoader implements ConfigurationLoader {

  private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".yaml", ".yml", ".json");

  private final AppConfiguration appConfiguration;

  public OpenApiFileLoader(AppConfiguration appConfiguration) {
    this.appConfiguration = appConfiguration;
  }

  @Override
  public Set<Path> get() {
    String folder = appConfiguration.openapiFolderPath();
    log.info("Loading OpenAPI specifications from {}", folder);

    Path folderPath = Paths.get(folder);
    if (!Files.exists(folderPath)) {
      log.info("OpenAPI folder {} does not exist; skipping OpenAPI import", folder);
      return Set.of();
    }

    try (Stream<Path> stream = Files.list(folderPath)) {
      return stream
          .filter(path -> !Files.isDirectory(path))
          .filter(OpenApiFileLoader::hasSupportedExtension)
          .map(Path::toAbsolutePath)
          .collect(Collectors.toSet());
    } catch (IOException e) {
      log.error("Failed to load OpenAPI specifications", e);
      throw new LoaderException(e);
    }
  }

  private static boolean hasSupportedExtension(Path path) {
    String name = path.getFileName().toString().toLowerCase();
    return SUPPORTED_EXTENSIONS.stream().anyMatch(name::endsWith);
  }
}
