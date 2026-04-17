FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# CI 使用 Maven Central（阿里云镜像在 GitHub Actions 上易遇 302 导致 go-offline 失败）
COPY pom.xml .
RUN mvn -q -B -Dmaven.test.skip=true dependency:resolve

COPY . .
RUN mvn -q -B -Dmaven.test.skip=true package

FROM eclipse-temurin:17-jre
WORKDIR /app
# git：Diff 本地兜底；mysql-client：供 entrypoint 对 MySQL 执行增量迁移（与 default-mysql-client 等价，包名更稳定）
# CI 偶发仅连上 security.ubuntu.com、archive 超时 → apt 仍返回 0 但主仓库索引缺失会导致「Package has no installation candidate」
# 处理：优先把官方源改为 HTTPS；update 后校验 mysql-client 元数据存在再 install
RUN set -eux; \
    export DEBIAN_FRONTEND=noninteractive; \
    printf '%s\n' \
      'Acquire::Retries "6";' \
      'Acquire::http::Timeout "120";' \
      'Acquire::https::Timeout "120";' \
      > /etc/apt/apt.conf.d/80-ci-retries; \
    find /etc/apt -type f \( -name '*.list' -o -name '*.sources' \) -print0 2>/dev/null | xargs -0 -r sed -i \
      's|http://archive.ubuntu.com|https://archive.ubuntu.com|g; s|http://security.ubuntu.com|https://security.ubuntu.com|g' || true; \
    n=0; \
    until apt-get update && apt-cache show mysql-client >/dev/null 2>&1; do \
      n=$((n + 1)); \
      if [ "$n" -gt 8 ]; then echo "apt-get update incomplete (no mysql-client in index) after 8 retries"; exit 1; fi; \
      echo "apt-get update retry $n/8 in 20s..."; \
      sleep 20; \
    done; \
    apt-get install -y --no-install-recommends git ca-certificates mysql-client curl; \
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

