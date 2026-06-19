package dev.norcode.configuration;

import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Predicate;

@ApplicationScoped
@Identifier("openapi")
public class OpenApiFileLoader extends AbstractFileLoader {

  private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".yaml", ".yml", ".json");

  public OpenApiFileLoader(AppConfiguration appConfiguration) {
    super(appConfiguration);
  }

  @Override
  protected String folder() {
    return appConfiguration.openapiFolderPath();
  }

  @Override
  protected String description() {
    return "OpenAPI specifications";
  }

  @Override
  protected Predicate<Path> fileFilter() {
    return OpenApiFileLoader::hasSupportedExtension;
  }

  private static boolean hasSupportedExtension(Path path) {
    String name = path.getFileName().toString().toLowerCase();
    return SUPPORTED_EXTENSIONS.stream().anyMatch(name::endsWith);
  }
}
