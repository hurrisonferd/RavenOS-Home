package com.iappyx.launcher.ravenos

import android.content.Context
import android.content.Intent
import java.io.File

/** Cross-surface mirror of the latest Goblin Vision ReactionPacket. */
object RavenReactionStateStore {
    const val ACTION_CHANGED = "com.ravenos.launcher.goblin.REACTION_CHANGED"
    const val EXTRA_JSON = "json"
    private const val DIR = "ravenos"
    private const val FILE = "goblin_reaction.json"

    fun write(context: Context, packet: RavenReactionPacket) {
        val app = context.applicationContext
        val json = packet.toJson().toString()
        try {
            val dir = File(app.filesDir, DIR).also { it.mkdirs() }
            val target = File(dir, FILE)
            val tmp = File(dir, "$FILE.tmp")
            tmp.writeText(json, Charsets.UTF_8)
            if (!tmp.renameTo(target)) {
                target.writeText(json, Charsets.UTF_8)
                tmp.delete()
            }
        } catch (_: Throwable) {}
        try {
            app.sendBroadcast(Intent(ACTION_CHANGED).setPackage(app.packageName).putExtra(EXTRA_JSON, json))
        } catch (_: Throwable) {}
    }

    fun readJson(context: Context): String? = try {
        val file = File(File(context.filesDir, DIR), FILE)
        if (file.isFile) file.readText(Charsets.UTF_8) else null
    } catch (_: Throwable) { null }
}
