package com.iappyx.launcher.faeryware

import android.content.Context
import android.content.Intent
import com.iappyx.launcher.LauncherPrefs
import com.iappyx.launcher.PlacementStore
import com.iappyx.launcher.model.CellType
import com.iappyx.launcher.model.HomeLayout
import com.iappyx.launcher.model.Placement
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Transaction boundary for HOUSE-mode cell possession. */
class FaerywarePossessionController(private val context: Context) {
    private val store: PlacementStore get() = PlacementStore(context)
    private val txFile = File(context.filesDir, "faeryware_possessions.json")

    data class Result(val ok: Boolean, val message: String, val targetId: String? = null, val resident: String? = null) {
        fun toJson(): JSONObject = JSONObject().apply {
            put("ok", ok); put("message", message)
            targetId?.let { put("targetId", it) }
            resident?.let { put("resident", it) }
        }
    }

    fun possess(targetId: String, resident: String = "KYU"): Result {
        if (targetId.isBlank()) return Result(false, "target id required")
        val tx = readTransactions()
        if (tx.has(targetId)) return Result(false, "target already possessed; LEAVE or recover first", targetId)

        val layout = store.load()
        val found = findPlacement(layout, targetId) ?: return Result(false, "no such home-grid placement", targetId)
        val pageIndex = found.first
        val original = found.second
        if (original.type != CellType.GENERATED_WIDGET) {
            return Result(false, "first proof only possesses an existing GENERATED_WIDGET", targetId)
        }

        // The restoration anchor is written BEFORE any layout mutation.
        val entry = JSONObject().apply {
            put("targetId", targetId)
            put("resident", resident)
            put("page", pageIndex)
            put("original", original.toJson().toString())
            put("acquiredAt", System.currentTimeMillis())
        }
        tx.put(targetId, entry)
        writeTransactions(tx)

        val replacement = original.copy(
            type = CellType.GENERATED_WIDGET,
            packageName = null,
            activityName = null,
            appWidgetId = null,
            generatedWidgetId = RESIDENT_WIDGET_ID,
            generatedWidgetAsset = RESIDENT_WIDGET_ASSET,
            folderName = null,
            folderItems = mutableListOf(),
        )
        layout.pages[pageIndex].placements.removeAll { it.id == targetId }
        layout.pages[pageIndex].placements.add(replacement)
        store.save(layout)
        broadcastChanged()

        val observed = findPlacement(store.load(), targetId)?.second
        val landed = observed?.type == CellType.GENERATED_WIDGET &&
            observed.generatedWidgetId == RESIDENT_WIDGET_ID &&
            observed.generatedWidgetAsset == RESIDENT_WIDGET_ASSET
        return if (landed) {
            Result(true, "resident surface possessed; restoration anchor retained", targetId, resident)
        } else {
            Result(false, "layout mutation was not observed; restoration anchor retained for recovery", targetId, resident)
        }
    }

    fun leave(targetId: String): Result {
        if (targetId.isBlank()) return Result(false, "target id required")
        val tx = readTransactions()
        val entry = tx.optJSONObject(targetId) ?: return Result(false, "no possession anchor for target", targetId)
        val pageIndex = entry.optInt("page", -1)
        val originalJson = entry.optString("original", "")
        val resident = entry.optString("resident", "")
        val original = try { Placement.fromJson(JSONObject(originalJson)) } catch (_: Throwable) { null }
            ?: return Result(false, "stored restoration payload is invalid; anchor retained", targetId, resident)

        val layout = store.load()
        if (pageIndex !in layout.pages.indices) return Result(false, "original page no longer exists; anchor retained", targetId, resident)

        // Work in memory first. Do not save anything until the original rectangle is proven free.
        for (page in layout.pages) page.placements.removeAll { it.id == targetId }
        for (dock in layout.dockPages) dock.removeAll { it.id == targetId }
        if (!fitsOnPage(layout, pageIndex, original)) {
            return Result(false, "original cell rectangle is occupied; anchor retained and layout left untouched", targetId, resident)
        }
        layout.pages[pageIndex].placements.add(original)
        store.save(layout)
        broadcastChanged()

        val observed = findPlacement(store.load(), targetId)?.second
        val exact = observed != null && observed.toJson().toString() == originalJson
        if (!exact) return Result(false, "exact restoration was not observed; anchor retained", targetId, resident)

        tx.remove(targetId)
        writeTransactions(tx)
        return Result(true, "original placement restored exactly; possession anchor cleared", targetId, resident)
    }

    fun status(): JSONObject = JSONObject().apply {
        put("ok", true)
        put("residentWidgetId", RESIDENT_WIDGET_ID)
        put("transactions", readTransactions())
    }

    private fun findPlacement(layout: HomeLayout, id: String): Pair<Int, Placement>? {
        for ((pageIndex, page) in layout.pages.withIndex()) {
            page.placements.firstOrNull { it.id == id }?.let { return pageIndex to it }
        }
        return null
    }

    private fun fitsOnPage(layout: HomeLayout, pageIndex: Int, candidate: Placement): Boolean {
        val page = layout.pages[pageIndex]
        if (candidate.row < 0 || candidate.col < 0 || candidate.row + candidate.hSpan > layout.rows || candidate.col + candidate.wSpan > layout.cols) return false
        for (existing in page.placements) {
            if (existing.id == candidate.id) continue
            val overlap = candidate.row < existing.row + existing.hSpan &&
                candidate.row + candidate.hSpan > existing.row &&
                candidate.col < existing.col + existing.wSpan &&
                candidate.col + candidate.wSpan > existing.col
            if (overlap) return false
        }
        return true
    }

    /** Corruption is a hard stop; never reinterpret a broken restoration journal as empty. */
    private fun readTransactions(): JSONObject {
        if (!txFile.exists()) return JSONObject()
        return try { JSONObject(txFile.readText(Charsets.UTF_8)) }
        catch (t: Throwable) { throw IllegalStateException("possession journal unreadable", t) }
    }

    private fun writeTransactions(obj: JSONObject) {
        val tmp = File(context.filesDir, "faeryware_possessions.json.tmp")
        tmp.writeText(obj.toString(), Charsets.UTF_8)
        try {
            Files.move(tmp.toPath(), txFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: Throwable) {
            Files.move(tmp.toPath(), txFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun broadcastChanged() {
        context.sendBroadcast(Intent(LauncherPrefs.CLIPPINGS_CHANGED_ACTION).setPackage(context.packageName))
    }

    companion object {
        const val RESIDENT_WIDGET_ID = "faeryware_resident"
        const val RESIDENT_WIDGET_ASSET = "widgets/faeryware_resident.html"
    }
}
