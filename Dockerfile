# syntax=docker/dockerfile:1

# ==================================================
# Stage 1: Build the Spring Boot application
# ==================================================
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy Maven configuration first for better layer caching
COPY pom.xml ./

# Download dependencies before copying source code
RUN mvn -B dependency:go-offline

# Copy application source
COPY src ./src

# Build the executable Spring Boot JAR
RUN mvn -B clean package -DskipTests


# ==================================================
# Stage 2: Production runtime
# ==================================================
FROM eclipse-temurin:17-jre-jammy AS runtime

WORKDIR /app

# Create a non-root application user
RUN groupadd --system srms \
    && useradd --system --gid srms --home-dir /app --shell /usr/sbin/nologin srms

# Copy the executable JAR from the build stage
COPY --from=build --chown=srms:srms /app/target/*.jar ./app.jar

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
ENV JAVA_OPTS=""

USER srms

EXPOSE 8080

# Checks whether the application port is accepting connections
HEALTHCHECK \
    --interval=30s \
    --timeout=5s \
    --start-period=60s \
    --retries=5 \
    CMD bash -c '</dev/tcp/127.0.0.1/${PORT:-8080}' || exit 1

# Render supplies PORT at runtime.
# Locally, the application defaults to port 8080.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]