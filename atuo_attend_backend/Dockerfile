FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# CI 使用 Maven Central（阿里云镜像在 GitHub Actions 上易遇 302 导致 go-offline 失败）
COPY pom.xml .
RUN mvn -q -B -Dmaven.test.skip=true dependency:resolve

COPY . .
RUN mvn -q -B -Dmaven.test.skip=true package

FROM eclipse-temurin:17-jre
WORKDIR /app
# git：Diff 本地兜底；default-mysql-client：供 entrypoint 对 MySQL 执行增量迁移（无需在宿主机手工 mysql 客户端跑脚本）
# CI（如 GitHub Actions）偶发连不上 archive.ubuntu.com：为 apt 加长超时与重试，并对 update/install 做有限次重试
RUN set -eux; \
    export DEBIAN_FRONTEND=noninteractive; \
    printf '%s\n' \
      'Acquire::Retries "6";' \
      'Acquire::http::Timeout "120";' \
      'Acquire::https::Timeout "120";' \
      > /etc/apt/apt.conf.d/80-ci-retries; \
    n=0; \
    until apt-get update; do \
      n=$((n + 1)); \
      if [ "$n" -gt 5 ]; then echo "apt-get update failed after 5 retries"; exit 1; fi; \
      echo "apt-get update retry $n/5 in 15s..."; \
      sleep 15; \
    done; \
    n=0; \
    until apt-get install -y --no-install-recommends git ca-certificates default-mysql-client curl; do \
      n=$((n + 1)); \
      if [ "$n" -gt 5 ]; then echo "apt-get install failed after 5 retries"; exit 1; fi; \
      echo "apt-get install retry $n/5 in 20s..."; \
      sleep 20; \
    done; \
    rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar
# 增量 DDL 与清单：容器每次启动时由 docker-entrypoint.sh 按 migrate_manifest.txt 顺序自动执行；CI/CD 推送后重建 backend 即完成库表更新
COPY --from=build /app/src/main/resources/db /app/db
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

ENV DB_HOST=mysql \
    DB_PORT=3306 \
    DB_NAME=autoattend \
    DB_USER=autoattend \
    DB_PASSWORD=autoattend_pwd \
    ADMIN_USERNAME=admin \
    ADMIN_PASSWORD=admin123

EXPOSE 8848
ENTRYPOINT ["/app/docker-entrypoint.sh"]

