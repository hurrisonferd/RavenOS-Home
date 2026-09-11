# Faeryware HOUSE source receipt — 2026-09-11

## Frozen sources

- RavenOS-Home base: `d5f118e498b5216e6d4ce4fdaf5b4ef5bac10320`
- iappyxOS Launcher: `3d0fb7517847283a3d1a173ac2d5bd54720a8af2`

## Source-level integration now present

- COLONY resident state is split from raw phone signals.
- Local fallback is visual/state only; it no longer owns freeform Fae dialogue.
- Provider-neutral Omni RV speech envelope fails closed by soak state.
- Faeryware exports read-only `content://com.faeryware.launcher.resident/state`, restricted in code to HOUSE launcher package(s).
- The iappyx overlay adds one bridge only for widget id `faeryware_resident`.
- Bundled `faeryware_resident` renders local resident state and only renders speech when verified upstream text is present.
- No new iappyx `CellType` is added.
- HOUSE possession is transactional inside the launcher process:
  - first proof accepts only an existing `GENERATED_WIDGET` target;
  - exact original placement JSON + original page are journaled before mutation;
  - journal is persistent across process/reboot;
  - unreadable journal fails closed rather than becoming empty;
  - LEAVE restores the stored placement and clears the journal entry only after exact restoration is observed.
- Possess/leave/status are exposed only through the existing authenticated Remote Edit server, after its paired-IP/session-cookie gate:
  - `GET /api/faeryware/status`
  - `POST /api/faeryware/possess {"id":"...","resident":"KYU"}`
  - `POST /api/faeryware/leave {"id":"..."}`
- Ghost House has a `PING HOUSE CELL` control that emits one bounded local event without requiring Accessibility or notification access.
- `apply-overlay.py` refuses an unreviewed iappyx SHA and missing patch anchors.
- `build-house.sh` initializes the pinned chassis, applies the overlay, and invokes iappyx `:app:assembleDebug` when executed in a network/toolchain-capable checkout.

## Proof ceiling

```text
SOURCE WRITES                         OBSERVED
PINNED IAPPYX GITLINK                OBSERVED
READ-ONLY RESIDENT PROVIDER SOURCE   OBSERVED
RESIDENT-ONLY IAPPYX BRIDGE SOURCE   OBSERVED
BUNDLED RESIDENT CELL SOURCE         OBSERVED
POSSESSION TRANSACTION SOURCE        OBSERVED
AUTHENTICATED POSSESS/LEAVE ROUTES   OBSERVED
MANUAL LOCAL EVENT SOURCE             OBSERVED
CHECKED-OUT OVERLAY APPLICATION      UNOBSERVED
ANDROID GRADLE COMPILE               UNOBSERVED
APK INSTALL                          UNOBSERVED
ANDROID HOME ROLE                    UNOBSERVED
LIVE CELL EVENT REACTION             UNOBSERVED
LIVE POSSESS/RESTORE                 UNOBSERVED
```

## Next proof edge

Run a real checkout:

```text
faeryware/house/build-house.sh
```

Then on Android:

1. install COLONY and HOUSE APKs;
2. select the iappyx/Faeryware HOUSE build as HOME;
3. place any generated widget and note its placement id;
4. pair Remote Edit;
5. call `/api/faeryware/possess` for that id;
6. confirm the Faeryware Resident cell appears;
7. press `PING HOUSE CELL` in Ghost House and confirm visible state changes;
8. call `/api/faeryware/leave`;
9. confirm the exact original placement returns;
10. reboot between possess and leave and repeat the recovery check.
