FROM node:22-alpine AS frontend-build
WORKDIR /frontend

COPY frontend/package*.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build

FROM maven:3.9.9-eclipse-temurin-17 AS backend-build
WORKDIR /app

COPY pom.xml ./
COPY src ./src

RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=backend-build /app/target/scms.jar /app/scms.jar
COPY --from=frontend-build /frontend/dist /app/frontend-dist

ENV PORT=10000
ENV FRONTEND_DIR=/app/frontend-dist
EXPOSE 10000

CMD ["java", "-cp", "/app/scms.jar", "com.scms.main.DeployServer"]
