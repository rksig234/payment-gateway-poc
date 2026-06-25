# Single parameterized multi-stage build for any service module in the reactor.
#
#   docker build --build-arg MODULE=gateway-routing-service --build-arg PORT=8090 -t gateway .
#
# MODULE must equal the module directory name (== its Maven artifactId).
ARG MODULE
ARG PORT=8080

# ---- build stage: build the target module + its dependencies (e.g. common) ----
FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /workspace
COPY . .
RUN mvn -q -B -pl ${MODULE} -am -DskipTests package

# ---- runtime stage: slim JRE with just the service jar ----
FROM eclipse-temurin:21-jre
ARG MODULE
ARG PORT
ENV PORT=${PORT}
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/${MODULE}/target/${MODULE}-0.1.0-SNAPSHOT.jar /app/app.jar
EXPOSE ${PORT}
# Readiness/health probe against Spring Boot Actuator.
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=10 \
  CMD curl -fs "http://localhost:${PORT}/actuator/health" | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
