# RavenOS Launcher Recovery Index

This is the durable entrance for **“where did that launcher feature go?”**

## Current authority

- Product: `RavenOS Launcher`
- Package target: `com.ravenos.launcher`
- Current overlap authority at recovery start: `ravenos-launcher-whole-phone@1a660aea0450d3946b3a6e8c61e86854807f1a95`
- Integration line: `ravenos-launcher-recovery-integration-20260912`
- Machine-readable authority: `../UPGRADE-RECOVERY-MANIFEST.json`
- Fail-closed checker: `../verify-recovery-manifest.py`

## Recovery law

```text
CURRENT WHOLE-PHONE WINS ON OVERLAP.
OLDER LINEAGES MAY DONATE ADDITIVE ORGANS.
OLDER PRESENTATION CODE MAY NOT REPLACE NEWER SCREEN-FIRST LAW.
RECURRENCE COUNTS DO NOT EARN SPEECH.
SCRATCH RECEIPTS ARE ARCHAEOLOGY, NOT RUNTIME.
A BUILD MUST FAIL IF A REQUIRED RECOVERED ORGAN OR WIRING MARKER DISAPPEARS.
```

## Recovered lineages

| Lineage | Checkpoint | Use |
|---|---|---|
| Whole-phone current | `1a660aea` | overlap authority; screen-first presenter and latest silence/cadence law |
| Sat-X expression/meta | `f6ae3124` | expression/meta/knowledge runtime, Goblin Brain bus, widget bridges, tests |
| Goblin Brain v4 | `015f118a` | lineage/checkpoint; overlapping runtime is superseded by richer Sat-X set |
| Crown Jewels | `18fd197e` | build/recovery lineage; failed workflow retained as evidence, not blindly imported |
| Haunted Sauce | `1c7b9de6` | presentation lineage only; never allowed to overwrite current screen-first presenter |

## Feature map

### Goblin Brain bus
`RavenGoblinBrainBusOS` is the single recovered entrance from `RavenOfficeBarService`.
`RavenGoblinSystemsRegistryOS` makes the recovered systems enumerable instead of implicit.

### Expression / EmojiOS / KaomojiOS
`RavenEmployeeExpressionBridge`, `RavenEmojiKaomojiProjection`, `RavenExpressionSelectorOS`,
`RavenKaomojiGrammarOS`, `RavenKaomojiExpansionBank`, `RavenExpressionOwnerBias`, and
`RavenExpressionSurfacePolicy` preserve owner-native expression while keeping the current
screen-first truth layer in charge.

### Meta dialogue / Sitcom machinery
`RavenGoblinMetaPipelineOS`, `RavenMetaDialogueEngine`, `RavenMetaDialogueRenderer`,
`RavenMetaGrammarOS`, `RavenMetaAntiRepeatOS`, `RavenDialogueTrickDeck`, and
`RavenSilenceGagOS` provide deterministic variety and anti-repeat behavior.

### Knowledge / widgets
`RavenKnowledgeBrokerOS`, policy/provider/state/worker organs, `RavenWidgetKnowledgeModule`,
and `RavenWidgetGoblinBrainModule` recover bounded knowledge and exact reaction-packet
projection into generated surfaces.

### Budgeting
`RavenOmniRvExpressionBudget` sheds decorative expression before core truth under pressure.

## How to recover later

1. Open `ravenos-launcher/UPGRADE-RECOVERY-MANIFEST.json`.
2. Search `feature_groups` for the missing capability.
3. Inspect the listed `required_files`.
4. Run `python3 ravenos-launcher/verify-recovery-manifest.py`.
5. If a file is missing, use the lineage SHA above as archaeology; do **not** replace newer current files wholesale.
6. Rebuild only after the verifier is green.

The manifest is recovery/source authority. It is **not** proof that an APK is installed or device-tested.
