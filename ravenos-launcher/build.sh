#!/usr/bin/env bash
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/.." && pwd)"
UPSTREAM="$ROOT/faeryware/house/iappyxOS-Launcher"

printf 'RavenOS Launcher: initialize pinned iappyx chassis\n'
git -C "$ROOT" submodule update --init --recursive faeryware/house/iappyxOS-Launcher

printf 'RavenOS Launcher: apply Faeryware HOUSE + RavenOS overlays\n'
python3 "$HERE/apply-ravenos-overlay.py"

printf 'RavenOS Launcher: build Android debug APK\n'
(
  cd "$UPSTREAM/src/launcher"
  chmod +x ./gradlew
  ./gradlew :app:assembleDebug --stacktrace
)

APK="$UPSTREAM/src/launcher/app/build/outputs/apk/debug/app-debug.apk"
test -s "$APK"
cp "$APK" "$ROOT/RavenOS-Launcher-debug.apk"
printf 'RavenOS Launcher APK: %s\n' "$ROOT/RavenOS-Launcher-debug.apk"
