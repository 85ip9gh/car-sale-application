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

# --chown on the COPY itself, not a RUN chown afterwards. A separate `RUN chown
# -R` rewrites every file it touches into a new layer, so the jar was stored in
# the image twice: two 54.1 MB layers for one 54.1 MB artifact.
COPY --from=build --chown=carsale:carsale /build/target/*.jar /app/app.jar

USER carsale
EXPOSE 8080

# Declared here as well as in compose.yaml so the image is self-describing
# wherever it runs, and so a scanner reading only the image can see it. curl is
# installed above for exactly this.
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=10 \
    CMD curl -fsS http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"'

# Heap sizing is deliberately NOT set here. It has to agree with the container's
# mem_limit, and when the two live in different files they drift: this ran at
# MaxRAMPercentage=70 against a 768m cap, which is a 564 MB heap on top of a
# ~200 MB non-heap floor, close enough to the ceiling to be OOMKilled if the
# heap ever filled. Both numbers now sit together in compose.yaml.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
