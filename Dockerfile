# syntax=docker/dockerfile:1

FROM eclipse-temurin:22-jdk AS builder
WORKDIR /workspace

# 캐시 최적화: 래퍼/스크립트 먼저
COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle.kts build.gradle.kts ./
COPY modules modules

RUN chmod +x gradlew

# 자세한 로그로 실패 지점 노출
RUN ./gradlew :modules:bootstrap:api-payment-gateway:bootJar \
    --no-daemon --stacktrace --info --warning-mode all

# ─────────────────────────────────────────────────────────────

FROM eclipse-temurin:22-jre
ENV APP_HOME=/app \
    SPRING_PROFILES_ACTIVE=default \
    PORT=8080 \
    JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"
WORKDIR ${APP_HOME}

# ✳️ 실행 가능한 bootJar만 복사 (파일명 패턴은 필요시 조정)
COPY --from=builder /workspace/modules/bootstrap/api-payment-gateway/build/libs/*.jar /app/app.jar

EXPOSE 8080
# Cloud Run의 PORT 반영(0.0.0.0 바인딩)
ENTRYPOINT ["java","-Dserver.port=${PORT}","-jar","/app/app.jar"]