# Faeryware iOS

A non-jailbroken iOS port of the resident Faeryware concept.

The Android build uses launcher, overlay, live wallpaper, widget, and tile surfaces. iOS cannot replace SpringBoard or draw arbitrary overlays above other apps, so this port uses the strongest native equivalents:

- SwiftUI colony habitat
- shared GoblinCore continuity through an App Group
- Home Screen and Lock Screen widgets
- ActivityKit Live Activity / Dynamic Island resident perch
- WidgetKit Control Center / Lock Screen / Action Button control
- App Intents for Shortcuts, Siri, Spotlight, and automations
- deterministic six-fae state carried from the Android implementation

## Fae carried from Android

| Fae | Color | Default whisper |
|---|---|---|
| Kyu | pink `#FF4E9D` | `hehe. still here.` |
| Paimon | green `#42DC76` | `hmm... pattern found.` |
| Luma | yellow `#F6CD5C` | `home. lights on.` |
| Sylph | cyan `#46D3FF` | `new path. zoom.` |
| Qira | purple `#CB57FF` | `boundary held. nope.` |
| Nyx | blue `#5A6CE6` | `☾ watching.` |

## Generate the Xcode project

Requirements:

- macOS
- Xcode 16 or newer
- iOS 18+ deployment target
- XcodeGen

```bash
cd faeryware/ios
brew install xcodegen
xcodegen generate
open FaerywareIOS.xcodeproj
```

The generated scheme is `Faeryware`.

## Signing setup

Before installing on a physical iPhone:

1. Select your Apple Developer team for both `Faeryware` and `FaerywareWidgets`.
2. Register these bundle IDs, or replace them with your own:
   - `com.ravenos.faeryware`
   - `com.ravenos.faeryware.widgets`
3. Enable the App Groups capability for both targets.
4. Register or replace the shared group:
   - `group.com.ravenos.faeryware`
5. Enable Live Activities for the main app target if Xcode requests capability reconciliation.

The same App Group string must exist in:

- `Shared/FaerywareCore.swift`
- `App/Faeryware.entitlements`
- `Widgets/FaerywareWidgets.entitlements`

## First run

1. Launch Faeryware.
2. Tap **Haunt Dynamic Island** to start the resident Live Activity.
3. Add **Faeryware Colony** from the Home Screen widget gallery.
4. Add the Lock Screen widget if desired.
5. Add **More Haunted** to Control Center.
6. In Shortcuts, use the generated Faeryware actions for automations.

Suggested automations:

```text
Safari opens      → Enter Fae Room: Explore / Summon Sylph
Messages opens    → Enter Fae Room: Communication / Summon Qira
Notes opens       → Enter Fae Room: Workshop / Summon Paimon
Music opens       → Enter Fae Room: Night Watch / Summon Nyx
Morning time      → Enter Fae Room: Cozy Room / Summon Luma
General/default   → Summon Kyu
```

These are intentionally user-configured. Faeryware does not inspect other apps' UI or notifications.

## Architecture

```text
                 ┌──────────────────────┐
                 │     GoblinCore       │
                 │  ColonyState + I/O   │
                 └──────────┬───────────┘
                            │ App Group
              ┌─────────────┼─────────────┐
              │             │             │
          SwiftUI App      Widgets     App Intents
              │             │             │
              │      ┌──────┴──────┐      │
              │      │             │      │
              │   Lock/Home   Control Center
              │
              └──────── ActivityKit ────────┐
                                            │
                                  Dynamic Island /
                                    Lock Screen
```

## Current MVP boundaries

Implemented in source:

- persistent local colony state
- exact Android fae ordering/colors/whispers
- summon/cycle fae
- Calm → Haunted → Feral cycling
- room state
- widget timeline reloads
- Home/Lock widget
- Live Activity / Dynamic Island UI
- Control Center haunt control
- Shortcuts/App Intents

Not yet included:

- production fae portrait assets
- animated SpriteKit room renderer
- notification scheduling/personality cadence
- Focus-filter integration
- Apple Watch companion
- CloudKit multi-device continuity

Those can layer on without changing the colony-state contract.
