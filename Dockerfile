FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

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
