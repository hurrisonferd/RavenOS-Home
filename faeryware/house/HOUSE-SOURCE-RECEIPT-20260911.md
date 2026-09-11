# Faeryware HOUSE source receipt — 2026-09-11

## Frozen sources

- RavenOS-Home base: `d5f118e498b5216e6d4ce4fdaf5b4ef5bac10320`
- iappyxOS Launcher: `3d0fb7517847283a3d1a173ac2d5bd54720a8af2`

## Source-level integration now present

- COLONY resident state is split from raw phone signals.
- Local fallback is visual/state only; it no longer owns freeform Fae dialogue.
- Provider-neutral Omni RV speech envelope fails closed by soak state.
- Durable possession restoration anchors exist before launcher mutation work.
- Faeryware exports a read-only `content://com.faeryware.launcher.resident/state` surface restricted in code to the HOUSE launcher package(s).
- The iappyx overlay adds one bridge only for widget id `faeryware_resident`.
- A bundled `faeryware_resident` generated-widget surface renders local resident state and renders speech only when verified upstream text is present.
- The overlay adds narrow provider package visibility and does not add a new iappyx `CellType`.
- `apply-overlay.py` refuses an unreviewed iappyx SHA and missing patch anchors.
- `build-house.sh` initializes the pinned chassis, applies the overlay, and invokes iappyx `:app:assembleDebug` when executed in a network/toolchain-capable checkout.

## Proof ceiling

```text
SOURCE WRITES                         OBSERVED
PINNED IAPPYX GITLINK                OBSERVED
READ-ONLY RESIDENT PROVIDER SOURCE   OBSERVED
RESIDENT-ONLY IAPPYX BRIDGE SOURCE   OBSERVED
BUNDLED RESIDENT CELL SOURCE         OBSERVED
CHECKED-OUT OVERLAY APPLICATION      UNOBSERVED
ANDROID GRADLE COMPILE               UNOBSERVED
APK INSTALL                          UNOBSERVED
ANDROID HOME ROLE                    UNOBSERVED
LIVE CELL EVENT REACTION             UNOBSERVED
PLACEMENT POSSESS/RESTORE            NOT IMPLEMENTED YET
```

The next proof edge is a real checkout running `faeryware/house/build-house.sh`, then installation of COLONY + HOUSE on Android and placement of the bundled `Faeryware Resident` cell.
