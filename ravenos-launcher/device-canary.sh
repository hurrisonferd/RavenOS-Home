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

printf '\n[1/8] inspect packaged haunted surfaces\n'
if command -v unzip >/dev/null 2>&1; then
  unzip -l "$APK" | grep -F 'assets/widgets/ravenos_office.html' >/dev/null \
    || { echo "FAIL: bundled RavenOS Office widget missing from APK" >&2; exit 1; }
  unzip -l "$APK" | grep -F 'assets/wallpapers/ravenos_office.html' >/dev/null \
    || { echo "FAIL: bundled RavenOS Office wallpaper missing from APK" >&2; exit 1; }
  echo "PASS: bundled Office widget + Office wallpaper packaged"
else
  echo "PENDING: unzip unavailable; packaged asset inspection skipped"
fi

printf '\n[2/8] install distinct RavenOS package\n'
adb install -r "$APK"
PATH_RESULT="$(adb shell pm path "$PKG" | tr -d '\r')"
[[ "$PATH_RESULT" == package:* ]] || { echo "FAIL: $PKG not installed" >&2; exit 1; }
echo "PASS: $PATH_RESULT"

printf '\n[3/8] verify package identity + wallpaper service\n'
PACKAGE_DUMP="$(adb shell dumpsys package "$PKG" 2>/dev/null | tr -d '\r')"
grep -m1 -E 'versionName=|versionCode=' <<<"$PACKAGE_DUMP" || true
if grep -q 'IappyxWallpaperService' <<<"$PACKAGE_DUMP"; then
  echo "PASS: RavenOS live-wallpaper service registered"
else
  echo "FAIL: RavenOS live-wallpaper service not registered" >&2
  exit 1
fi
echo "PASS: package=$PKG"

printf '\n[4/8] launch RavenOS\n'
adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null
sleep 2
echo "PASS: launch intent sent"

printf '\n[5/8] HOME role\n'
HOME_HOLDERS="$(adb shell cmd role get-role-holders android.app.role.HOME 2>/dev/null | tr -d '\r' || true)"
if grep -qx "$PKG" <<<"$HOME_HOLDERS"; then
  echo "PASS: RavenOS owns HOME"
else
  echo "PENDING: RavenOS is not default HOME yet"
  echo "Action: System Deck -> MAKE RAVENOS DEFAULT HOME"
fi

printf '\n[6/8] Office Bar notification channel\n'
if adb shell dumpsys notification 2>/dev/null | grep -q 'ravenos_office_bar'; then
  echo "PASS: RavenOS Office Bar channel observed"
else
  echo "PENDING: Office Bar channel not observed yet"
  echo "Action: open System Deck -> WAKE OFFICE BAR"
fi

printf '\n[7/8] awareness grants\n'
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

printf '\n[8/8] deterministic / cross-surface live canary\n'
cat <<'EOF'
On-device checks:
  1. Swipe into SYSTEM Deck and confirm APP = com.ravenos.launcher.
  2. Run LOCAL RAVENOS CANARY and record readiness.
  3. Raven Search: media 37 -> media volume should become 37%.
  4. Raven Search: office kyu -> Office Bar, Home Whisper, and Home Aura all switch to 💗 KYU / KYU accent / KYU note.
  5. In Studio -> Widgets, place bundled "RavenOS Office". It must show the same current owner/note/accent as Home and update live on `office atom`, `office yori`, then `office auto`.
  6. In Studio -> Wallpapers, select bundled "RavenOS Office". It must receive the same owner/accent/note and react to later office changes without reloading the launcher.
  7. Raven Search: office auto -> deterministic context routing resumes across Home, Office Bar, Follow-Me, widget, and wallpaper.
  8. Raven Search: haunt calm -> Home Whisper hides, Home Aura becomes minimal, Follow-Me disappears even if overlay permission remains granted.
  9. Raven Search: haunt lived-in -> compact Home presence returns while Follow-Me remains suppressed.
 10. Raven Search: haunt haunted / feral / apocalypse -> projections intensify without granting any new Android permission.
 11. Raven Search: clear office cadence, then rapidly bounce Spotify -> browser -> Settings and trigger several notifications. `office cadence` must show accepted + suppressed counts; lower haunt levels should suppress more flapping.
 12. Repeat the same quick switching in APOCALYPSE. `office cadence` should show a much shorter hold/duplicate window and visibly faster resident changes.
 13. Confirm a higher-priority event (screen off / battery low / power transition) can interrupt ordinary app/notification dwell instead of waiting behind it.
 14. Enable Foreground Awareness; switch Spotify/browser/Settings and verify deterministic owner/note changes and human-readable app labels.
 15. Grant overlay access; run `follow me` and verify the draggable Office chip follows across apps.
 16. Tap Follow-Me -> RavenOS reopens; drag it -> position persists.
 17. Turn screen off -> Office receipt routes NIGHT/screen:off. Unlock -> HOME/user:present receipt.
 18. Plug power in/out -> POWER receipts include local battery percentage.
 19. Raven Search: office trace -> bounded local routing history contains owner/signal/haunt/context receipts.
 20. Raven Search: clear office trace -> trace clears; new activity starts a fresh history.
 21. Raven Search: office sleep -> Office Bar, Home Aura, Home Whisper, Follow-Me all disappear and ordinary signals do not revive them. `office wake` restores presence.
 22. While Office wallpaper is hidden behind another app, leave it for a minute; return Home and confirm it resumes current Office state rather than continuously burning visible animation work off-screen.
 23. Reboot once awake and once asleep: awake restores after BOOT_COMPLETED; explicit sleep survives reboot.
EOF

echo
echo "DEVICE_CANARY_SOURCE_COMPLETE=true"
echo "RUNTIME_RESULT=REQUIRES_HUMAN_OBSERVATION_FOR_HOME_WIDGET_WALLPAPER_OVERLAY_CADENCE_SCREEN_POWER_TRACE_AND_REBOOT_EDGES"
