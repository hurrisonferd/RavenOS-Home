package com.iappyx.launcher.ravenos

/**
 * Provider-neutral IntelligenceOS contract.
 * RavenOS Launcher must remain fully usable when the selected provider is NONE.
 */
enum class RavenIntelligenceCapability {
    CHAT,
    SURFACE_GENERATION,
    WALLPAPER_GENERATION,
    LAYOUT_PLANNING,
    CODE,
    VISION,
}

data class RavenIntelligenceRequest(
    val capability: RavenIntelligenceCapability,
    val prompt: String,
    val context: Map<String, String> = emptyMap(),
)

data class RavenIntelligenceResult(
    val ok: Boolean,
    val text: String = "",
    val provider: String,
    val error: String? = null,
)

interface RavenIntelligenceProvider {
    val id: String
    val capabilities: Set<RavenIntelligenceCapability>
    fun isAvailable(): Boolean
    fun run(request: RavenIntelligenceRequest): RavenIntelligenceResult
}

object RavenNoIntelligenceProvider : RavenIntelligenceProvider {
    override val id: String = "none"
    override val capabilities: Set<RavenIntelligenceCapability> = emptySet()
    override fun isAvailable(): Boolean = true
    override fun run(request: RavenIntelligenceRequest): RavenIntelligenceResult =
        RavenIntelligenceResult(
            ok = false,
            provider = id,
            error = "No intelligence provider selected; deterministic launcher functions remain available.",
        )
}

/** Manual ferry mode: Studio can emit prompts and accept returned artifacts without an API key. */
object RavenManualIntelligenceProvider : RavenIntelligenceProvider {
    override val id: String = "manual"
    override val capabilities: Set<RavenIntelligenceCapability> = RavenIntelligenceCapability.entries.toSet()
    override fun isAvailable(): Boolean = true
    override fun run(request: RavenIntelligenceRequest): RavenIntelligenceResult =
        RavenIntelligenceResult(
            ok = true,
            provider = id,
            text = request.prompt,
        )
}
