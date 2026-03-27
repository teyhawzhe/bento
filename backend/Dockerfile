FROM gradle:8.14.3-jdk21-alpine AS builder
WORKDIR /workspace

COPY build.gradle settings.gradle ./
COPY src ./src

RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends fonts-noto-cjk \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /workspace/build/libs/*.jar app.jar

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=production

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
