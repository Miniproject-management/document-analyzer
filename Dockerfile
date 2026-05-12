# CI(GitHub Actions) → ECR → 배포만 사용. 로컬 Gradle/gradlew 빌드 불필요.
# 빌드는 이 이미지 안에서만 수행된다.
FROM gradle:8.12-jdk17 AS builder

WORKDIR /app

COPY build.gradle settings.gradle ./
COPY src src

RUN gradle clean bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
