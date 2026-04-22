FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/scms.jar /app/scms.jar

ENV PORT=10000
EXPOSE 10000

CMD ["java", "-cp", "/app/scms.jar", "com.scms.main.DeployServer"]
