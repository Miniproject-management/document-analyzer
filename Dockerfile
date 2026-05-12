# Build stage
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# 현재 프로젝트 전체 복사 (dto/entity/repository 포함)
COPY . .

# Gradle 빌드
RUN gradle clean build -x test

# Run stage
FROM eclipse-temurin:17-jdk

WORKDIR /app

# jar 복사
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]