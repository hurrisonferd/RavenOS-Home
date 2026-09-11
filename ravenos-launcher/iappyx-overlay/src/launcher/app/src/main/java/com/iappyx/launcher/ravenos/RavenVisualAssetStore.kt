package com.iappyx.launcher.ravenos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.iappyx.launcher.R

/** Runtime cropper for Raven's canonical 6x6 Digi-Fae reaction atlas. */
object RavenVisualAssetStore {
    private const val COLS = 6
    private const val ROWS = 6
    @Volatile private var atlas: Bitmap? = null

    private val ownerRow = mapOf(
        "KYU" to 0,
        "PAIMON" to 1,
        "LUMA" to 2,
        "NYX" to 3,
        "SYLPH" to 4,
        "QIRA" to 5,
    )

    private val stateCol = mapOf(
        "KYU" to listOf("HI", "LETS_GO", "ON_IT", "HEHE", "BONK", "SUS"),
        "PAIMON" to listOf("HMM", "I_SEE_IT", "EXACTLY", "BIG_BRAIN", "SUS", "ALL_GOOD"),
        "LUMA" to listOf("GOOD_MORNING", "YOU_GOT_THIS", "COMFY", "ITS_OKAY", "BEAUTIFUL", "HOME"),
        "NYX" to listOf("SILENCE", "WATCHING", "UNDERSTOOD", "REST", "NOTED", "LATER"),
        "SYLPH" to listOf("LETS_EXPLORE", "SO_COOL", "IDEA", "ZOOM", "CURIOUS", "NEW_PATH"),
        "QIRA" to listOf("YES", "NO", "SAY_IT", "BOUNDARIES", "EXCUSE_ME", "REAL_TALK"),
    )

    fun reaction(context: Context, owner: String, state: String): Bitmap? {
        val id = owner.uppercase()
        val row = ownerRow[id] ?: return null
        val states = stateCol[id] ?: return null
        val col = states.indexOf(state.uppercase())
        if (col < 0) return null
        val source = atlas ?: synchronized(this) {
            atlas ?: BitmapFactory.decodeResource(context.resources, R.drawable.raven_fae_reaction_atlas)?.also { atlas = it }
        } ?: return null
        val cellW = source.width / COLS
        val cellH = source.height / ROWS
        val x = col * cellW
        val y = row * cellH
        return try { Bitmap.createBitmap(source, x, y, cellW, cellH) } catch (_: Throwable) { null }
    }

    fun hasVisual(owner: String): Boolean = owner.uppercase() in ownerRow
}
