# Faeryware Desktop Resident

A transparent Tauri desktop-pet shell using the same Digi Fae vocabulary as the Android Overlay Colony.

## Current source-bound behaviors

- transparent, undecorated, always-on-top resident window
- six Raven-supplied resident PNGs: Kyu, Paimon, Luma, Nyx, Sylph, Qira
- deterministic six-expression vocabulary per Fae
- CALM / HAUNTED / FERAL motion
- bounded desktop wandering
- left/right edge perching
- click-through mode
- local persistent Fae / haunt / perch state
- Ghost House controls hidden behind right-click
- double-click cycles resident
- explicit BANISH exits the process

This is an embodiment shell, not autonomous computer control. It does not read keystrokes, scrape windows, execute arbitrary shell commands, or hide itself from the user.

## Run

Install Node.js, Rust, and Tauri desktop prerequisites, then:

```bash
npm install
npm run dev
```

Build:

```bash
npm run build
```

## Next bridge

`../shared/colony-events.v1.json` defines the small event vocabulary intended to be shared with the Android overlay. Context adapters can later emit those events without changing Fae identity or granting arbitrary execution.
