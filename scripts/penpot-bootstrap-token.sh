#!/usr/bin/env bash
# 在不打开 Penpot 网页的前提下，通过 RPC 自动获取 Personal Access Token（官方镜像，无改源码）。
# Penpot 2.12+ 使用 /api/main/methods/<method>；旧版为 /api/rpc/command/<method>。本脚本自动尝试两种路径。
# 依赖：curl、python3（解析 JSON）；可选 jq。
#
# 用法：
#   export PENPOT_BASE_URL=http://127.0.0.1:9001   # 或 http://penpot-frontend:8080（容器内）
#   export PENPOT_EMAIL=service@example.com
#   export PENPOT_PASSWORD='your-password'
#   ./scripts/penpot-bootstrap-token.sh
#
# 成功时 stdout 仅打印一行 token 字符串，便于写入 CI Secret 或 .env（勿提交 Git）。

set -euo pipefail

PENPOT_BASE_URL="${PENPOT_BASE_URL:-http://127.0.0.1:9001}"
EMAIL="${PENPOT_EMAIL:-}"
PASS="${PENPOT_PASSWORD:-}"
TOKEN_NAME="${PENPOT_TOKEN_NAME:-autoattend-bootstrap}"
CLIENT_HEADER="${PENPOT_CLIENT_HEADER:-penpot-backend}"

if [[ -z "$EMAIL" || -z "$PASS" ]]; then
  echo "请设置 PENPOT_EMAIL 与 PENPOT_PASSWORD" >&2
  exit 1
fi

BASE="${PENPOT_BASE_URL%/}"
COOKIE_JAR="$(mktemp)"
trap 'rm -f "$COOKIE_JAR"' EXIT

login_json=$(python3 -c "import json,sys; print(json.dumps({'email':sys.argv[1],'password':sys.argv[2]}))" "$EMAIL" "$PASS")

LOGIN_PATHS=(
  "/api/main/methods/login-with-password"
  "/api/rpc/command/login-with-password"
)

http_code=""
for path in "${LOGIN_PATHS[@]}"; do
  http_code=$(curl -sS -o /tmp/penpot-login-body.json -w '%{http_code}' \
    -c "$COOKIE_JAR" \
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json' \
    -H "x-client: $CLIENT_HEADER" \
    -X POST "$BASE$path" \
    -d "$login_json" || true)
  if [[ "$http_code" == "200" ]]; then
    break
  fi
done

if [[ "$http_code" != "200" ]]; then
  echo "login-with-password 失败 HTTP $http_code（已尝试新/旧 RPC 路径）" >&2
  cat /tmp/penpot-login-body.json >&2 || true
  exit 1
fi

create_body=$(python3 -c "import json; print(json.dumps({'name': '$TOKEN_NAME'}))")

TOKEN_PATHS=(
  "/api/main/methods/create-access-token"
  "/api/rpc/command/create-access-token"
)

http_code=""
for path in "${TOKEN_PATHS[@]}"; do
  http_code=$(curl -sS -o /tmp/penpot-token-body.json -w '%{http_code}' \
    -b "$COOKIE_JAR" \
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json' \
    -H "x-client: $CLIENT_HEADER" \
    -X POST "$BASE$path" \
    -d "$create_body" || true)
  if [[ "$http_code" == "200" ]]; then
    break
  fi
done

if [[ "$http_code" != "200" ]]; then
  echo "create-access-token 失败 HTTP $http_code（已尝试新/旧 RPC 路径）" >&2
  cat /tmp/penpot-token-body.json >&2 || true
  exit 1
fi

python3 <<'PY'
import json, sys
with open("/tmp/penpot-token-body.json", "r", encoding="utf-8") as f:
    data = json.load(f)
token = data.get("token") or data.get("Token")
if not token:
    print("响应中未找到 token 字段:", data, file=sys.stderr)
    sys.exit(1)
print(token)
PY
