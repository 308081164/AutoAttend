FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# CI 使用 Maven Central（阿里云镜像在 GitHub Actions 上易遇 302 导致 go-offline 失败）
COPY pom.xml .
RUN mvn -q -B -Dmaven.test.skip=true dependency:resolve

COPY . .
RUN mvn -q -B -Dmaven.test.skip=true package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENV DB_HOST=mysql \
    DB_PORT=3306 \
    DB_NAME=autoattend \
    DB_USER=autoattend \
    DB_PASSWORD=autoattend_pwd \
    ADMIN_USERNAME=admin \
    ADMIN_PASSWORD=admin123

EXPOSE 8848
ENTRYPOINT ["java","-jar","/app/app.jar","--server.port=8848"]

