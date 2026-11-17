# Build stage: compiles the jar. Tests are skipped here on purpose; run `mvn test` before you build the image.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -q -Dmaven.test.skip=true package

# Run stage: just a JRE and the jar, as an unprivileged user.
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 --no-create-home app
WORKDIR /app
COPY --from=build /app/target/portfolio-api-*.jar app.jar
USER app

# "prod" refuses to start without SPRING_DATASOURCE_URL (see application-prod.properties).
# The heap follows the container's memory limit instead of the host's.
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

# The app listens on $PORT when the platform sets it, otherwise 8080.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
