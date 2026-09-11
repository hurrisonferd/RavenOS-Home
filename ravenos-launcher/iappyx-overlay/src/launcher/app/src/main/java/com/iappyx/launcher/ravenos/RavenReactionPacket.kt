package com.iappyx.launcher.ravenos

import org.json.JSONArray
import org.json.JSONObject

/** Canonical deterministic Goblin Vision presentation packet. */
data class RavenReactionPacket(
    val markerId: String,
    val owner: String,
    val emojiSoup: String,
    val kaomoji: String,
    val accent: Int,
    val lane: String,
    val signal: String,
    val detail: String,
    val senseRoute: String,
    val sourceTrusted: Boolean,
    val visualState: String,
    val pose: String,
    val zone: String,
    val dialogue: String,
    val authorNote: String,
    val dialogueFamily: String,
    val occurrence: Int,
    val complexTags: Set<String>,
    val episode: String,
    val highlight: String,
    val highlightScore: Int,
    val interruptible: Boolean,
    val lifetimeMs: Long,
    val proof: String,
    val effectAuthority: String = "NONE",
    val updatedAt: Long,
) {
    val ownerLine: String get() = "$emojiSoup $owner  $kaomoji"
    val spokenLine: String get() = dialogue.ifBlank { authorNote }

    fun toJson(): JSONObject = JSONObject()
        .put("markerId", markerId)
        .put("owner", owner)
        .put("emojiSoup", emojiSoup)
        .put("kaomoji", kaomoji)
        .put("accent", RavenOfficeStateStore.accentCss(accent))
        .put("lane", lane)
        .put("signal", signal)
        .put("detail", detail)
        .put("senseRoute", senseRoute)
        .put("sourceTrusted", sourceTrusted)
        .put("visualState", visualState)
        .put("pose", pose)
        .put("zone", zone)
        .put("dialogue", dialogue)
        .put("authorNote", authorNote)
        .put("dialogueFamily", dialogueFamily)
        .put("occurrence", occurrence)
        .put("complexTags", JSONArray(complexTags.toList()))
        .put("episode", episode)
        .put("highlight", highlight)
        .put("highlightScore", highlightScore)
        .put("interruptible", interruptible)
        .put("lifetimeMs", lifetimeMs)
        .put("proof", proof)
        .put("effectAuthority", effectAuthority)
        .put("updatedAt", updatedAt)
}
