#!/usr/bin/env bash
set -euo pipefail

HOUSE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$HOUSE/../.." && pwd)"
UPSTREAM="$HOUSE/iappyxOS-Launcher"

printf 'Faeryware HOUSE: initialize pinned iappyx chassis\n'
git -C "$REPO_ROOT" submodule update --init --recursive faeryware/house/iappyxOS-Launcher

printf 'Faeryware HOUSE: apply resident overlay\n'
python3 "$HOUSE/apply-overlay.py"

printf 'Faeryware HOUSE: build iappyx launcher debug APK\n'
(
  cd "$UPSTREAM/src/launcher"
  chmod +x ./gradlew
  ./gradlew :app:assembleDebug
)

printf 'Faeryware COLONY: build resident APK\n'
(
  cd "$REPO_ROOT/faeryware/android"
  if [[ -x ./gradlew ]]; then
    ./gradlew :app:assembleDebug
  elif [[ -x "$REPO_ROOT/android/gradlew" ]]; then
    "$REPO_ROOT/android/gradlew" -p "$REPO_ROOT/faeryware/android" :app:assembleDebug
  else
    printf 'No Faeryware Gradle wrapper is committed; HOUSE build succeeded, COLONY build requires Android Studio/Gradle.\n' >&2
  fi
)
