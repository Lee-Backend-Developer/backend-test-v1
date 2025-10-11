# syntax=docker/dockerfile:1

FROM eclipse-temurin:22-jdk AS builder

WORKDIR /workspace

RUN apt-get update \
    && apt-get install -y --no-install-recommends unzip \
    && rm -rf /var/lib/apt/lists/*

COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY modules modules

RUN chmod +x gradlew
RUN ./gradlew :modules:bootstrap:api-payment-gateway:bootJar --no-daemon

FROM eclipse-temurin:21-jre

ENV APP_HOME=/app \
    SPRING_PROFILES_ACTIVE=default

WORKDIR ${APP_HOME}

COPY --from=builder /workspace/modules/bootstrap/api-payment-gateway/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]
