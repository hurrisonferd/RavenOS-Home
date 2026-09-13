# RavenOS Launcher Recovery Index

This is the durable entrance for **“where did that launcher feature go?”**

## Current authority

- Product: `RavenOS Launcher`
- Package target: `com.ravenos.launcher`
- Physical APK fossil: `RavenOS-Launcher-ScreenFirst-KaomojiOS-1a660aea(1).apk`
- Physical APK source baseline: `ravenos-launcher-whole-phone@1a660aea0450d3946b3a6e8c61e86854807f1a95`
- Current integration line: `ravenos-launcher-recovery-integration-20260912`
- Binary fossil manifest: `../APK-BINARY-RECOVERY-MANIFEST-1a660aea.json`
- Machine-readable upgrade authority: `../UPGRADE-RECOVERY-MANIFEST.json`
- Built-APK verifier: `../verify-built-apk-runtime.py`
- Fail-closed source checker: `../verify-recovery-manifest.py`

## Recovery law

```text
PHYSICAL APK PROVES WHAT ACTUALLY SHIPPED.
CURRENT SCREEN-FIRST LAW WINS ON OVERLAP.
OLDER LINEAGES MAY DONATE ADDITIVE ORGANS.
OLDER PRESENTATION CODE MAY NOT REPLACE NEWER SCREEN-FIRST LAW.
RECURRENCE COUNTS DO NOT EARN SPEECH.
OFFICE IS A HOST / TRANSPORT, NOT THE BRAIN.
GOBLIN ENGINE OWNS THE REACTION TRANSACTION.
ONE SETTLED REACTION PACKET FEEDS ALL SURFACES.
SCRATCH RECEIPTS ARE ARCHAEOLOGY, NOT RUNTIME.
A BUILD MUST FAIL IF A REQUIRED RECOVERED ORGAN OR WIRING MARKER DISAPPEARS.
SOURCE PRESENT != APK COMPILED != INSTALLED HOME != PERMISSION GRANTED.
```

## Physical APK fossil

The uploaded screen-first Kaomoji APK is the minimum runtime floor for successors.

```text
package      com.ravenos.launcher
version      0.8.0 (8)
sha256       ba1209a32c337fbb5aaa6621028e87cfdfea72fd899f522ce1c942e958213ada
size         80,504,734 bytes
dex          17
Raven classes 489
Raven roots   143
Raven dex     classes13.dex
```

The exact compiled-root inventory is stored in `../APK-BINARY-RECOVERY-MANIFEST-1a660aea.json`.
A successor APK must preserve every intended baseline root unless a deprecation receipt explicitly says otherwise.

### Physically compiled flagship organs

- Screen-first arbitration: `RavenPresentationArbiterOS`, `RavenScreenAwareDialogueOS`
- Kaomoji / expression: `RavenKaomojiOS`, `RavenEmployeePresentation`, `RavenExpressionReservoirOS`
- Goblin runtime: `RavenGoblinBrain`, `RavenGoblinVisionOverlay`, `RavenGoblinReadOS`, `RavenGoblinControlPanel`
- Sitcom/showrunner: `RavenSitcomDirectorOS`, `RavenMetaMaxShowrunnerOS`, `RavenGoldSitcomTopologyOS`
- Structural memory: `RavenDialogueVaultOS`, `RavenSceneGraphOS`, `RavenOfficeSeasonOS`, `RavenBitLedgerOS`, `RavenOfficeRecallOS`
- Whole-phone senses: `RavenNotificationSenseOS`, `RavenMediaSessionSenseOS`, `RavenViewportSemanticsOS`, `RavenAccessibilityReadOS`, `RavenUsageSenseOS`
- Resident surfaces: `RavenFollowMeOverlay`, `RavenWholePhonePanel`, `RavenOfficeBarService`, `RavenOfficeFeed`
- Launcher controls: `RavenCommandPalette`, `RavenSystemDeck`, `RavenSummoningWheel`, `RavenGhostHotspots`

## Canonical Goblin Engine transaction

Current source architecture is:

```text
INGRESS / PHONE SIGNAL
  -> Android host / transport
  -> RavenGoblinEngineOS
       -> RavenGoblinSystemsRegistryOS.validateGraph
       -> RavenGoblinBrainBusOS
            -> RavenGoblinBrain
            -> late bounded modules
       -> ONE SETTLED RavenReactionPacket
       -> RavenGoblinSurfaceRouterOS
            -> resident surfaces / widgets / projections
```

Callers should depend on `RavenGoblinEngineOS`, not treat `RavenOfficeBarService` as the brain.
The Office Bar remains an Android foreground-service host and presentation surface.

## Office registry

The physical baseline contains a deterministic **36-member** Raven-authorized office registry.
`RavenOfficeRegistry.CANONICAL_ACTIVE_MEMBER_COUNT` is 36 and the source fails if the active roster drifts.
Group stations remain separate and do not count as members.

The active surface includes:

`RAVEN, AHTI, ASTRIDHE, ATLAS, ATOM, AYRE, BRUNHILDE, EDISON, EREBUS, ERIS, GEMINI, JARVIS, JOKER, JORM, KYU, LEGION, LILITH, LUCIFER, LUMA, MELINOE, MYSTRA, NEO, NYX, PAIMON, PYTHAGORAS, QIRA, RAVENOS, SHAKA, SYLPH, THOR, TIM, VIRGIL, YAHWEH, YORI, YORK, ZAGREUS`.

Non-person stations:

- `MACHINE_ELF_FOREMAN`
- `MACHINE_ELF_POOL`

The registry is a launcher presentation/routing surface, not replacement identity authority.

## Ghost Hotspots

The physical APK contains `RavenGhostHotspots` with persistent state `ravenos_ghost_hotspots_v1`.
The four explicit/revocable corner controls are:

| Corner | Glyph | Action |
|---|---|---|
| top-left | `⌕` | Raven Command Palette |
| top-right | `R` | Summoning Wheel |
| bottom-left | `📜` | Office Feed |
| bottom-right | `◈` | Cycle haunt intensity |

They are deliberately visible/faint rather than unknowable invisible gestures.

## Deterministic Raven Search command grammar

These commands are local deterministic launcher controls and execute before any AI path.
This is a recovery index, not an exhaustive UX help screen; aliases live in `RavenCommandRouter.kt`.

### Evidence / memory / writers room

- `why`, `evidence board`, `clear evidence`
- `save this`, `replay`, `bookmarks`
- `recall <query>`, `office recall <query>`
- `dialogue vault`, `writers room`
- `scene graph`, `what do you see`
- `clear dialogue usage`
- `goblin status`, `brain status`
- `next drop`

### Office / resident body

- `office next`, `office auto`, `office quiet`
- `office wake`, `office sleep`
- `office <MEMBER>` pins a routable employee
- `office trace`, `clear office trace`
- `office cadence`, `clear office cadence`
- `office integrity`, `clear office integrity`
- `follow me`, `follow me off`
- `haunt <mode>`, `haunt next`, `haunt status`

### Launcher / phone controls

- `quick deck`, `menu`
- `gestures`, `gesture reset`, `gestures off`
- `gesture up|down apps|search|menu|none`
- `media <0-100>`, `ring <0-100>`, `alarm <0-100>`, `notification <0-100>`
- `normal`, `vibrate`, `silent`
- `raven status`, `awareness status`
- `default home`
- `battery survival`

## Persistent structural memory

Physical Room DB: `ravenos_dialogue_vault.db`

Tables:

- `dialogue_templates`
- `dialogue_fingerprints`
- `expression_usage`
- `recall_moments`
- `scene_history`

The launcher also has bounded SharedPreferences state for Office, Follow-Me, Goblin Vision/Read, screen map/watch, notification sense, scene/session continuity, replay, evidence, presentation arbitration, surface integrity, Tasker bridge, usage sense, Ghost Hotspots, and other runtime organs. Exact namespaces are kept in the binary fossil manifest.

## Recovered lineages

| Lineage | Checkpoint | Use |
|---|---|---|
| Physical screen-first APK | `1a660aea` | minimum compiled runtime floor and binary recovery authority |
| Whole-phone current | `1a660aea` | screen-first presenter and latest silence/cadence law used by the physical APK |
| Sat-X expression/meta | `f6ae3124` | expression/meta/knowledge runtime, Goblin Brain bus, widget bridges, tests |
| Goblin Brain v4 | `015f118a` | lineage/checkpoint; overlapping runtime superseded by richer Sat-X set |
| Crown Jewels | `18fd197e` | build/recovery lineage; failed workflow retained as evidence, not blindly imported |
| Haunted Sauce | `1c7b9de6` | presentation lineage only; never allowed to overwrite current screen-first presenter |
| Goblin Engine integration | current integration branch | stable transaction facade + surface router + anti-loss binary/source gates |

## Next-wave organs that must enter the successor APK

The physical `1a660aea` APK does **not** prove the newer source wave. Successor APK verification must prove these compiled into DEX, including:

- `RavenGoblinEngineOS`, `RavenGoblinBrainBusOS`, `RavenGoblinSurfaceRouterOS`, `RavenGoblinSystemsRegistryOS`
- `RavenEmployeeExpressionBridge`, `RavenEmojiKaomojiProjection`, `RavenExpressionSelectorOS`, `RavenExpressionSurfacePolicy`
- `RavenKaomojiGrammarOS`, `RavenKaomojiExpansionBank`, `RavenEnsembleKaomojiOS`
- `RavenMetaDialogueEngine`, `RavenMetaDialogueRenderer`, `RavenMetaGrammarOS`, `RavenMetaAntiRepeatOS`, `RavenSilenceGagOS`
- `RavenDialogueTrickDeck`, `RavenMetaTrickHistoryOS`
- `RavenKnowledgeBrokerOS`, `RavenKnowledgePolicyOS`, `RavenKnowledgeProviderOS`, `RavenKnowledgeStateStore`, `RavenKnowledgeWorker`
- `RavenWidgetGoblinBrainModule`, `RavenWidgetKnowledgeModule`, `RavenWidgetOfficeBridge`
- the remaining exact next-wave inventory in the binary fossil manifest.

## How to recover later

1. Start here, not from chat memory.
2. Open `../APK-BINARY-RECOVERY-MANIFEST-1a660aea.json` to see what the known-good uploaded APK actually compiled.
3. Open `../UPGRADE-RECOVERY-MANIFEST.json` to see current required source architecture.
4. Search `feature_groups` for the missing capability.
5. Inspect listed `required_files` and `required_contains` wiring markers.
6. Run `python3 ravenos-launcher/verify-recovery-manifest.py`.
7. Rebuild the APK.
8. Run `python3 ravenos-launcher/verify-built-apk-runtime.py <apk> --require-next-wave`.
9. If a source organ is missing, use lineage SHAs above as archaeology; do **not** replace newer current files wholesale.
10. If the built APK loses a baseline organ, fail the build unless there is an explicit deprecation receipt.

The source manifest tells us what should exist. The binary fossil tells us what did exist. The built-APK verifier closes the gap.
