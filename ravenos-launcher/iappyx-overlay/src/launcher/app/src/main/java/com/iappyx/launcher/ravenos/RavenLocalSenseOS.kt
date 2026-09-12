package com.iappyx.launcher.ravenos

/** Known deterministic phone state outranks optional interpretation. */
object RavenLocalSenseOS {
    enum class Route {
        LAUNCHER_NATIVE,
        ANDROID_CALLBACK,
        MEDIA_SESSION,
        NOTIFICATION_SOURCE,
        TRIGGER_PACK,
        LOCAL_OCR,
        ACCESSIBILITY_SEMANTIC,
        LOCAL_VISION,
        DELIBERATION_ELIGIBLE,
        SILENCE,
    }

    data class Decision(
        val route: Route,
        val trusted: Boolean,
        val effectAuthority: String = "NONE",
        val reason: String,
    )

    fun resolve(marker: RavenMarkerBus.Marker): Decision {
        val key = marker.key
        return when {
            key == "HOME_ENTER" || key == "ROOM_CHANGED" || key == "APP_UNIVERSE_OPENED" || key == "SEARCH_OPENED" || key == "SYSTEM_DECK_OPENED" ->
                Decision(Route.LAUNCHER_NATIVE, true, reason = "launcher-native state")
            key == "NOTIFICATION_POSTED" || key == "NOTIFICATION_REMOVED" ->
                Decision(Route.NOTIFICATION_SOURCE, true, reason = "notification-listener metadata/ranking")
            key == "SCREEN_VISUAL" ->
                Decision(Route.LOCAL_VISION, true, reason = "owner-armed MediaProjection visual delta; raw frame not persisted")
            key == "SCREEN_TEXT" ->
                Decision(Route.LOCAL_OCR, true, reason = "owner-armed local OCR from transient MediaProjection frame")
            key == "SCREEN_SEMANTIC" ->
                Decision(Route.ACCESSIBILITY_SEMANTIC, true, reason = "owner-armed visible Accessibility semantics; editable/password values excluded")
            key.startsWith("MEDIA") ->
                Decision(Route.MEDIA_SESSION, true, reason = "media semantic state")
            key.startsWith("BATTERY") || key.startsWith("POWER") || key == "DEVICE" || key == "NIGHT" || key == "WINDOW_CHANGE" ->
                Decision(Route.ANDROID_CALLBACK, true, reason = "android lifecycle/window callback")
            key == "APP_ENTER" ->
                Decision(Route.TRIGGER_PACK, true, reason = "package/app trigger")
            marker.source.equals("LOCAL_OCR", true) ->
                Decision(Route.LOCAL_OCR, true, reason = "explicit local OCR derived evidence")
            marker.source.equals("LOCAL_VISION", true) ->
                Decision(Route.LOCAL_VISION, true, reason = "explicit local vision derived evidence")
            marker.salience >= 7 ->
                Decision(Route.DELIBERATION_ELIGIBLE, false, reason = "ambiguous high-salience marker; eligibility is not execution")
            else -> Decision(Route.SILENCE, false, reason = "no stronger trusted route")
        }
    }
}
