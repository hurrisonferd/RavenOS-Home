# Faeryware House v6 — Dual Habitat

Authority: RAVEN

## Frozen inputs

- RavenOS-Home start: `d5f118e498b5216e6d4ce4fdaf5b4ef5bac10320`
- iappyxOS Launcher upstream: `3d0fb7517847283a3d1a173ac2d5bd54720a8af2`
- Upstream repository: `https://github.com/iappyx/iappyxOS-Launcher`
- License: MIT; preserve upstream copyright/license notices.

## Product split

### COLONY

Another launcher (Samsung One UI or any other HOME) remains HOME. Existing Faeryware surfaces continue to work:

- resident overlay
- wallpaper
- stock Android widget
- narrow foreground-app awareness
- narrow notification-source awareness
- Quick Settings tile
- Ghost House

### HOUSE

The pinned iappyxOS Launcher is the HOME chassis. Faeryware is the resident system layered above launcher plumbing.

```text
IAPPYX = HOUSE PLUMBING
FAERYWARE = RESIDENT SYSTEM
RAVENOS / OMNI RV = OWNER-NATIVE IDENTITY + COGNITION
```

Do not reimplement working launcher machinery merely to make it look native to Faeryware.

## v0.9 resident boundary

The existing Android application now begins the resident/runtime split:

- `ResidentSignalStore` owns bounded local phone observations.
- `ResidentEventBus` routes local facts and refresh signals.
- `ResidentRuntime` selects a resident and visual state without inventing freeform dialogue.
- `ResidentCapabilityPolicy` separates surface occupancy from authority.
- `ResidentOfficeBridge` is provider-neutral and accepts only explicit verified Omni RV envelopes.
- `ResidentPossessionStore` durably preserves displaced placement JSON before any future HOUSE-mode possession.
- `FaerywareMemoryStore` remains only as a compatibility facade for v5 surfaces.

## Anti-Jim mobile law

Local fallback may choose:

- resident
- visual expression/state
- animation
- surface
- urgency

Local fallback may NOT invent generic freeform resident dialogue.

Owner-native speech requires an explicit verified `ResidentOfficeBridge` envelope. Missing/CONFLICT/RESERVED/NON_IDENTITY/OFFLINE state fails closed.

## First HOUSE proof contract

The first iappyx integration must prove exactly this:

1. iappyx remains a functional Android HOME.
2. Existing iappyx grid, app drawer, folders, stock widgets and generated widgets remain operational.
3. KYU exists in `ResidentRuntime` independently of any UI cell.
4. One existing generated-widget placement is selected as the first possession surface; do not add a new `CellType` yet.
5. `ResidentPossessionStore.begin(...)` records the exact original placement JSON before mutation.
6. One bounded local event reaches `ResidentEventBus`.
7. The possessed surface visibly changes state.
8. LEAVE restores the exact stored placement JSON.
9. Only after restoration succeeds may `confirmRestored(...)` erase the restoration anchor.
10. Restart/reboot must not orphan the displacement record.

## iappyx concepts explicitly reused

The pinned upstream already provides the plumbing Faeryware should consume rather than rebuild:

- atomic JSON `PlacementStore`
- launcher page/grid/dock placement model
- `AppWidgetHost` plumbing
- generated widget WebViews / bridge surfaces
- profiles and triggers
- clippings
- quick widgets
- plugins with capability gating and a narrow public facade
- command snapshots / undo concepts
- backup / restore
- search and app drawer

## Proof boundary of this commit

This source pass establishes the resident split, provider-neutral office bridge, durable possession transaction, and pinned upstream relationship.

It does **not** claim:

- an Android checkout was compiled in this ChatGPT host
- an APK was installed
- iappyx was executed as HOME on a device
- a generated iappyx placement has already been possessed

Those require a real checkout/Android toolchain/device run.
