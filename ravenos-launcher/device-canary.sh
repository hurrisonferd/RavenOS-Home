#!/usr/bin/env bash
set -euo pipefail

APK="${1:-RavenOS-Launcher-debug.apk}"
PKG="com.ravenos.launcher"

if ! command -v adb >/dev/null 2>&1; then
  echo "BLOCKED: adb is not installed or not on PATH" >&2
  exit 2
fi
if [[ ! -s "$APK" ]]; then
  echo "BLOCKED: APK not found: $APK" >&2
  exit 2
fi

adb wait-for-device
SERIAL="$(adb get-serialno)"
echo "RAVENOS DEVICE CANARY"
echo "device=$SERIAL"
echo "apk=$APK"

printf '\n[1/7] install distinct RavenOS package\n'
adb install -r "$APK"
PATH_RESULT="$(adb shell pm path "$PKG" | tr -d '\r')"
[[ "$PATH_RESULT" == package:* ]] || { echo "FAIL: $PKG not installed" >&2; exit 1; }
echo "PASS: $PATH_RESULT"

printf '\n[2/7] verify package identity\n'
adb shell dumpsys package "$PKG" | grep -m1 -E 'versionName=|versionCode=' || true
echo "PASS: package=$PKG"

printf '\n[3/7] launch RavenOS\n'
adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null
sleep 2
echo "PASS: launch intent sent"

printf '\n[4/7] HOME role\n'
HOME_HOLDERS="$(adb shell cmd role get-role-holders android.app.role.HOME 2>/dev/null | tr -d '\r' || true)"
if grep -qx "$PKG" <<<"$HOME_HOLDERS"; then
  echo "PASS: RavenOS owns HOME"
else
  echo "PENDING: RavenOS is not default HOME yet"
  echo "Action: System Deck -> MAKE RAVENOS DEFAULT HOME"
fi

printf '\n[5/7] Office Bar notification channel\n'
if adb shell dumpsys notification 2>/dev/null | grep -q 'ravenos_office_bar'; then
  echo "PASS: RavenOS Office Bar channel observed"
else
  echo "PENDING: Office Bar channel not observed yet"
  echo "Action: open System Deck -> WAKE OFFICE BAR"
fi

printf '\n[6/7] awareness grants\n'
NOTIF_LISTENERS="$(adb shell settings get secure enabled_notification_listeners 2>/dev/null | tr -d '\r' || true)"
ACCESSIBILITY="$(adb shell settings get secure enabled_accessibility_services 2>/dev/null | tr -d '\r' || true)"
if grep -q "$PKG" <<<"$NOTIF_LISTENERS"; then
  echo "PASS: notification awareness enabled"
else
  echo "PENDING: notification awareness disabled"
fi
if grep -q "$PKG" <<<"$ACCESSIBILITY"; then
  echo "PASS: foreground awareness enabled"
else
  echo "PENDING: foreground awareness disabled"
fi

printf '\n[7/7] deterministic command / reactive-bar manual canary\n'
cat <<'EOF'
On-device checks:
  1. Swipe into SYSTEM Deck and confirm APP = com.ravenos.launcher.
  2. Run LOCAL RAVENOS CANARY and record readiness X/6.
  3. Raven Search: media 37 -> media volume should become 37%.
  4. Raven Search: office kyu -> Office Bar should pin 💗 KYU.
  5. Raven Search: office auto -> context routing resumes.
  6. Raven Search: office sleep -> bar disappears and launcher signals do not revive it.
  7. Raven Search: office wake -> bar returns.
  8. Enable Foreground Awareness; switch Spotify/browser/Settings and verify owner/note changes.
  9. Reboot once with Office Bar awake; verify it returns after normal BOOT_COMPLETED.
 10. Reboot once with Office Bar asleep; verify it stays asleep.
EOF

echo
echo "DEVICE_CANARY_SOURCE_COMPLETE=true"
echo "RUNTIME_RESULT=REQUIRES_HUMAN_OBSERVATION_FOR_UI_AND_REBOOT_EDGES"
