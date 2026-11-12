#!/usr/bin/env bash
# Chép giao diện web + locales NGUYÊN từ ios-project (nguồn NFsdk-UI nằm trên máy macOS — DESIGN #8e).
# Không sửa tay nfsdk/src/main/assets/nfsdk/** — sửa ở NFsdk-UI rồi đồng bộ lại.
#   tools/sync_web.sh [thư mục nguồn Resources]   (mặc định ../ios-project/NFsdk/Resources)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="${1:-$ROOT/../ios-project/NFsdk/Resources}"
DST="$ROOT/nfsdk/src/main/assets/nfsdk"
RES="$ROOT/nfsdk/src/main/res/drawable-nodpi"

for d in web locales; do
  [ -d "$SRC/$d" ] || { echo "Không thấy $SRC/$d" >&2; exit 1; }
done

rm -rf "$DST/web" "$DST/locales"
mkdir -p "$DST" "$RES"
cp -R "$SRC/web" "$DST/web"
cp -R "$SRC/locales" "$DST/locales"
find "$DST" -name .DS_Store -delete

# Ảnh floating / Joy (tên resource Android: chữ thường + gạch dưới)
[ -f "$SRC/floating/compact_logo.png" ]  && cp "$SRC/floating/compact_logo.png"  "$RES/nfsdk_floating_compact.png"
[ -f "$SRC/floating/expanded_logo.png" ] && cp "$SRC/floating/expanded_logo.png" "$RES/nfsdk_floating_expanded.png"
[ -f "$SRC/joy/joy_logo.png" ]           && cp "$SRC/joy/joy_logo.png"           "$RES/nfsdk_joy_logo.png"

echo "Đã đồng bộ từ $SRC"
( cd "$DST" && find . -type f | sort )
