package com.iappyx.launcher.ravenos

/** RavenOS Launcher common object model. A resident may inhabit a surface; it is not the surface. */
enum class RavenSurfaceType {
    APP,
    SHORTCUT,
    FOLDER,
    ANDROID_WIDGET,
    NATIVE_WIDGET,
    WEB_SURFACE,
    CONTROL,
    MEDIA,
    STACK,
    CLIPPING,
    PORTAL,
    RESIDENT,
    GENERATED,
}

enum class RavenCapability {
    STORAGE,
    NETWORK,
    MEDIA_READ,
    MEDIA_CONTROL,
    AUDIO_CONTROL,
    NOTIFICATION_METADATA,
    NOTIFICATION_CONTENT,
    CLIPBOARD,
    LOCATION,
    CAMERA,
    MICROPHONE,
    CONTACTS,
    CALENDAR,
    BLUETOOTH,
    LOCAL_NETWORK,
    VIBRATION,
    TTS,
}

data class RavenSurfaceEnvelope(
    val id: String,
    val type: RavenSurfaceType,
    val room: String,
    val owner: String? = null,
    val resident: String? = null,
    val capabilities: Set<RavenCapability> = emptySet(),
    val source: String = "native",
    val provenance: String = "ravenos",
    val persistent: Boolean = true,
)
