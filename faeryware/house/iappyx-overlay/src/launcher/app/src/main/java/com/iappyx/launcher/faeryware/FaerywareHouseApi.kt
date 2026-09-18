package com.iappyx.launcher.faeryware

import android.content.Context
import com.iappyx.launcher.remoteedit.server.JsonResponse
import com.iappyx.launcher.remoteedit.server.MicroHttpServer

/** Authenticated Remote Edit surface for first-proof possess/leave actions. */
class FaerywareHouseApi(context: Context) {
    private val controller = FaerywarePossessionController(context)

    fun status(ex: MicroHttpServer.Exchange) {
        try { JsonResponse.ok(ex, controller.status()) }
        catch (t: Throwable) { JsonResponse.error(ex, 500, "faeryware status failed: ${t.javaClass.simpleName}") }
    }

    fun possess(ex: MicroHttpServer.Exchange) {
        val body = JsonResponse.readJsonObject(ex) ?: return JsonResponse.error(ex, 400, "no body")
        val id = body.optString("id").trim()
        val resident = body.optString("resident", "KYU").trim().ifBlank { "KYU" }
        try { respond(ex, controller.possess(id, resident)) }
        catch (t: Throwable) { JsonResponse.error(ex, 500, "faeryware possess failed: ${t.javaClass.simpleName}") }
    }

    fun leave(ex: MicroHttpServer.Exchange) {
        val body = JsonResponse.readJsonObject(ex) ?: return JsonResponse.error(ex, 400, "no body")
        val id = body.optString("id").trim()
        try { respond(ex, controller.leave(id)) }
        catch (t: Throwable) { JsonResponse.error(ex, 500, "faeryware leave failed: ${t.javaClass.simpleName}") }
    }

    private fun respond(ex: MicroHttpServer.Exchange, result: FaerywarePossessionController.Result) {
        if (result.ok) JsonResponse.ok(ex, result.toJson())
        else JsonResponse.error(ex, 400, result.message)
    }
}
