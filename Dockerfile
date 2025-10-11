# syntax=docker/dockerfile:1

# ▶ 빌드/런타임 버전 통일 (Java 21)
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

# Gradle 캐시 최적화: 래퍼와 스크립트 먼저 복사
COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle.kts build.gradle.kts ./

# 멀티모듈 소스
COPY modules modules

# 권한 + 의존성 프리패치 (옵션)
RUN chmod +x gradlew

# 필요한 모듈만 bootJar 빌드
RUN ./gradlew :modules:bootstrap:api-payment-gateway:bootJar --no-daemon

# ─────────────────────────────────────────────────────────────

FROM eclipse-temurin:21-jre

ENV APP_HOME=/app \
    SPRING_PROFILES_ACTIVE=default \
    PORT=8080 \
    JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"

WORKDIR ${APP_HOME}

# ✳️ bootJar 산출물만 확실히 복사 (plain 제외)
#   산출물이 1개라면 *.jar로 충분하지만, plain이 섞이면 문제가 생겨서 아래처럼 명시하는 걸 권장합니다.
#   실제 파일명을 확인해 app.jar로 복사하세요.
#   예) api-payment-gateway-0.0.1-SNAPSHOT.jar
COPY --from=builder /workspace/modules/bootstrap/api-payment-gateway/build/libs/*-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

# ✅ Cloud Run PORT 반영 (0.0.0.0 바인드)
ENTRYPOINT ["java","-Dserver.port=${PORT}","-jar","/app/app.jar"]