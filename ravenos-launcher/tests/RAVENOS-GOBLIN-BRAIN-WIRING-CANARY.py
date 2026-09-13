#!/usr/bin/env python3
from __future__ import annotations
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "iappyx-overlay" / "src" / "launcher" / "app" / "src" / "main" / "java" / "com" / "iappyx" / "launcher" / "ravenos"


def text(name: str) -> str:
    p = SRC / name
    if not p.is_file():
        raise SystemExit(f"GOBLIN_WIRING_FAIL:missing_source:{name}")
    return p.read_text(encoding="utf-8")


def require(cond: bool, msg: str) -> None:
    if not cond:
        raise SystemExit(f"GOBLIN_WIRING_FAIL:{msg}")


def main() -> int:
    office = text("RavenOfficeBarService.kt")
    bus = text("RavenGoblinBrainBusOS.kt")
    brain = text("RavenGoblinBrain.kt")
    store = text("RavenReactionStateStore.kt")
    overlay = text("RavenGoblinVisionOverlay.kt")
    follow = text("RavenFollowMeOverlay.kt")
    registry = text("RavenGoblinSystemsRegistryOS.kt")

    # One canonical event/reaction throat.
    require("RavenGoblinBrainBusOS.react" in office, "office_bar_does_not_call_bus")
    require("RavenGoblinBrain.react" in bus, "bus_does_not_call_brain")
    require("RavenReactionStateStore.write" in office, "settled_packet_not_written")

    # Sitcom / continuity organs must be in the real brain path, not docs only.
    required_brain_calls = [
        "RavenMarkerBus.emit",
        "RavenLocalSenseOS.resolve",
        "RavenComplexEventOS.analyze",
        "RavenEpisodeOS.phase",
        "RavenCallbackMemoryOS.observe",
        "RavenSessionNarrativeOS.observe",
        "RavenScreenContextOS.snapshot",
        "RavenEpisodeScriptOS.observe",
        "RavenInterruptibilityOS.evaluate",
        "RavenSitcomDirectorOS.direct",
        "RavenBitLedgerOS.observe",
        "RavenMetaMaxShowrunnerOS.direct",
        "RavenPlotStackOS.snapshot",
        "RavenBackstageOS.observe",
        "RavenOfficeSeasonOS.snapshot",
        "RavenGoldSitcomTopologyOS.direct",
        "RavenLongSeriesDialogueOS.select",
        "RavenPresentationArbiterOS.decide",
        "RavenSceneExpressionOS.decorate",
        "RavenEvidenceBoard.record",
    ]
    for call in required_brain_calls:
        require(call in brain, f"brain_pipe_missing:{call}")

    # Late modular bus organs.
    for call in (
        "RavenGoblinSystemsRegistryOS.enabledUnder",
        "RavenGoblinMetaPipelineOS.enrich",
        "RavenKnowledgeBrokerOS.observe",
    ):
        require(call in bus, f"bus_pipe_missing:{call}")

    # Cross-surface settlement: surfaces consume one settled ReactionPacket.
    require("RavenWidgetGoblinBrainModule.broadcast" in store, "widget_packet_fanout_missing")
    require("RavenWatchletOS.render" in store, "watchlet_packet_fanout_missing")
    require("RavenTaskerBridge.emit" in store, "tasker_semantic_fanout_missing")

    # Follow-Me ownership is split intentionally: state/permission seam + newer resident body.
    require("RavenFollowMeOverlay.isEnabled" in overlay, "resident_body_not_bound_to_follow_me_state")
    require("RavenFollowMeOverlay.isPending" in overlay, "follow_me_pending_boundary_missing")
    require("TYPE_APPLICATION_OVERLAY" in follow, "follow_me_overlay_boundary_missing")
    require("RavenGoblinVisionOverlay.renderReaction" in office, "resident_reaction_renderer_missing")

    # Registry must at least enumerate the major architectural organs.
    for organ in (
        "SITCOM_DIRECTOR", "METAMAX_SHOWRUNNER", "GOLD_TOPOLOGY", "PRESENTATION_ARBITER",
        "META_GRAMMAR", "KNOWLEDGE_BROKER", "FOLLOW_ME", "GOBLIN_OVERLAY", "RAVEN_WIDGET",
    ):
        require(f'Organ("{organ}"' in registry, f"registry_missing:{organ}")

    # Prevent the old degradation mode where a tiny occurrence counter replaces the actual showrunner.
    require(brain.count("Raven") >= 25, "brain_suspiciously_thin")
    require("SCREEN" in brain and "PHONE" in brain and "CHIP" in brain and "DIAGNOSTIC" in brain,
            "showrunner_recovery_modes_missing")

    print("RAVENOS_GOBLIN_BRAIN_WIRING_CANARY_PASS")
    print(f"brain_calls={len(required_brain_calls)} bus_layers=3 settled_surface_fanout=3")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
