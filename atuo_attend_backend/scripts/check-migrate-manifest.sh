#!/usr/bin/env bash
# 确保 resources/db 下所有「增量迁移」SQL 均已列入 migrate_manifest.txt，避免推送 main 后容器未执行新 DDL。
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DB_DIR="$ROOT/src/main/resources/db"
MANIFEST="$DB_DIR/migrate_manifest.txt"
cd "$DB_DIR"

missing=0
# 约定：文件名含 migration 的增量脚本必须出现在清单中（与仓库既有命名一致）
for f in schema_*migration*.sql; do
  [ -f "$f" ] || continue
  if ! grep -qxF "$f" "$MANIFEST" 2>/dev/null; then
    echo "ERROR: $f 未列入 migrate_manifest.txt，推送到 main 后 Docker 启动将不会执行该迁移。" >&2
    missing=1
  fi
done

if [ "$missing" -ne 0 ]; then
  echo >&2
  echo "请将缺失文件名按依赖顺序追加到: $MANIFEST" >&2
  exit 1
fi

echo "OK: 所有 schema_*migration*.sql 均已列入 migrate_manifest.txt"
