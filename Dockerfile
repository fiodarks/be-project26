# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY gradlew build.gradle settings.gradle ./
COPY gradle/ gradle/

RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN ./gradlew --no-daemon -v

COPY src/ src/

# Build the runnable Spring Boot jar (OpenAPI generation is wired into compileJava)
RUN ./gradlew --no-daemon clean bootJar -x test

FROM eclipse-temurin:25-jre
WORKDIR /app

ENV JAVA_OPTS=""
ENV ARCHIVE_STORAGE_BASE_DIR="/data/uploads"

COPY --from=build /workspace/build/libs/*.jar /app/app.jar

EXPOSE 8080
VOLUME ["/data"]

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
