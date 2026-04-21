# Boggrt

Boggrt is a mock API server that serves responses based on configurable endpoints and conditions.

## Features

- Configure multiple mock API endpoints.
- Define specific conditions for incoming requests (method, path, body fields).
- Supports JSONPath-like syntax for request validation.
- Returns configured mock responses when conditions are met, otherwise returns 404.

For detailed configuration options, see [Configuration Specification](spec.md).

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

## Build docker image 
```shell script
docker build -f src/main/docker/Dockerfile.native-micro -t quarkus/boggrt .
```

## Running the Docker image with the native executable
```shell script
docker run -d --env BOGGRT_SOURCE=/json -v ./src/main/resources:/json --rm -p 8080:8080 quarkus/boggrt
```

You can then execute your native executable with: `./target/boggrt-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.
