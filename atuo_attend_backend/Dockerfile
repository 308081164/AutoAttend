FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# 为 Maven 配置国内镜像（阿里云），加速依赖下载
RUN mkdir -p /root/.m2
RUN printf '%s\n' \
  '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"' \
  '  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"' \
  '  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0' \
  '                      https://maven.apache.org/xsd/settings-1.0.0.xsd">' \
  '  <mirrors>' \
  '    <mirror>' \
  '      <id>aliyunmaven</id>' \
  '      <mirrorOf>central</mirrorOf>' \
  '      <name>Aliyun Maven</name>' \
  '      <url>https://maven.aliyun.com/repository/central</url>' \
  '    </mirror>' \
  '  </mirrors>' \
  '</settings>' \
  > /root/.m2/settings.xml

COPY . .
RUN ./mvnw -q -DskipTests package

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

