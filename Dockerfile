# --- Build stage ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

# --- Runtime stage ---
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd -r srms && useradd -r -g srms srms
COPY --from=build --chown=srms:srms /app/target/*.jar app.jar
USER srms
EXPOSE 8080
# Respect the container's cgroup memory limit instead of sizing the heap off host RAM
# (the JVM's own default heuristic sees the whole host, not the --memory limit).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
HEALTHCHECK --interval=5s --timeout=5s --retries=20 --start-period=20s \
  CMD bash -c '</dev/tcp/localhost/${PORT:-8080}' || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
