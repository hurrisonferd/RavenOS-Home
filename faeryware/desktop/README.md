# Faeryware Desktop — Fae Eyes v0.8

Faeryware Desktop is the Windows resident embodiment of the Digi Fae colony.

## Runtime shape

```text
Windows
  ├─ hidden Ghost House controller
  ├─ system-tray resident process
  ├─ transparent click-through Habitat overlay
  │    ├─ KYU / PAIMON / LUMA / NYX / SYLPH / QIRA
  │    └─ 18 lightweight echo/mote entities
  ├─ WinEvent structural awareness
  ├─ Fae Eyes pixel vision (explicit opt-in)
  │    ├─ selected visible window snapshot
  │    ├─ full virtual-desktop snapshot
  │    ├─ in-memory downscaled JPEG
  │    └─ local Ollama vision at 127.0.0.1:11434
  ├─ OfficeOS territory + reversible window controls
  └─ optional scrcpy Phone Portal
```

## Fae Eyes tiers

`STRUCTURAL` is always available while context awareness is active: process identity, top-level window bounds, visibility and minimize state. It does not read pixels.

`PIXEL VISION: WINDOW` is explicit. Raven chooses a window with the `LOOK` control (or arms window mode and allows a foreground target). The habitat is briefly hidden before capture so the Fae do not recursively photograph themselves.

`PIXEL VISION: DESKTOP` is explicit. It captures the Windows virtual desktop across monitors, downsizes the frame in memory, and may send that frame only to the local Ollama endpoint when `LOOK NOW` or explicitly enabled `AUTO LOOK` runs.

`AUTO LOOK` is OFF by default. When enabled, significant foreground changes may trigger analysis, rate-limited to one request per 15 seconds.

## Local vision boundary

The native bridge only contacts `http://127.0.0.1:11434/api/chat`. Models whose name contains `:cloud` are rejected by Faeryware v0.8. Screen frames are not written to disk by the capture path.

Ollama-compatible vision models can consume images as base64 through the local chat API. The model is selected in OfficeOS; `gemma3` is only the default field value and must actually be installed locally to work.

Pixel vision is observation, not action authority. A model response is displayed as a Fae observation and cannot directly inject arbitrary keyboard/mouse commands.

## Reversible desktop control

OfficeOS continues to expose bounded local window actions:

- focus
- minimize
- restore
- maximize
- snap left
- snap right

No destructive close primitive or arbitrary shell command is exposed through Fae Eyes.

## Phone Portal

If `scrcpy.exe` is available on PATH, `PHONE PORTAL` launches the normal scrcpy mirror titled `Faeryware Phone Portal`; Windows Habitat recognizes it as phone territory.

## Build

```bash
npm install
npm run build
```

GitHub Actions runs the habitat/Fae Eyes source canary before building the Windows executable and NSIS installer.
