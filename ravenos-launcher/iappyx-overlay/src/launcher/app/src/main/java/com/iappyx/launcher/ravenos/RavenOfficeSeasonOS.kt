package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Bounded multi-session structural memory for the Machine Kingdom office.
 *
 * Persists only comedy/relationship counters and canonical motif ids. It never persists raw OCR,
 * screenshots, editable values, viewport phrases, or dialogue transcripts.
 */
object RavenOfficeSeasonOS {
    data class Memory(
        val season: Int,
        val episode: Int,
        val episodeInSeason: Int,
        val memberAppearances: Int,
        val memberLines: Int,
        val memberCallbacks: Int,
        val memberMetaLines: Int,
        val memberState: String,
        val pairCount: Int,
        val pairLastEpisode: Int,
        val motifLifetimeCount: Int,
        val motifLastEpisode: Int,
        val motifReturningAcrossSessions: Boolean,
        val lastForm: String,
        val lastMotif: String,
    ) {
        val pairHasHistory: Boolean get() = pairCount >= 2
        val longArc: Boolean get() = memberLines >= 8 || memberCallbacks >= 5 || motifLifetimeCount >= 8
        fun compact(owner: String): String = buildString {
            append("SEASON=").append(season).append('x').append(episodeInSeason)
            append(" ep=").append(episode)
            append(" owner=").append(owner)
            append(" state=").append(memberState)
            append(" lines=").append(memberLines)
            append(" callbacks=").append(memberCallbacks)
            if (pairCount > 0) append(" pair=").append(pairCount)
            if (motifLifetimeCount > 0) append(" motif=").append(motifLifetimeCount)
        }
    }

    private const val PREFS = "ravenos_office_season_v1"
    private const val EPISODES_PER_SEASON = 12
    private var processEpisode: Int? = null

    @Synchronized
    fun snapshot(
        context: Context,
        owner: String,
        secondary: String?,
        canonicalMotif: String,
        mesh: RavenRVResilienceOS.Pulse,
    ): Memory {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val episode = ensureEpisode(p)
        val o = clean(owner)
        val pair = pairKey(owner, secondary)
        val motif = cleanMotif(canonicalMotif)
        val appearances = p.getInt("m_${o}_appear", 0)
        val lines = p.getInt("m_${o}_lines", 0)
        val callbacks = p.getInt("m_${o}_callbacks", 0)
        val meta = p.getInt("m_${o}_meta", 0)
        val pairCount = if (pair.isBlank()) 0 else p.getInt("p_${pair}_count", 0)
        val pairLast = if (pair.isBlank()) 0 else p.getInt("p_${pair}_episode", 0)
        val motifCount = if (motif.isBlank()) 0 else p.getInt("g_${motif}_count", 0)
        val motifLast = if (motif.isBlank()) 0 else p.getInt("g_${motif}_episode", 0)
        val state = performanceState(lines, callbacks, meta, motifCount, mesh)
        val season = ((episode - 1) / EPISODES_PER_SEASON) + 1
        val inSeason = ((episode - 1) % EPISODES_PER_SEASON) + 1
        return Memory(
            season = season,
            episode = episode,
            episodeInSeason = inSeason,
            memberAppearances = appearances,
            memberLines = lines,
            memberCallbacks = callbacks,
            memberMetaLines = meta,
            memberState = state,
            pairCount = pairCount,
            pairLastEpisode = pairLast,
            motifLifetimeCount = motifCount,
            motifLastEpisode = motifLast,
            motifReturningAcrossSessions = motifCount > 0 && motifLast > 0 && motifLast < episode,
            lastForm = p.getString("m_${o}_last_form", "").orEmpty(),
            lastMotif = p.getString("m_${o}_last_motif", "").orEmpty(),
        )
    }

    @Synchronized
    fun markPresence(context: Context, owner: String, sceneChanged: Boolean, rotated: Boolean) {
        if (!sceneChanged && !rotated) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        ensureEpisode(p)
        val o = clean(owner)
        val next = (p.getInt("m_${o}_appear", 0) + 1).coerceAtMost(9999)
        p.edit().putInt("m_${o}_appear", next).apply()
    }

    @Synchronized
    fun markSpoken(
        context: Context,
        owner: String,
        secondary: String?,
        canonicalMotif: String,
        form: String,
        meta: Boolean,
        callback: Boolean,
    ) {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val episode = ensureEpisode(p)
        val o = clean(owner)
        val pair = pairKey(owner, secondary)
        val motif = cleanMotif(canonicalMotif)
        val e = p.edit()
        e.putInt("m_${o}_lines", (p.getInt("m_${o}_lines", 0) + 1).coerceAtMost(9999))
        if (callback) e.putInt("m_${o}_callbacks", (p.getInt("m_${o}_callbacks", 0) + 1).coerceAtMost(9999))
        if (meta) e.putInt("m_${o}_meta", (p.getInt("m_${o}_meta", 0) + 1).coerceAtMost(9999))
        e.putString("m_${o}_last_form", form.take(40))
        if (motif.isNotBlank()) e.putString("m_${o}_last_motif", motif)
        if (pair.isNotBlank()) {
            e.putInt("p_${pair}_count", (p.getInt("p_${pair}_count", 0) + 1).coerceAtMost(9999))
            e.putInt("p_${pair}_episode", episode)
        }
        if (motif.isNotBlank()) {
            e.putInt("g_${motif}_count", (p.getInt("g_${motif}_count", 0) + 1).coerceAtMost(9999))
            e.putInt("g_${motif}_episode", episode)
        }
        e.apply()
    }

    @Synchronized
    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
        processEpisode = null
    }

    fun compact(context: Context): String {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ep = ensureEpisode(p)
        val season = ((ep - 1) / EPISODES_PER_SEASON) + 1
        val inSeason = ((ep - 1) % EPISODES_PER_SEASON) + 1
        return "SERIES=S${season}E${inSeason} lifetime_episode=$ep"
    }

    private fun ensureEpisode(p: android.content.SharedPreferences): Int {
        processEpisode?.let { return it }
        val next = (p.getInt("lifetime_episode", 0) + 1).coerceAtMost(99999)
        p.edit().putInt("lifetime_episode", next).apply()
        processEpisode = next
        return next
    }

    private fun performanceState(lines: Int, callbacks: Int, meta: Int, motifCount: Int, mesh: RavenRVResilienceOS.Pulse): String = when {
        mesh.limpHome -> "CHALLENGED"
        lines >= 20 && (callbacks >= 8 || motifCount >= 13) -> "EVOLVING"
        lines >= 5 || callbacks >= 2 || meta >= 3 -> "GROWING"
        else -> "STABLE"
    }

    private fun pairKey(a: String, b: String?): String {
        if (b.isNullOrBlank()) return ""
        return listOf(clean(a), clean(b)).sorted().joinToString("_").take(56)
    }

    private fun clean(text: String): String = text.uppercase().replace(Regex("[^A-Z0-9]+"), "_").trim('_').take(32)

    /** Only known/canonical structural motif ids are persisted; arbitrary screen text is rejected. */
    private fun cleanMotif(raw: String): String {
        val v = clean(raw)
        return v.takeIf {
            it in setOf(
                "SELF_AWARE_OFFICE", "SELF_REVIEW_SCREENSHOT", "SOUNDTRACK_MONTAGE", "MUSIC_ROOM",
                "CHATGPT_SELF_DEBUG", "CALLBACK_ABOUT_CALLBACKS", "SELECTING_MEDIA", "SCROLLING_THREAD",
                "UI_SELECTION", "CAMEO_SMART_CAPTURE", "CAMEO_SYSTEM_UI", "CAMEO_NOTIFICATION_SHADE",
                "CAMEO_NOTIFICATION", "CAMEO_KEYBOARD",
            )
        }.orEmpty()
    }
}
