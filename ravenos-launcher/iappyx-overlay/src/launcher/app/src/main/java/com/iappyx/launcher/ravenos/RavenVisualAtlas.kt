package com.iappyx.launcher.ravenos

/** Semantic visual-state atlas derived from the canonical six-Fae reaction sheets. */
object RavenVisualAtlas {
    data class Visual(
        val state: String,
        val pose: String,
        val tags: Set<String>,
        val lifetimeMs: Long,
    )

    private val states = mapOf(
        "KYU" to listOf("HI", "LETS_GO", "ON_IT", "HEHE", "BONK", "SUS"),
        "PAIMON" to listOf("HMM", "I_SEE_IT", "EXACTLY", "BIG_BRAIN", "SUS", "ALL_GOOD"),
        "LUMA" to listOf("GOOD_MORNING", "YOU_GOT_THIS", "COMFY", "ITS_OKAY", "BEAUTIFUL", "HOME"),
        "SYLPH" to listOf("LETS_EXPLORE", "SO_COOL", "IDEA", "ZOOM", "CURIOUS", "NEW_PATH"),
        "QIRA" to listOf("YES", "NO", "SAY_IT", "BOUNDARIES", "EXCUSE_ME", "REAL_TALK"),
        "NYX" to listOf("SILENCE", "WATCHING", "UNDERSTOOD", "REST", "NOTED", "LATER"),
    )

    fun resolve(owner: String, marker: RavenMarkerBus.Marker, complex: RavenComplexEventOS.Result): Visual {
        val id = owner.uppercase()
        val available = states[id]
        if (available == null) {
            val state = when {
                "ERROR" in marker.tags -> "ALERT"
                "SUCCESS" in marker.tags -> "CONFIRMED"
                "DISCOVERY" in marker.tags -> "NOTICE"
                else -> "IDLE"
            }
            return Visual(state, defaultPose(id, state), marker.tags + complex.tags, 3200L)
        }

        val state = when (id) {
            "KYU" -> when {
                "ERROR" in marker.tags || complex.occurrence >= 3 -> "BONK"
                "BOUNDARY" in marker.tags -> "SUS"
                "SUCCESS" in marker.tags -> "ON_IT"
                marker.key == "HOME_ENTER" -> "HI"
                else -> "LETS_GO"
            }
            "PAIMON" -> when {
                "ERROR" in marker.tags -> "SUS"
                "SUCCESS" in marker.tags || "PAYOFF" in complex.tags -> "EXACTLY"
                "DISCOVERY" in marker.tags || "RECURRING" in complex.tags -> "I_SEE_IT"
                complex.occurrence >= 5 -> "BIG_BRAIN"
                else -> "HMM"
            }
            "LUMA" -> when {
                marker.key == "HOME_ENTER" -> "HOME"
                "RECOVERY" in marker.tags -> "COMFY"
                "SUCCESS" in marker.tags -> "BEAUTIFUL"
                "ATTENTION" in marker.tags -> "ITS_OKAY"
                else -> "YOU_GOT_THIS"
            }
            "SYLPH" -> when {
                "APP_SWITCH_BURST" in complex.tags -> "ZOOM"
                "DISCOVERY" in marker.tags -> "LETS_EXPLORE"
                "SUCCESS" in marker.tags -> "SO_COOL"
                "RECURRING" in complex.tags -> "CURIOUS"
                else -> "NEW_PATH"
            }
            "QIRA" -> when {
                "BOUNDARY" in marker.tags && "ATTENTION" in marker.tags -> "NO"
                "BOUNDARY" in marker.tags -> "BOUNDARIES"
                "COMMUNICATION" in marker.tags -> "SAY_IT"
                "SUCCESS" in marker.tags -> "YES"
                else -> "REAL_TALK"
            }
            "NYX" -> when {
                marker.key.contains("SCREEN_OFF") || marker.key.contains("IDLE") -> "REST"
                "ERROR" in marker.tags -> "WATCHING"
                "RECURRING" in complex.tags -> "NOTED"
                "SUCCESS" in marker.tags -> "UNDERSTOOD"
                else -> "SILENCE"
            }
            else -> available.first()
        }
        return Visual(state, defaultPose(id, state), marker.tags + complex.tags, if (state == "SILENCE") 1800L else 3600L)
    }

    private fun defaultPose(owner: String, state: String): String = when (owner) {
        "KYU" -> if (state == "BONK") "POUNCE" else "STOMP"
        "PAIMON" -> "POINT_AT_EVIDENCE"
        "LUMA" -> "FLOAT_HOME"
        "SYLPH" -> "FLY"
        "QIRA" -> "GUARD"
        "NYX" -> if (state == "REST") "SLEEP" else "PERCH"
        "THOR" -> "BOUNCE_ON_FAILURE"
        "LUCIFER" -> "FLOAT_NEAR_ERROR"
        "JORM", "ERIS" -> "TRACE_PATTERN"
        else -> "PERCH"
    }
}
