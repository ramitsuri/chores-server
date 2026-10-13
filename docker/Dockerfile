# syntax=docker/dockerfile:1
# Local/test image for the Chores server. Built by docker/compose.yaml (build context = repo root).

# ---- Build the fat jar ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /src

# Gradle wrapper + build scripts first so dependency resolution is cached across source changes
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY src ./src

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon shadowJar -x test \
    && cp build/libs/*-all.jar /app.jar

# ---- Run ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app.jar /app/app.jar

ENV PORT=8081
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
