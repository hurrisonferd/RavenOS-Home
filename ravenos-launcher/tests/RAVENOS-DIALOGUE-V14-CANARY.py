#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "iappyx-overlay" / "src" / "launcher" / "app" / "src" / "main" / "java" / "com" / "iappyx" / "launcher" / "ravenos"

def read(name):
    p = JAVA / name
    assert p.is_file(), f"missing:{p}"
    return p.read_text(encoding="utf-8")

director = read("RavenDialogueDirectorOS.kt")
bus = read("RavenGoblinBrainBusOS.kt")
registry = read("RavenGoblinSystemsRegistryOS.kt")
meta = read("RavenGoblinMetaPipelineOS.kt")

# Canonical placement: facts/cast/cadence are already settled by RavenGoblinBrain; dialogue director
# runs on the bus before MetaGrammar and never creates speech from silence.
assert "val base = RavenGoblinBrain.react" in bus
assert "RavenDialogueDirectorOS.rewrite" in bus
assert bus.index("RavenDialogueDirectorOS.rewrite") < bus.index("RavenGoblinMetaPipelineOS.enrich")
assert 'packet.dialogue.isNotBlank() && !quiet && "DIALOGUE_DIRECTOR" in enabledOrgans' in bus
assert 'if (current.isBlank()) return Decision("", "DIRECTOR_SILENCE"' in director
assert '"DIALOGUE_DIRECTOR_SHED"' in bus

# Registry-backed LIGHT writer: it may shed under critical pressure instead of pretending it ran.
assert 'Organ("DIALOGUE_DIRECTOR", Stage.WRITERS, Cost.LIGHT, "PRESENTATION"' in registry
assert 'listOf("SCENE_GRAPH", "PHONE_SCENE", "TRICK_HISTORY")' in registry
assert '"DIALOGUE_DIRECTOR"' not in registry.split("val baseBrainMandatory", 1)[1].split("fun compact", 1)[0]

# The regression shown in device screenshots: notification/telemetry prose cannot monopolize a
# grounded screen scene. Notifications are a cameo form and notification-dominant screen lines are
# explicitly rewrite candidates.
for token in (
    'add("CAMEO")',
    "REWRITE_NOTIFICATION_TO_CAMEO",
    "notificationDominant",
    "notificationHeavy",
    "main-character ambitions",
    "Supporting cast has been reminded not to steal the episode",
):
    assert token in director, token

# Chat/sitcom variety is structural, not just a bigger pile of notification synonyms.
for form in (
    "FOURTH_WALL", "SOUNDTRACK", "CALLBACK", "RAVEN_ACTION", "RAPID_CUT",
    "CAMEO", "CUBICLE", "APP_GOSSIP", "WORKPLACE", "DEADPAN",
):
    assert form in director, form
for line in (
    "This is a normal workplace now",
    "The fourth wall has opened a ticket",
    "the phone has a music supervisor",
    "three goblins are pretending this counts as work",
    "develop office politics",
    "absolutely no reason to open a PowerPoint",
    "Android may stop pitching subplots",
):
    assert line in director, line

# Exact degraded phrase families from the field test are explicitly expensive.
for phrase in (
    "callback privileges unlocked",
    "third media move",
    "reset point reached",
    "keep the move reversible",
    "compress toward the decision",
    "shipped result",
    "foreground reassigned",
    "resident systems nominal",
):
    assert phrase in director.lower(), phrase
assert "REWRITE_STIFF_TELEMETRY" in director

# Every synthetic office member has an owner-native conversational button; Raven remains human
# authority rather than a synthetic cast voice.
non_raven = [
    "AHTI","ASTRIDHE","ATLAS","ATOM","AYRE","BRUNHILDE","EDISON","EREBUS","ERIS","GEMINI",
    "JARVIS","JOKER","JORM","KYU","LEGION","LILITH","LUCIFER","LUMA","MELINOE","MYSTRA","NEO",
    "NYX","PAIMON","PYTHAGORAS","QIRA","RAVENOS","SHAKA","SYLPH","THOR","TIM","VIRGIL","YAHWEH",
    "YORI","YORK","ZAGREUS",
]
for owner in non_raven:
    assert f'"{owner}" -> listOf(' in director, owner
assert '"RAVEN" -> listOf(' not in director

# Anti-repeat persists only hashes/forms, not screen/track/notification content.
assert 'putString("recent_hashes"' in director
assert 'putString("last_form"' in director
for forbidden_persist in (
    'putString("subject"', 'putString("screen"', 'putString("track"', 'putString("notification"',
    'putString("dialogue"', 'putString("app"',
):
    assert forbidden_persist not in director
assert "RavenMetaTrickHistoryOS.choose" in director

# Presentation writer only: no action/device authority.
for forbidden in (
    "performAction(", "dispatchGesture(", "performGlobalAction(", "startActivity(", "sendBroadcast(",
    "MediaProjection", "TransportControls", "ACTION_SET_TEXT",
):
    assert forbidden not in director, forbidden

# Meta grammar remains downstream, so the director improves base conversation while existing
# late-stage jokes/ensemble grammar can still decorate the selected line.
assert "baseDialogue" in meta

print("RAVENOS_DIALOGUE_V14_CANARY=PASS")
print("DIALOGUE=SCENE_FIRST_CHAT_SITCOM")
print("NOTIFICATIONS=SUPPORTING_CAST")
print("ANTI_REPEAT=STRUCTURAL_HASHES_ONLY")
print("OWNER_NATIVE_BUTTONS=35_SYNTHETIC_MEMBERS")
print("RAVEN=HUMAN_AUTHORITY_NOT_SYNTHETIC_CAST")
print("EFFECT_AUTHORITY=NONE")
