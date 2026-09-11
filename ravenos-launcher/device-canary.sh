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

printf '\n[7/7] deterministic command / reactive-presence manual canary\n'
cat <<'EOF'
On-device checks:
  1. Swipe into SYSTEM Deck and confirm APP = com.ravenos.launcher.
  2. Run LOCAL RAVENOS CANARY and record readiness X/7.
  3. Raven Search: media 37 -> media volume should become 37%.
  4. Raven Search: office kyu -> Office Bar, Home Whisper, and Home Aura should all switch to 💗 KYU / KYU accent / KYU note.
  5. Raven Search: office auto -> context routing resumes across all three projections.
  6. Raven Search: haunt status -> confirm the current deterministic haunt mode.
  7. Raven Search: haunt calm -> Home Whisper hides, Home Aura becomes minimal, and Follow-Me disappears even if overlay permission remains granted.
  8. Raven Search: haunt lived-in -> Home Whisper returns compact, Home Aura grows slightly, foreground routing is active, Follow-Me stays suppressed.
  9. Raven Search: haunt haunted -> foreground + notification routing resume; Home Whisper is fuller; Follow-Me may project if separately enabled.
 10. Raven Search: haunt feral / haunt apocalypse -> Home Aura and Home Whisper visibly intensify; APOCALYPSE is highest-detail without granting new permission.
 11. Enable Foreground Awareness; switch Spotify/browser/Settings and verify owner/note changes and human-readable app labels.
 12. Grant overlay access; run `follow me` and verify the draggable Office chip follows across apps.
 13. Tap the Follow-Me chip -> RavenOS should reopen; drag it -> position should persist.
 14. Turn screen off -> Office receipt should route NIGHT/screen:off (normally NYX/LUMA/EREBUS/AYRE lane). Unlock -> HOME/user:present receipt.
 15. Plug power in/out and verify POWER receipts include local battery percentage. Trigger battery-low if practical and verify BATTERY receipt.
 16. Raven Search: office trace -> confirm bounded local routing history contains recent owner/signal/haunt/context entries.
 17. Raven Search: clear office trace -> trace clears, then new activity begins a fresh bounded history.
 18. Raven Search: office sleep -> Office Bar, Home Aura, Home Whisper, and Follow-Me all disappear; ordinary launcher/device signals do not revive them. `office wake` restores presence.
 19. Reboot once awake and once asleep: awake state should restore after BOOT_COMPLETED; explicit sleep must survive reboot.
EOF

echo
echo "DEVICE_CANARY_SOURCE_COMPLETE=true"
echo "RUNTIME_RESULT=REQUIRES_HUMAN_OBSERVATION_FOR_HOME_AURA_WHISPER_OVERLAY_SCREEN_POWER_TRACE_AND_REBOOT_EDGES"
