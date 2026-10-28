FROM gradle:8.8-jdk21 AS build
WORKDIR /app

COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

RUN addgroup -S delivery && adduser -S -G delivery delivery
USER delivery

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
