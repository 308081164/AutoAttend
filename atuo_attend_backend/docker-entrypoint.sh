#!/bin/sh
# 启动 Spring Boot 前：等待 MySQL 就绪并按 migrate_manifest.txt 执行迁移（幂可重复执行）。
# 生产/CI：勿在服务器上单独执行 mysql 迁移命令；推送代码后由部署流水线重建 backend 容器，本脚本即自动跑完清单。
# 清单与 SQL 来源：镜像内 /app/db（构建时 COPY）或 Compose 挂载的宿主 ./atuo_attend_backend/.../db（与仓库一致）。
set -e

if [ "${SKIP_DB_MIGRATE:-}" = "1" ]; then
  echo "[entrypoint] SKIP_DB_MIGRATE=1，跳过数据库迁移"
  exec java -jar /app/app.jar --server.port=8848
fi

DB_HOST="${DB_HOST:-mysql}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-autoattend}"
DB_USER="${DB_USER:-autoattend}"
DB_PASSWORD="${DB_PASSWORD:-}"

echo "[entrypoint] 等待 MySQL: ${DB_HOST}:${DB_PORT} ..."
i=0
while [ "$i" -lt 90 ]; do
  if mysqladmin ping -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" --silent 2>/dev/null; then
    echo "[entrypoint] MySQL 已就绪"
    break
  fi
  i=$((i + 1))
  sleep 2
done

if ! mysqladmin ping -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" --silent 2>/dev/null; then
  echo "[entrypoint] 错误: 无法在约 3 分钟内连接 MySQL，请检查 DB_* 与网络"
  exit 1
fi

if [ -f /app/db/migrate_manifest.txt ]; then
  echo "[entrypoint] 按清单执行数据库迁移..."
  while IFS= read -r line || [ -n "$line" ]; do
    case "$line" in \#*|"") continue ;; esac
    f="/app/db/$line"
    if [ -f "$f" ]; then
      echo "[entrypoint] -> $line"
      mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < "$f" \
        || echo "[entrypoint] !! $line 执行有报错（若列/表已存在可忽略）"
    else
      echo "[entrypoint] !! 缺少文件: $f"
    fi
  done < /app/db/migrate_manifest.txt
  echo "[entrypoint] 迁移步骤结束，启动应用"
else
  echo "[entrypoint] 未找到 /app/db/migrate_manifest.txt，跳过迁移"
fi

exec java -jar /app/app.jar --server.port=8848
