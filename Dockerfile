FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN for attempt in 1 2 3 4 5; do \
      mvn -B -U -Dmaven.wagon.http.retryHandler.count=5 -DskipTests package > /tmp/maven-build.log 2>&1; status=$?; \
      cat /tmp/maven-build.log; \
      if [ "$status" -eq 0 ]; then exit 0; fi; \
      if ! grep -Eqi '429|Too Many Requests' /tmp/maven-build.log || [ "$attempt" -eq 5 ]; then exit "$status"; fi; \
      delay=$((attempt * 10)); \
      echo "Maven Central rate limited the build; retrying in ${delay}s (attempt ${attempt}/5)..." >&2; \
      sleep "$delay"; \
    done; \
    exit 1

FROM eclipse-temurin:17-jre

WORKDIR /app
RUN groupadd --system --gid 10001 chatapp \
    && useradd --system --uid 10001 --gid chatapp --home-dir /app chatapp \
    && mkdir -p /app/config \
    && chown -R chatapp:chatapp /app

COPY --from=build --chown=chatapp:chatapp /workspace/target/*.jar /app/app.jar

USER chatapp
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
