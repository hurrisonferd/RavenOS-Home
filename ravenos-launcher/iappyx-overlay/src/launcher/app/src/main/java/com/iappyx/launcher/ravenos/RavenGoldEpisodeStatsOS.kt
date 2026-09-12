package com.iappyx.launcher.ravenos

/** Process-session Gold episode counters. No content or transcript storage. */
object RavenGoldEpisodeStatsOS {
    data class Snapshot(
        val events: Int,
        val comments: Int,
        val callbacks: Int,
        val silences: Int,
        val windowSwitches: Int,
        val crosstalk: Int,
        val backstagePromotions: Int,
    ) {
        fun compact(): String = "GOLD_STATS events=$events comments=$comments callbacks=$callbacks silence=$silences windows=$windowSwitches cross=$crosstalk backstage=$backstagePromotions"
    }

    private var events = 0
    private var comments = 0
    private var callbacks = 0
    private var silences = 0
    private var windowSwitches = 0
    private var crosstalk = 0
    private var backstagePromotions = 0

    @Synchronized
    fun observe(marker: RavenMarkerBus.Marker) {
        events = (events + 1).coerceAtMost(99999)
        val k = marker.key.uppercase()
        if (k.contains("WINDOW") || k in setOf("FOREGROUND_APP", "FOREGROUND_USAGE", "APP_ENTER")) {
            windowSwitches = (windowSwitches + 1).coerceAtMost(99999)
        }
    }

    @Synchronized
    fun recordDecision(spoken: Boolean, callback: Boolean, gold: RavenGoldSitcomTopologyOS.Beat, backstage: RavenBackstageOS.Cue) {
        if (spoken) comments = (comments + 1).coerceAtMost(99999)
        else silences = (silences + 1).coerceAtMost(99999)
        if (spoken && callback) callbacks = (callbacks + 1).coerceAtMost(99999)
        if (spoken && gold.secondary != null) crosstalk = (crosstalk + 1).coerceAtMost(99999)
        if (spoken && gold.reason == "gold-backstage-crosstalk" && backstage.candidate != null) {
            backstagePromotions = (backstagePromotions + 1).coerceAtMost(99999)
        }
    }

    @Synchronized
    fun snapshot(): Snapshot = Snapshot(events, comments, callbacks, silences, windowSwitches, crosstalk, backstagePromotions)

    @Synchronized
    fun close(): Snapshot {
        val s = snapshot()
        events = 0
        comments = 0
        callbacks = 0
        silences = 0
        windowSwitches = 0
        crosstalk = 0
        backstagePromotions = 0
        return s
    }

    @Synchronized
    fun clear() {
        events = 0
        comments = 0
        callbacks = 0
        silences = 0
        windowSwitches = 0
        crosstalk = 0
        backstagePromotions = 0
    }
}
