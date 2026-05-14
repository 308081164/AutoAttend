#!/usr/bin/env sh
# 下载 Noto Sans CJK 简体到构建目录（不污染 src），供 Maven 打进 classpath:fonts/
# 用法: scripts/fetch-noto-cjk-font.sh <输出目录>
set -eu
URL="https://raw.githubusercontent.com/notofonts/noto-cjk/main/Sans/OTF/SimplifiedChinese/NotoSansCJKsc-Regular.otf"
OUT_DIR="${1:-}"
if [ -z "$OUT_DIR" ]; then
  echo "usage: $0 <output-directory>" >&2
  exit 1
fi
DEST="$OUT_DIR/NotoSansCJKsc-Regular.otf"
MIN_BYTES=1000000

mkdir -p "$OUT_DIR"

file_size() {
  if [ -f "$1" ]; then
    wc -c < "$1" | tr -d ' '
  else
    echo 0
  fi
}

sz="$(file_size "$DEST")"
if [ "$sz" -ge "$MIN_BYTES" ] 2>/dev/null; then
  echo "Noto CJK font already present ($sz bytes): $DEST"
  exit 0
fi

tmp="$DEST.part.$$"
rm -f "$tmp"
if command -v curl >/dev/null 2>&1; then
  curl -fsSL --retry 4 --retry-delay 2 -o "$tmp" "$URL"
elif command -v wget >/dev/null 2>&1; then
  wget -q --tries=4 --waitretry=2 -O "$tmp" "$URL"
else
  echo "需要 curl 或 wget 以下载中文字体" >&2
  exit 1
fi

sz2="$(file_size "$tmp")"
if [ "$sz2" -lt "$MIN_BYTES" ]; then
  echo "下载的字体文件过小 ($sz2 bytes)，可能失败" >&2
  rm -f "$tmp"
  exit 1
fi

mv -f "$tmp" "$DEST"
echo "已下载 Noto CJK 简体字体: $DEST ($sz2 bytes)"
