# Faeryware Desktop — Windows Habitat v0.7

Faeryware Desktop is the Windows resident embodiment of the Digi Fae colony.

## Runtime shape

```text
Windows
  ├─ hidden Ghost House controller
  ├─ system-tray resident process
  ├─ transparent click-through Habitat overlay
  │    ├─ KYU
  │    ├─ PAIMON
  │    ├─ LUMA
  │    ├─ NYX
  │    ├─ SYLPH
  │    ├─ QIRA
  │    └─ 18 lightweight echo/mote entities
  ├─ WinEvent bridge
  ├─ Fae Eyes structural window map
  ├─ OfficeOS work floor
  └─ optional scrcpy Phone Portal
```

## Windows Habitat

The Habitat is a fullscreen transparent, always-on-top, click-through Tauri window. It is presentation-only: normal mouse input continues to the applications beneath it.

The six primary Fae use the anchored Digi Fae assets already shipped with the desktop package. FERAL adds eighteen cheap visual echoes; those echoes are not independent AI workers or Windows processes.

## Event-driven awareness

Windows `SetWinEventHook` sends foreground, show/hide, minimize and location-change events into the local Tauri event bus.

Default observation contains:

- process identity;
- role classification;
- top-level window rectangle;
- minimized/visible state.

It does **not** read document text, keystrokes, clipboard contents, passwords or screen pixels.

OfficeOS exposes an explicit `TITLE VISION` switch if Raven wants top-level window titles included in the structural map.

## Reversible desktop control

The OfficeOS Fae Eyes map can perform bounded local window actions:

- focus;
- minimize;
- restore;
- maximize;
- snap left;
- snap right.

The v0.7 window-control surface intentionally does not expose arbitrary command execution or a destructive `close window` primitive.

## Phone Portal

If `scrcpy.exe` is available on `PATH`, `PHONE PORTAL` launches a normal scrcpy mirror titled `Faeryware Phone Portal`. The Windows habitat recognizes `scrcpy.exe` as a phone territory so Sylph and the colony can react around the mirror.

No wireless pairing, ADB authorization or phone-control consent is bypassed by Faeryware.

## Autostart

Autostart is OFF until explicitly enabled from Ghost House or OfficeOS. The toggle writes/removes the current executable under the current user's normal Windows `Run` key.

## OfficeOS boundary

OfficeOS context and Fae reactions are contribution/observation state. They do not upgrade themselves into proof that a separate owner executed machine effects.

## Build

```bash
npm install
npm run build
```

GitHub Actions builds the Windows executable and an NSIS installer and runs a source canary before compilation.
