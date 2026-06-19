package dev.norcode.parser;

import io.quarkus.runtime.annotations.RegisterForReflection;
import net.datafaker.providers.base.Bool;
import net.datafaker.providers.base.Internet;
import net.datafaker.providers.base.Lorem;
import net.datafaker.providers.base.Name;
import net.datafaker.providers.base.Number;
import net.datafaker.providers.base.Text;

/**
 * Datafaker resolves values such as {@code firstName()} through its YAML expression engine, which
 * reflectively invokes provider methods (e.g. {@code Name.maleFirstName()}). GraalVM native image
 * cannot see those reflective calls statically, so the provider classes must be registered for
 * reflection explicitly. If a {@code MissingReflectionRegistrationError} appears for another
 * provider at runtime, add that class here as well.
 */
@RegisterForReflection(
    targets = {
      Name.class,
      Internet.class,
      Lorem.class,
      Number.class,
      Bool.class,
      Text.class,
    })
public class DatafakerReflectionConfig {}
