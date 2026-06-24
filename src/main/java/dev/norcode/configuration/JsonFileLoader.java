package dev.norcode.configuration;

import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.file.Path;
import java.util.function.Predicate;

@ApplicationScoped
@Identifier("json")
public class JsonFileLoader extends AbstractFileLoader {

  public JsonFileLoader(AppConfiguration appConfiguration) {
    super(appConfiguration);
  }

  @Override
  protected String folder() {
    return appConfiguration.endpointsFolderPath();
  }

  @Override
  protected String description() {
    return "endpoint configurations";
  }

  @Override
  protected Predicate<Path> fileFilter() {
    return path -> path.getFileName().toString().endsWith(".json");
  }
}
