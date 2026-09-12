package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Local structural search over Machine Kingdom history.
 *
 * The recall index deliberately excludes spoken dialogue, author notes, raw detail, OCR text,
 * screenshots, editable values and notification bodies. Search covers only owner/signal/writer
 * family/scene/canonical motif/episode metadata.
 */
object RavenOfficeRecallOS {
    @JvmStatic
    fun indexReaction(context: Context, packet: RavenReactionPacket) {
        if (packet.dialogue.isBlank()) return
        runCatching {
            val dao = RavenDialogueVaultDatabase.get(context).dao()
            val motif = canonicalMotif(field(packet.detail, "script_motif"))
            val scene = field(packet.detail, "screen_kind").ifBlank { packet.signal }.take(40)
            dao.putRecall(
                RavenRecallMomentEntity(
                    id = "${packet.markerId}-${packet.updatedAt}",
                    owner = packet.owner.take(32),
                    signal = packet.signal.take(32),
                    family = packet.dialogueFamily.take(180),
                    scene = scene,
                    motif = motif.ifBlank { "NONE" },
                    episode = packet.episode.take(40),
                    at = packet.updatedAt,
                ),
            )
            if (dao.recallCount() > 1_100) dao.pruneRecall(1_000)
        }
    }

    @JvmStatic
    fun searchBlocking(context: Context, query: String?): String {
        val q = query.orEmpty().trim()
        if (q.isBlank()) return "OFFICE RECALL: try `recall KYU`, `recall callback`, `recall Smart Capture`, or `recall music`."
        return runCatching {
            val rows = RavenDialogueVaultDatabase.get(context).dao().searchRecall(q, 8)
            if (rows.isEmpty()) return@runCatching "OFFICE RECALL: no structural matches for `$q`."
            buildString {
                append("OFFICE RECALL · ").append(q)
                rows.forEach { row ->
                    append('\n')
                    append(row.owner).append(" · ")
                    append(row.scene).append(" · ")
                    append(row.motif).append(" · ")
                    append(shortFamily(row.family))
                }
            }
        }.getOrElse { "OFFICE RECALL: vault unavailable; try again after the office speaks." }
    }

    @JvmStatic
    fun compact(context: Context): String = runCatching {
        "OFFICE_RECALL=${RavenDialogueVaultDatabase.get(context).dao().recallCount()} structural moments"
    }.getOrElse { "OFFICE_RECALL=UNAVAILABLE" }

    @JvmStatic
    fun clear(context: Context) {
        runCatching { RavenDialogueVaultDatabase.get(context).dao().clearRecall() }
    }

    private fun canonicalMotif(raw: String): String {
        val v = raw.uppercase().replace(Regex("[^A-Z0-9_]+"), "_").trim('_')
        return v.takeIf {
            it in setOf(
                "SELF_AWARE_OFFICE", "SELF_REVIEW_SCREENSHOT", "SOUNDTRACK_MONTAGE", "MUSIC_ROOM",
                "CHATGPT_SELF_DEBUG", "CALLBACK_ABOUT_CALLBACKS", "SELECTING_MEDIA", "SCROLLING_THREAD",
                "UI_SELECTION", "CAMEO_SMART_CAPTURE", "CAMEO_SYSTEM_UI", "CAMEO_NOTIFICATION_SHADE",
                "CAMEO_NOTIFICATION", "CAMEO_KEYBOARD",
            )
        }.orEmpty()
    }

    private fun field(detail: String, name: String): String = detail
        .split('|')
        .firstOrNull { it.startsWith("$name:") }
        ?.substringAfter(':')
        ?.trim()
        .orEmpty()

    private fun shortFamily(family: String): String = family.split('+').firstOrNull().orEmpty().ifBlank { "UNKNOWN" }
}
