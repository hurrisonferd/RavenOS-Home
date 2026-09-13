#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "iappyx-overlay" / "src" / "launcher" / "app" / "src" / "main" / "java" / "com" / "iappyx" / "launcher" / "ravenos"

def read(name):
    p = JAVA / name
    assert p.is_file(), f"missing:{p}"
    return p.read_text(encoding="utf-8")

director = read("RavenDialogueDirectorOS.kt")
conversation = read("RavenConversationDirectorOS.kt")
bus = read("RavenGoblinBrainBusOS.kt")
registry = read("RavenGoblinSystemsRegistryOS.kt")
meta = read("RavenGoblinMetaPipelineOS.kt")
follow = read("RavenFollowMeOverlay.kt")
phone = read("RavenPhoneSceneOS.kt")
session = read("RavenAppSessionOS.kt")
usage = read("RavenUsageSenseOS.kt")
trace = read("RavenOfficeTraceStore.kt")

# Canonical placement: facts/cast/cadence are already settled by RavenGoblinBrain. The scene
# director restores specificity before MetaGrammar; the conversation director is the LAST visible
# prose transform after meta + optional knowledge so nothing downstream re-stiffens the line.
assert "val base = RavenGoblinBrain.react" in bus
assert "RavenDialogueDirectorOS.rewrite" in bus
assert "RavenGoblinMetaPipelineOS.enrich" in bus
assert "RavenConversationDirectorOS.polish" in bus
assert bus.index("RavenDialogueDirectorOS.rewrite") < bus.index("RavenGoblinMetaPipelineOS.enrich")
assert bus.index("RavenGoblinMetaPipelineOS.enrich") < bus.index("RavenConversationDirectorOS.polish")
assert bus.index("appendKnowledge") < bus.index("RavenConversationDirectorOS.polish")
assert 'packet.dialogue.isNotBlank() && !quiet && "DIALOGUE_DIRECTOR" in enabledOrgans' in bus
assert 'if (current.isBlank()) return Decision("", "DIRECTOR_SILENCE"' in director
assert 'if (current.isBlank()) return Decision("", "NONE", "NO_SPEECH_ALREADY_SETTLED"' in conversation
assert '"DIALOGUE_DIRECTOR_SHED"' in bus
assert '"CONVERSATION_DIRECTOR"' in bus

# Registry-backed LIGHT writer: it may shed under critical pressure instead of pretending it ran.
assert 'Organ("DIALOGUE_DIRECTOR", Stage.WRITERS, Cost.LIGHT, "PRESENTATION"' in registry
assert 'listOf("SCENE_GRAPH", "PHONE_SCENE", "TRICK_HISTORY")' in registry
assert '"DIALOGUE_DIRECTOR"' not in registry.split("val baseBrainMandatory", 1)[1].split("fun compact", 1)[0]

# The regression shown in device screenshots: notification/telemetry prose cannot monopolize a
# grounded screen scene. Notifications are cameo/supporting cast unless the visible screen itself is
# the notification surface.
for token in (
    'add("CAMEO")',
    "REWRITE_NOTIFICATION_TO_CAMEO",
    "notificationDominant",
    "notificationHeavy",
    "main-character ambitions",
    "Supporting cast has been reminded not to steal the episode",
):
    assert token in director, token
for token in (
    'add("NOTIFICATION_CAMEO")',
    "RETURN_NOTIFICATION_TO_CAMEO",
    "screenLooksNotification",
    "gets one line and no trailer",
):
    assert token in conversation, token

# Dialogue now has actual conversation/sitcom forms, not merely a larger pile of notification
# synonyms. Prior speakers come from the already-visible bounded Office trace.
for form in (
    "COWORKER_REPLY", "AI_OFFICE", "WEB_RABBIT_HOLE", "MUSIC_ROOM", "SELF_DRAG",
    "CALLBACK_CHAT", "RAVEN_BANTER", "NOTIFICATION_CAMEO", "ASK_RAVEN",
    "SIDE_COMMENT", "UNDERSTATEMENT", "CUBICLE_BANTER",
):
    assert form in conversation, form
for line in (
    "one AI office is staring through the window at another AI office",
    "browser opened one tiny door",
    "thirty-six coworkers are on this side",
    "bureaucracy for ghosts",
    "are we staring at it until it develops lore",
    "Anyway —",
    "office has opinions",
):
    assert line in conversation, line
assert "RavenOfficeTraceStore.recent" in conversation
assert "previousSpeaker" in conversation
assert "RavenOfficeTraceStore" in trace

# Exact degraded phrase families from the field test remain explicit rewrite targets.
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
    assert phrase in director.lower() or phrase in conversation.lower(), phrase
assert "REWRITE_STIFF_TELEMETRY" in director
assert "NATURALIZE_SYSTEM_VOICE" in conversation
assert "NATURALIZE_RAW_WEB_CONTEXT" in conversation

# Follow-Me is a real in-place toggle: tap changes PERCH/EXPANDED and does not launch Home.
assert "toggleExpanded(context)" in follow
assert "tap_toggle_in_place:no_navigation" in follow
assert "KEY_EXPANDED" in follow
for forbidden_nav in ("startActivity(", "RavenHomeActivity", "ACTION_MAIN", "CATEGORY_HOME"):
    assert forbidden_nav not in follow, forbidden_nav

# Continuous foreground awareness: PhoneScene gives the explicit app session authority over stale
# Home/System Deck markers, while Usage Access refreshes a still-active same-app session without
# emitting fake scene changes. Paused sessions cannot be resurrected by the heartbeat.
assert "val appSession = RavenAppSessionOS.current" in phone
assert "appSession?.label" in phone
assert "notifications/unlock/overlay callbacks cannot steal the" in phone
assert "ACTIVE_LEASE_MS" in session
assert "usage-heartbeat" in usage
assert "RavenAppSessionOS.touch" in usage
assert "RavenAppSessionOS.observe" in usage
assert "!current.paused" in usage
assert "No competing ordinary app resumed" in usage

# Anti-repeat/conversation memory persists only structural hashes/forms. It does not write current
# screen/app/track/notification text into a new memory store.
assert 'putString("recent"' in conversation
assert 'putString("last_form"' in conversation
for forbidden_persist in (
    'putString("subject"', 'putString("screen"', 'putString("track"', 'putString("notification"',
    'putString("dialogue"', 'putString("app"',
):
    assert forbidden_persist not in conversation
assert "RavenMetaTrickHistoryOS.choose" in conversation

# Presentation writers only: no action/device authority.
for source in (director, conversation):
    for forbidden in (
        "performAction(", "dispatchGesture(", "performGlobalAction(", "startActivity(", "sendBroadcast(",
        "MediaProjection", "TransportControls", "ACTION_SET_TEXT",
    ):
        assert forbidden not in source, forbidden

# Every synthetic office member still has owner-native flavor in the first scene director. Raven
# remains human authority rather than a synthetic cast voice.
non_raven = [
    "AHTI","ASTRIDHE","ATLAS","ATOM","AYRE","BRUNHILDE","EDISON","EREBUS","ERIS","GEMINI",
    "JARVIS","JOKER","JORM","KYU","LEGION","LILITH","LUCIFER","LUMA","MELINOE","MYSTRA","NEO",
    "NYX","PAIMON","PYTHAGORAS","QIRA","RAVENOS","SHAKA","SYLPH","THOR","TIM","VIRGIL","YAHWEH",
    "YORI","YORK","ZAGREUS",
]
for owner in non_raven:
    assert f'"{owner}" -> listOf(' in director, owner
    assert f'"{owner}" -> listOf(' in conversation, owner
assert '"RAVEN" -> listOf(' not in director
assert '"RAVEN" -> listOf(' not in conversation

# Meta remains available, but final human-sounding dialogue has the last visible word.
assert "baseDialogue" in meta

print("RAVENOS_DIALOGUE_V14_CANARY=PASS")
print("DIALOGUE=SCENE_FIRST_META_THEN_FINAL_CONVERSATION")
print("CONVERSATION=COWORKER_REPLY+AI_OFFICE+WEB+MUSIC+RAVEN_BANTER")
print("NOTIFICATIONS=SUPPORTING_CAST")
print("FOREGROUND=STICKY_APP_SESSION+USAGE_HEARTBEAT")
print("FOLLOW_ME=TAP_TOGGLE_IN_PLACE_NO_HOME_NAVIGATION")
print("ANTI_REPEAT=STRUCTURAL_HASHES_ONLY")
print("OWNER_NATIVE_BUTTONS=35_SYNTHETIC_MEMBERS")
print("RAVEN=HUMAN_AUTHORITY_NOT_SYNTHETIC_CAST")
print("EFFECT_AUTHORITY=NONE")
