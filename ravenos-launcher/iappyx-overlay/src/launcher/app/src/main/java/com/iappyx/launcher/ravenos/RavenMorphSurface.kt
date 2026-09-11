package com.iappyx.launcher.ravenos

import android.app.Activity

/**
 * Haunt-requirement compatibility surface.
 *
 * This used to render a floating 230dp "Recovery Field" card over Raven Home. Device testing
 * showed that it duplicated the resident projection and obscured useful launcher controls.
 * Requirements now influence the canonical Office state instead; Home/Office Bar/Goblin Vision
 * project that one state. The object remains as a no-op compatibility seam for existing callers.
 */
object RavenMorphSurface {
    fun attach(activity: Activity) = Unit
    fun refresh(activity: Activity) = Unit
    fun hide() = Unit
}
