# ---- build stage: Gradle 로 실행 가능한 bootJar 생성 ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 의존성 캐시 레이어: 빌드 스크립트만 먼저 복사해 받아두면 소스만 바뀔 때 재다운로드를 피한다
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies || true

COPY src ./src
# 테스트는 Testcontainers(도커 필요)라 이미지 빌드 시엔 제외하고 bootJar 만 만든다
RUN ./gradlew --no-daemon clean bootJar -x test

# ---- run stage: JRE 만 담아 경량 실행 ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
