#!/usr/bin/env bash
set -euo pipefail

python3 ravenos-launcher/apply-ravenos-overlay.py
python3 ravenos-launcher/patch-ravenos-ergonomics.py faeryware/house/iappyxOS-Launcher

pushd faeryware/house/iappyxOS-Launcher/src/launcher >/dev/null
chmod +x ./gradlew
./gradlew :app:compileDebugKotlin --stacktrace
./gradlew :app:assembleDebug --stacktrace
popd >/dev/null

SHORT_SHA="${GITHUB_SHA::8}"
RUN_ID="${GITHUB_RUN_ID:-local}"
ATTEMPT="${GITHUB_RUN_ATTEMPT:-1}"
BUILD_KEY="run-${RUN_ID}-a${ATTEMPT}-${SHORT_SHA}"
APK_NAME="RavenOS-Launcher-${BUILD_KEY}.apk"
ARCHIVE_FOLDER="RavenOS-Launcher-${BUILD_KEY}"
TAG="ravenos-launcher-archive-${RUN_ID}-${ATTEMPT}"
SRC='faeryware/house/iappyxOS-Launcher/src/launcher/app/build/outputs/apk/debug/app-debug.apk'

test -s "$SRC"
cp "$SRC" "$APK_NAME"
SHA256="$(sha256sum "$APK_NAME" | awk '{print $1}')"
BYTES="$(stat -c '%s' "$APK_NAME")"
printf '%s  %s\n' "$SHA256" "$APK_NAME" > "${APK_NAME}.sha256"

AAPT="$(find "$ANDROID_HOME/build-tools" -type f -name aapt | sort -V | tail -1)"
test -x "$AAPT"
"$AAPT" dump badging "$APK_NAME" | tee RavenOS-Launcher-badging.txt
grep -F "package: name='com.ravenos.launcher'" RavenOS-Launcher-badging.txt
grep -F "application-label:'RavenOS Launcher'" RavenOS-Launcher-badging.txt

VERSION_NAME="$(sed -n "s/.*versionName='\([^']*\)'.*/\1/p" RavenOS-Launcher-badging.txt | head -1)"
VERSION_CODE="$(sed -n "s/.*versionCode='\([^']*\)'.*/\1/p" RavenOS-Launcher-badging.txt | head -1)"
BUILT_AT="$(date -u +'%Y-%m-%dT%H:%M:%SZ')"
REPO="${GITHUB_REPOSITORY:-hurrisonferd/RavenOS-Home}"
BRANCH="${GITHUB_REF_NAME:-local}"
RUN_URL="https://github.com/${REPO}/actions/runs/${RUN_ID}"
COMMIT_URL="https://github.com/${REPO}/commit/${GITHUB_SHA}"
RELEASE_URL="https://github.com/${REPO}/releases/tag/${TAG}"
APK_URL="https://github.com/${REPO}/releases/download/${TAG}/${APK_NAME}"

jq -n \
  --arg schema 'ravenos-launcher-apk-archive/v1' \
  --arg product 'RavenOS Launcher' \
  --arg package 'com.ravenos.launcher' \
  --arg source_repo "$REPO" \
  --arg source_sha "$GITHUB_SHA" \
  --arg source_branch "$BRANCH" \
  --arg run_id "$RUN_ID" \
  --arg run_attempt "$ATTEMPT" \
  --arg run_url "$RUN_URL" \
  --arg commit_url "$COMMIT_URL" \
  --arg built_at "$BUILT_AT" \
  --arg apk_filename "$APK_NAME" \
  --arg apk_sha256 "$SHA256" \
  --arg apk_bytes "$BYTES" \
  --arg version_name "$VERSION_NAME" \
  --arg version_code "$VERSION_CODE" \
  --arg archive_folder "$ARCHIVE_FOLDER" \
  --arg release_tag "$TAG" \
  --arg release_url "$RELEASE_URL" \
  --arg apk_download_url "$APK_URL" \
  '{schema:$schema,product:$product,package:$package,source_repo:$source_repo,source_sha:$source_sha,source_branch:$source_branch,run_id:$run_id,run_attempt:$run_attempt,run_url:$run_url,commit_url:$commit_url,built_at:$built_at,apk_filename:$apk_filename,apk_sha256:$apk_sha256,apk_bytes:($apk_bytes|tonumber),version_name:$version_name,version_code:$version_code,archive_folder:$archive_folder,release_tag:$release_tag,release_url:$release_url,apk_download_url:$apk_download_url}' \
  > RavenOS-Launcher-archive-manifest.json

cat RavenOS-Launcher-archive-manifest.json
if [[ -n "${GITHUB_ENV:-}" ]]; then
  {
    echo "APK_NAME=$APK_NAME"
    echo "ARCHIVE_FOLDER=$ARCHIVE_FOLDER"
    echo "TAG=$TAG"
  } >> "$GITHUB_ENV"
fi
