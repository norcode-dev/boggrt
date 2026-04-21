package dev.norcode.configuration;

public class LoaderException extends RuntimeException {
  public LoaderException(Throwable cause) {
    super("Failed to load endpoint configuration", cause);
  }
}
