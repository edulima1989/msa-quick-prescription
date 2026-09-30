FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace

COPY gradle gradle
COPY gradlew settings.gradle build.gradle ./
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

COPY src src
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --uid 1001 spring
COPY --from=builder /workspace/build/libs/*.jar app.jar
USER spring

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]