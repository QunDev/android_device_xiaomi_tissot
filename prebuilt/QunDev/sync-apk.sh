#!/usr/bin/env bash
#
# sync-apk.sh — copy the freshly built QunDev release APK into this device tree
# prebuilt slot, so the next ROM build (m QunDev / mka bacon) picks it up.
#
# Usage:
#   ./sync-apk.sh            # copy the existing release-unsigned APK
#   ./sync-apk.sh --build    # run gradle assembleRelease first, then copy
#   QUNDEV_PROJECT=/path ./sync-apk.sh   # override the Android Studio project dir
#
set -euo pipefail

# Destination = this script's own directory (device/xiaomi/tissot/prebuilt/QunDev).
DEST_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEST="$DEST_DIR/QunDev.apk"

# Source Android Studio project (override with QUNDEV_PROJECT).
PROJECT="${QUNDEV_PROJECT:-/home/qundev/AndroidStudioProjects/QunDev}"
APK="$PROJECT/app/build/outputs/apk/release/app-release-unsigned.apk"

DO_BUILD=0
for arg in "$@"; do
  case "$arg" in
    -b|--build) DO_BUILD=1 ;;
    -h|--help)  grep '^#' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "Unknown arg: $arg (try --help)" >&2; exit 2 ;;
  esac
done

if [[ ! -d "$PROJECT" ]]; then
  echo "ERROR: project dir not found: $PROJECT (set QUNDEV_PROJECT)" >&2
  exit 1
fi

if [[ "$DO_BUILD" == 1 ]]; then
  echo ">> Building release APK (gradle assembleRelease)..."
  ( cd "$PROJECT" && ./gradlew :app:assembleRelease )
fi

if [[ ! -f "$APK" ]]; then
  echo "ERROR: APK not found: $APK" >&2
  echo "       Build it in Android Studio, or run: $0 --build" >&2
  exit 1
fi

cp -f "$APK" "$DEST"
echo ">> Copied:"
echo "     from $APK"
echo "     to   $DEST"
ls -l --time-style=+%Y-%m-%d_%H:%M "$DEST" | awk '{print "     " $0}'

# Best-effort package-name sanity check (must stay com.qundev.qundev).
AAPT="$(command -v aapt2 || true)"
if [[ -n "$AAPT" ]]; then
  PKG="$("$AAPT" dump packagename "$DEST" 2>/dev/null || true)"
  echo "     package: ${PKG:-<aapt2 failed>}"
  if [[ -n "$PKG" && "$PKG" != "com.qundev.qundev" ]]; then
    echo "WARNING: package is '$PKG', expected com.qundev.qundev" >&2
  fi
fi

cat <<'EOF'

Next:
  - Quick rebuild of just the app module:   m QunDev
  - Full ROM:                               mka bacon
  - Live test on a running device (root):
      adb root && adb remount && \
      adb push QunDev.apk /system/priv-app/QunDev/QunDev.apk && adb reboot
    (only works if the on-device build is also platform-signed; otherwise reflash)

Remember to `git add QunDev.apk && git commit` to version the new prebuilt.
EOF
