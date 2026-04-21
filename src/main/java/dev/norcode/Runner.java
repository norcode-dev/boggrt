package dev.norcode;

import dev.norcode.configuration.Configuration;
import dev.norcode.configuration.ConfigurationLoader;
import io.quarkus.runtime.StartupEvent;
import io.vertx.ext.web.Router;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Runner {

    @Inject
    ConfigurationLoader configurationLoader;

    void installRoute(@Observes StartupEvent startupEvent, Router router) {

        log.info("Installing routes");
        Configuration configuration = configurationLoader.get();
        router.route()
                .path("/hello")
                .method(configuration.method())
                .handler(
                        routingContext -> routingContext.response()
                                .putHeader("content-type", "application/json")
                                .end("""
                {
                "message": "Hello from Quarkus REST"
                }
    """));
    }
}
