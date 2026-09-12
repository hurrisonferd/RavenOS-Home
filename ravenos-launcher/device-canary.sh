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

printf '\n[3/8] verify package identity + services\n'
PACKAGE_DUMP="$(adb shell dumpsys package "$PKG" 2>/dev/null | tr -d '\r')"
grep -m1 -E 'versionName=|versionCode=' <<<"$PACKAGE_DUMP" || true
if grep -q 'IappyxWallpaperService' <<<"$PACKAGE_DUMP"; then
  echo "PASS: RavenOS live-wallpaper service registered"
else
  echo "FAIL: RavenOS live-wallpaper service not registered" >&2
  exit 1
fi
if grep -q 'RavenScreenWatchService' <<<"$PACKAGE_DUMP"; then
  echo "PASS: owner-armed Goblin Eye service registered"
else
  echo "FAIL: Goblin Eye MediaProjection service not registered" >&2
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

printf '\n[6/8] Office Bar / Goblin Eye notification channels\n'
NOTIF_DUMP="$(adb shell dumpsys notification 2>/dev/null || true)"
if grep -q 'ravenos_office_bar' <<<"$NOTIF_DUMP"; then
  echo "PASS: RavenOS Office Bar channel observed"
else
  echo "PENDING: Office Bar channel not observed yet"
  echo "Action: open System Deck -> WAKE OFFICE BAR"
fi
if grep -q 'ravenos_goblin_eye' <<<"$NOTIF_DUMP"; then
  echo "PASS: Goblin Eye channel already observed"
else
  echo "PENDING: Goblin Eye channel appears after first owner-armed capture session"
fi

printf '\n[7/8] awareness grants\n'
NOTIF_LISTENERS="$(adb shell settings get secure enabled_notification_listeners 2>/dev/null | tr -d '\r' || true)"
ACCESSIBILITY="$(adb shell settings get secure enabled_accessibility_services 2>/dev/null | tr -d '\r' || true)"
if grep -q "$PKG" <<<"$NOTIF_LISTENERS"; then
  echo "PASS: notification + media-session awareness enabled"
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
  1. HOME: confirm the small R edge tab is visible without blocking ordinary icon/widget interaction.
  2. Tap R -> Raven Menu opens with Quick Deck / Home / Apps / Search / System / Gesture controls.
  3. Long-press R -> RavenOS Gesture Controls opens directly.
  4. Open Quick Deck -> MEDIA/RING/ALARM sliders work immediately without entering Studio or configuring AI.
  5. From Quick Deck, Home / Apps / Search / System buttons each reach the expected destination.
  6. System Deck -> NAVIGATION / ERGONOMICS shows current UP/DOWN mappings and exposes OPEN RAVEN MENU / CONFIGURE GESTURES / RESET GESTURES.
  7. Raven Search: `gestures` -> returns current mapping. `gesture up menu` -> upward swipe opens Raven Menu. `gesture down none` -> downward launcher action is disabled while the R edge tab still works.
  8. Raven Search: `gestures off` -> both vertical launcher gestures stop firing. Explicit R menu must still reach Apps/Search/System. `gesture reset` restores Up=Apps / Down=Search.
  9. Raven Search: `quick deck` -> native Quick Deck opens.
 10. HAUNTED Home Whisper should be visibly smaller than the earlier billboard-sized build; LIVED-IN is smaller still. FERAL/APOCALYPSE deliberately scale larger.
 11. Swipe into SYSTEM Deck and confirm APP = com.ravenos.launcher.
 12. Run LOCAL RAVENOS CANARY and record readiness.
 13. Raven Search: media 37 -> media volume should become 37%.
 14. Raven Search: office kyu -> Office Bar, Home Whisper, and Home Aura all switch to 💗 KYU / KYU accent / KYU note.
 15. In SYSTEM Deck -> SURFACE INTEGRITY, refresh. CANONICAL should be SETTLED/CURRENT; OFFICE_BAR should be POSTED/CURRENT; HOME_AURA and HOME_WHISPER should be RENDERED/CURRENT. FOLLOW_ME may be INACTIVE until explicitly enabled.
 16. In Studio -> Widgets, place bundled "RavenOS Office". It must show the same current owner/note/accent as Home and update live on `office atom`, `office yori`, then `office auto`.
 17. Refresh SURFACE INTEGRITY after the widget visibly changes. WIDGETS must graduate from DISPATCHED/CURRENT to CONSUMED/CURRENT with detail identifying widget=ravenos_office.
 18. In Studio -> Wallpapers, select bundled "RavenOS Office". It must receive the same owner/accent/note and react to later office changes without reloading the launcher.
 19. Refresh SURFACE INTEGRITY after the wallpaper visibly changes. WALLPAPER_CHANNEL must graduate from DISPATCHED/CURRENT to CONSUMED/CURRENT with detail wallpaper_js.
 20. Change resident again (`office kyu`, `office atom`, or `office yori`) and refresh integrity. Both WIDGETS and WALLPAPER_CHANNEL must acknowledge the new exact canonical updatedAt.
 21. Raven Search: office auto -> deterministic context routing resumes across Home, Office Bar, Follow-Me, widget, and wallpaper.
 22. Raven Search: haunt calm -> Home Whisper hides, Home Aura becomes minimal, Follow-Me disappears even if overlay permission remains granted.
 23. Raven Search: haunt lived-in -> compact Home presence returns while Follow-Me remains suppressed.
 24. Raven Search: haunt haunted / feral / apocalypse -> projections intensify without granting any new Android permission.
 25. Raven Search: clear office cadence, then rapidly bounce Spotify -> browser -> Settings and trigger several notifications. `office cadence` must show accepted + suppressed counts; lower haunt levels should suppress more flapping.
 26. Repeat the same quick switching in APOCALYPSE. `office cadence` should show a much shorter hold/duplicate window and visibly faster resident changes.
 27. Confirm a higher-priority event (screen off / battery low / power transition) can interrupt ordinary app/notification dwell instead of waiting behind it.
 28. Enable Foreground Awareness; switch Spotify/browser/Settings and verify deterministic owner/note changes and human-readable app labels.
 29. Grant overlay access; run `follow me` and verify the draggable Office chip follows across apps. Refresh SURFACE INTEGRITY: FOLLOW_ME must become RENDERED/CURRENT.
 30. Tap Follow-Me -> RavenOS reopens; drag it -> position persists.
 31. Turn screen off -> Office receipt routes NIGHT/screen:off. Unlock -> HOME/user:present receipt.
 32. Plug power in/out -> POWER receipts include local battery percentage.
 33. Raven Search: office trace -> bounded local routing history contains owner/signal/haunt/context receipts.
 34. Raven Search: office integrity -> same proof ledger is available through deterministic Raven Search, including CONSUMED receipts from widget/wallpaper when those proof surfaces are active.
 35. Raven Search: clear office trace -> trace clears; new activity starts a fresh history.
 36. Raven Search: office sleep -> Office Bar and Follow-Me must report INACTIVE on the integrity ledger; ordinary signals do not revive them. `office wake` restores presence.
 37. While Office wallpaper is hidden behind another app, leave it for a minute; return Home and confirm it resumes current Office state rather than continuously burning visible animation work off-screen.
 38. Reboot once awake and once asleep: awake restores after BOOT_COMPLETED; explicit sleep survives reboot.

Whole-phone Goblin Vision checks:
 39. Native Home -> R Menu -> 👁 Whole-phone senses. Status should show RavenOS readiness plus NOTIFICATION SENSE, MEDIA SESSION, Galaxy/Android readiness, and EYE state.
 40. Notification Sense defaults to SOURCE. Post notifications from several apps; Office reactions may use package/category/importance/conversation/alerting/burst metadata but must not quote title/body. Switch to SEMANTIC LOCAL and verify a title may appear locally; switch back to SOURCE and confirm title/body disappear from new reactions.
 41. With notification access granted, play Suno/Spotify/YouTube Music. Whole-phone panel -> REFRESH MEDIA STATE should identify active playback state and track metadata when the app exposes a MediaSession. Pause/resume/change tracks and confirm MEDIA_SESSION reactions evolve without microphone capture.
 42. Tap 👁 ARM GOBLIN EYE. Android must show its own screen-capture consent UI. Before owner approval, EYE remains OFF and no visual markers may be claimed.
 43. Approve the capture. A second persistent notification should say "Goblin Eye armed" and Whole-phone Senses should show EYE=ON. The service must state raw frames are not persisted.
 44. With Goblin Eye armed, move Chrome/Suno/Home/Settings through visibly different screens. Office Feed should gain SCREEN_VISUAL reactions containing LOCAL_VISION proof and coarse motion/delta/luma evidence. They must not claim OCR text or identify pixel semantics that the deterministic analyzer did not derive.
 45. Leave one app visually stable for several seconds. Goblin Eye should become quiet rather than generating commentary every sample. Then perform a large screen transition and confirm the next earned reaction resumes.
 46. Enable Appear-on-top + Follow-Me in HAUNTED/FERAL. Goblin Vision text overlay and eligible Watchlet art should follow over ordinary apps while Android SystemUI/notification shade remains represented by the Office notification rather than a fake over-SystemUI overlay.
 47. Trigger a real notification burst from one source. Office Feed should reflect burst count / alerting / interruption-filter metadata instead of producing identical source-only rows. Remove/dismiss notifications and verify NOTIFICATION_REMOVED can produce payoff/closure rather than another POSTED line.
 48. On Galaxy devices, Whole-phone Senses -> GALAXY / BACKGROUND SURVIVAL should identify the model and whether battery optimization exemption is confirmed. Manually add RavenOS Launcher to Samsung's Never sleeping apps list when persistent haunting is desired.
 49. Tap STOP GOBLIN EYE (panel or persistent notification). EYE must return OFF, the Goblin Eye foreground notification disappears, and no new SCREEN_VISUAL markers are produced after the capture session ends.
 50. Restart Goblin Eye later. Android must ask for capture consent again; RavenOS must not silently reuse the previous MediaProjection grant.
EOF

echo
echo "DEVICE_CANARY_SOURCE_COMPLETE=true"
echo "RUNTIME_RESULT=REQUIRES_HUMAN_OBSERVATION_FOR_ERGONOMICS_QUICK_DECK_HOME_WIDGET_WALLPAPER_OVERLAY_INTEGRITY_CADENCE_SCREEN_POWER_TRACE_REBOOT_NOTIFICATION_RANKING_MEDIA_SESSION_GOBLIN_EYE_AND_GALAXY_SURVIVAL_EDGES"
