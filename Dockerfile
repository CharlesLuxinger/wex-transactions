FROM eclipse-temurin:25-jdk-jammy AS builder

WORKDIR /workspace

COPY gradlew ./gradlew
COPY gradle ./gradle
COPY settings.gradle.kts ./settings.gradle.kts
COPY build.gradle.kts ./build.gradle.kts

RUN chmod +x ./gradlew

COPY src ./src

RUN ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:25-jre-jammy

WORKDIR /app

COPY --from=builder /workspace/build/libs/*.jar /app/app.jar

EXPOSE 8080

USER 1000:1000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
