FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN apk add --no-cache maven && \
    mvn clean package -DskipTests && \
    apk del maven

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar
COPY exercise-muscle-groups.yml .

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
