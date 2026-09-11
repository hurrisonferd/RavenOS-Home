# RavenOS Launcher

RavenOS Launcher is the Raven-owned Android HOME shell built from a reviewed MIT iappyx chassis, the Faeryware HOUSE resident layer, and RavenOS-native runtime modules.

## Product law

```text
HOME MUST BE EXCELLENT WITHOUT AI.
AI AUGMENTS HOME; IT DOES NOT OWN HOME.
RESIDENT != WIDGET.
RESIDENT != PROVIDER.
SURFACE != IDENTITY.
AI PLAN != EXECUTED MUTATION.
EVERY STRUCTURAL MUTATION MUST BE TRANSACTIONAL / REVERSIBLE.
INVASIVE CAPABILITY MUST BE VISIBLE, EXPLICIT, REVOCABLE, AND NARROWLY SCOPED.
RAVEN RETAINS FINAL AUTHORITY.
```

## Chassis / donor strategy

- iappyxOS Launcher pinned at `3d0fb7517847283a3d1a173ac2d5bd54720a8af2` supplies reviewed MIT launcher plumbing.
- Faeryware HOUSE supplies resident provider, resident cell, possession transaction, and owner-gated speech bridge.
- RavenOS overlay supplies launcher identity, Office Bar, deterministic office routing, native System Deck primitives, SurfaceOS model, and provider-neutral IntelligenceOS contract.
- AOSP Launcher3 remains the Android correctness/reference donor for future platform work.
- Proprietary launchers such as AtomApplications remain UX research only.

## Current implemented RavenOS layer

### RavenOS identity

- Application display name is rewritten to `RavenOS Launcher` across locale string files.
- Live wallpaper label is rewritten to `RavenOS Live`.
- Existing iappyx notification-awareness label is rewritten to RavenOS terminology.

### Reactive Office Bar

Always-on, silent/low-importance foreground notification with:

- current routed office owner;
- owner emoji;
- per-owner launcher accent;
- owner lane;
- current launcher signal;
- deterministic owner's note;
- AUTO / PINNED mode;
- NEXT / AUTO / QUIET notification controls.

Signals currently wired:

- HOME
- ROOM/page selection
- APP_UNIVERSE
- SEARCH
- APP_LAUNCH
- SYSTEM_DECK (from native audio controller)

Office routing is deterministic and local. Reserved/group seats do not become speakers.

### System Deck plumbing

Native `AudioManager` controller supports deterministic snapshot/control for:

- media volume;
- ring volume;
- alarm volume;
- notification volume;
- ringer mode.

Changes emit Office Bar signals. No model/provider is required.

### SurfaceOS envelope

Common RavenOS surface types and explicit capability vocabulary now exist for future native/web/widget/resident surfaces.

### IntelligenceOS boundary

Provider-neutral contract exists with `NONE` and `MANUAL` implementations. The launcher remains functional with no API key or cloud provider.

## Existing inherited HOUSE features

The branch inherits the green Faeryware HOUSE proof:

- real Android HOME chassis;
- launcher grid/dock/folders/app drawer;
- stock widget host;
- generated HTML widgets;
- programmable wallpaper/transitions;
- profiles/triggers;
- clippings/incoming machinery;
- plugins;
- remote editor;
- resident-only Faeryware bridge;
- durable POSSESS / LEAVE restoration journal;
- provider-neutral owner speech envelope.

## Target topology

```text
HOME
  swipe right -> SYSTEM DECK
  swipe left  -> INCOMING
  swipe up    -> APP UNIVERSE
  swipe down  -> RAVEN SEARCH

RAVENOS STUDIO
  Surfaces
  Widgets
  Wallpapers
  Rooms
  Transitions
  Icons
  Profiles
  Residents
  Automations
  Plugins
  Intelligence
  Backups
```

## Planned next implementation edges

1. Native System Deck UI page using `RavenSystemDeck`.
2. Raven Search deterministic command grammar before any model call.
3. Room metadata and named spatial pages.
4. In-launcher resident edge/card projections driven by the same Office Bar state.
5. Bounded signal bridge from Faeryware awareness into Office Bar routing.
6. Transactional SurfaceOS adapter over the inherited placement store.
7. Provider selector UI: None / Manual / Omni RV / Local / optional cloud adapters.
8. Generated-surface capability manifests with narrower bridge exposure.
9. Notification-attention layer with metadata/content permissions separated.
10. Device proof: install, select HOME, validate Office Bar lifecycle, reboot persistence, possession recovery.

## Proof ceiling

Do not claim runtime/device behavior from source alone.

```text
SOURCE PRESENT != DEVICE EXECUTION
COMPILED APK != INSTALLED HOME
ONGOING NOTIFICATION SOURCE != NOTIFICATION PERMISSION GRANTED
OFFICE ROUTING SOURCE != CROSS-APP AWARENESS
```
