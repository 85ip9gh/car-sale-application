# Build the Spring Boot API. Build context is the repository root.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Dependencies first so source edits do not invalidate the dependency layer.
COPY java_backend_CRUD_1/pom.xml ./pom.xml
RUN mvn -B -q dependency:go-offline

COPY java_backend_CRUD_1/src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# curl backs the compose health check; the base image does not ship it.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

# Unprivileged runtime user.
RUN groupadd --system --gid 10001 carsale \
 && useradd --system --uid 10001 --gid carsale --no-create-home carsale

COPY --from=build /build/target/*.jar /app/app.jar
RUN chown -R carsale:carsale /app

USER carsale
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "/app/app.jar"]
