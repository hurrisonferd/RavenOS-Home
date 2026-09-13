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
    engine = text("RavenGoblinEngineOS.kt")
    router = text("RavenGoblinSurfaceRouterOS.kt")
    bus = text("RavenGoblinBrainBusOS.kt")
    brain = text("RavenGoblinBrain.kt")
    module_rack = text("RavenGoblinModuleRackOS.kt")
    extension_rack = text("RavenGoblinExtensionRackOS.kt")
    store = text("RavenReactionStateStore.kt")
    overlay = text("RavenGoblinVisionOverlay.kt")
    follow = text("RavenFollowMeOverlay.kt")
    registry = text("RavenGoblinSystemsRegistryOS.kt")

    # One canonical reaction transaction.
    require("RavenGoblinEngineOS.react" in office, "office_bar_does_not_call_engine")
    require("RavenGoblinBrainBusOS.react" in engine, "engine_does_not_call_module_bus")
    require("RavenGoblinSurfaceRouterOS.settle" in engine, "engine_does_not_settle_surfaces")
    require("RavenGoblinBrain.react" in bus, "module_bus_does_not_call_brain")

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

    # Modular load planning and executable late organs.
    require("RavenGoblinModuleRackOS.plan" in bus, "bus_does_not_use_module_rack")
    require("RavenGoblinExtensionRackOS.apply" in bus, "bus_does_not_use_extension_rack")
    require("RavenGoblinSystemsRegistryOS.enabledUnder" in module_rack, "module_rack_not_bound_to_registry")
    require("dependency:" in module_rack, "module_rack_dependency_shedding_missing")
    require("mandatory_dependency_shed" in module_rack, "module_rack_mandatory_dependency_alarm_missing")
    require("interface Extension" in extension_rack, "extension_contract_missing")
    require('override val id: String = "META_GRAMMAR"' in extension_rack, "meta_grammar_extension_missing")
    require('override val id: String = "KNOWLEDGE_BROKER"' in extension_rack, "knowledge_extension_missing")
    require("RavenGoblinMetaPipelineOS.enrich" in extension_rack, "meta_pipeline_not_executed_by_extension")
    require("RavenKnowledgeBrokerOS.observe" in extension_rack, "knowledge_broker_not_executed_by_extension")

    # Cross-surface settlement: every surface consumes the same settled ReactionPacket.
    require("RavenReactionStateStore.write" in router, "reaction_state_settlement_missing")
    require("RavenOfficeStateStore.write" in router, "office_state_settlement_missing")
    require("RavenOfficeTraceStore.record" in router, "office_trace_settlement_missing")
    require("RavenGoblinVisionOverlay.renderReaction" in router, "resident_reaction_renderer_missing")
    require("RavenHomeAura.render" in router, "home_aura_projection_missing")
    require("RavenHomeWhisper.render" in router, "home_whisper_projection_missing")
    require("RavenWidgetGoblinBrainModule.broadcast" in store, "widget_packet_fanout_missing")
    require("RavenWatchletOS.render" in store, "watchlet_packet_fanout_missing")
    require("RavenTaskerBridge.emit" in store, "tasker_semantic_fanout_missing")

    # Follow-Me ownership is split intentionally: state/permission seam + newer resident body.
    require("RavenFollowMeOverlay.isEnabled" in overlay, "resident_body_not_bound_to_follow_me_state")
    require("RavenFollowMeOverlay.isPending" in overlay, "follow_me_pending_boundary_missing")
    require("TYPE_APPLICATION_OVERLAY" in follow, "follow_me_overlay_boundary_missing")

    # Registry is now the recoverable module graph for launcher sources, brain, governance and surfaces.
    required_organs = (
        "SCREEN_MONITOR", "PHONE_PULSE", "USAGE_SENSE", "NOTIFICATION_SENSE", "MEDIA_SESSION",
        "ACCESSIBILITY_READ", "SITCOM_DIRECTOR", "METAMAX_SHOWRUNNER", "GOLD_TOPOLOGY",
        "PRESENTATION_ARBITER", "MODULE_RACK", "LATE_EXTENSION_RACK", "GOBLIN_ENGINE",
        "META_GRAMMAR", "KNOWLEDGE_BROKER", "SURFACE_ROUTER", "OFFICE_BAR", "FOLLOW_ME",
        "GOBLIN_OVERLAY", "RAVEN_WIDGET", "WATCHLET", "TASKER_BRIDGE", "WALLPAPER_CHANNEL",
    )
    for organ in required_organs:
        require(f'Organ("{organ}"' in registry, f"registry_missing:{organ}")
    require("fun validateGraph()" in registry, "module_graph_validator_missing")

    # Prevent the old degradation mode where a tiny occurrence counter replaces the actual showrunner.
    require(brain.count("Raven") >= 25, "brain_suspiciously_thin")
    require("SCREEN" in brain and "PHONE" in brain and "CHIP" in brain and "DIAGNOSTIC" in brain,
            "showrunner_recovery_modes_missing")

    print("RAVENOS_GOBLIN_BRAIN_WIRING_CANARY_PASS")
    print(
        f"brain_calls={len(required_brain_calls)} module_rack=dependency-aware "
        f"extensions=2 surface_router=9 registry_organs={len(required_organs)}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
