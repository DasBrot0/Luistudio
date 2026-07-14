FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw mvnw
COPY mvnw.cmd mvnw.cmd

RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -q clean verify

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Ajustes para instancias pequenas (ej. 500 MB en Render)
ENV JAVA_TOOL_OPTIONS="-XX:+UseSerialGC -XX:InitialRAMPercentage=25 -XX:MaxRAMPercentage=70 -XX:MaxMetaspaceSize=128m -Djava.security.egd=file:/dev/./urandom"

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
