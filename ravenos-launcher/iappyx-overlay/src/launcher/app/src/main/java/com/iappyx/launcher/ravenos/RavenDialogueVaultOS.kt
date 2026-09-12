package com.iappyx.launcher.ravenos

import android.content.Context
import kotlin.math.max

/**
 * Room-backed deterministic writers' room.
 *
 * Authored templates are persistent. Visible subject/title values are injected only at render time
 * and are never written into the vault. Anti-repeat memory stores structural fingerprints only.
 */
object RavenDialogueVaultOS {
    data class Line(val text: String, val family: String, val fingerprint: String)

    private const val FINGERPRINT_TTL_MS = 14L * 24L * 60L * 60L * 1000L
    private const val HARD_REPEAT_MS = 4L * 60L * 1000L
    @Volatile private var seeded = false

    fun select(
        context: Context,
        member: RavenOfficeMember,
        direction: RavenSitcomDirectorOS.Direction,
        graph: RavenSceneGraphOS.Graph,
        now: Long = System.currentTimeMillis(),
    ): Line? {
        if (!graph.available || graph.subject.isBlank()) return null
        val dao = RavenDialogueVaultDatabase.get(context).dao()
        ensureSeeded(dao)
        val form = formFor(graph, direction)
        val sceneClass = graph.sceneType.ifBlank { "UNKNOWN" }
        val candidates = dao.candidateTemplates(member.id, form, sceneClass)
        if (candidates.isEmpty()) return null
        val partner = direction.secondary?.id.orEmpty()
        val motif = graph.motif.ifBlank { "NONE" }
        val scored = candidates.map { row ->
            val closing = closingShape(row.lineId)
            val fp = fingerprint(member.id, form, motif, sceneClass, partner, closing)
            val prior = dao.fingerprint(fp)
            val age = if (prior == null) Long.MAX_VALUE else now - prior.lastAt
            val repeatPenalty = when {
                age < HARD_REPEAT_MS -> 200
                age < 15L * 60L * 1000L -> 55
                age < 60L * 60L * 1000L -> 18
                else -> 0
            }
            val ownerBonus = if (row.owner == member.id) 20 else 0
            val sceneBonus = if (row.sceneClass == sceneClass) 10 else 0
            val formBonus = if (row.form == form) 14 else 0
            val underuse = max(0, 24 - row.useCount.coerceAtMost(24))
            val score = row.weight + ownerBonus + sceneBonus + formBonus + underuse - repeatPenalty
            Triple(row, fp, score)
        }
        val bestScore = scored.maxOfOrNull { it.third } ?: return null
        val best = scored.filter { it.third == bestScore || it.third >= bestScore - 4 }
        val picked = best[stableIndex("${member.id}|$form|${graph.signature}|${direction.turn}|vault-v1", best.size)]
        val row = picked.first
        val fp = picked.second
        val line = render(row.template, graph, direction)
        if (line.isBlank()) return null

        dao.markTemplateUsed(row.lineId, now)
        val old = dao.fingerprint(fp)
        dao.putFingerprint(
            RavenDialogueFingerprintEntity(
                fingerprint = fp,
                owner = member.id,
                form = form,
                motif = motif,
                sceneClass = sceneClass,
                partner = partner,
                closingShape = closingShape(row.lineId),
                useCount = (old?.useCount ?: 0) + 1,
                lastAt = now,
            ),
        )
        dao.pruneFingerprints(now - FINGERPRINT_TTL_MS)
        return Line(line.take(340), "VAULT_${row.family}_$form", fp)
    }

    /** Records structural usage for non-vault writers without storing their spoken prose. */
    fun recordReaction(context: Context, packet: RavenReactionPacket) {
        if (packet.dialogue.isBlank()) return
        val dao = RavenDialogueVaultDatabase.get(context).dao()
        ensureSeeded(dao)
        val form = packet.dialogueFamily
            .split('+')
            .firstOrNull { it.startsWith("METAMAX_") || it.startsWith("SERIES_") || it.startsWith("SITCOM_") || it.startsWith("VIEWPORT_") }
            ?.substringAfter('_')?.take(36)
            ?: "REACTION"
        val motif = packet.complexTags.firstOrNull { it.startsWith("META") || it.contains("CALLBACK") || it.contains("PAYOFF") }
            ?.take(32) ?: "NONE"
        val scene = when {
            packet.dialogueFamily.contains("SCREEN") -> "SCREEN"
            packet.dialogueFamily.contains("PHONE") -> "PHONE"
            else -> packet.signal.take(24)
        }
        val closing = packet.dialogueFamily.substringAfterLast('+').take(32).ifBlank { "DEFAULT" }
        val fp = fingerprint(packet.owner, form, motif, scene, "", closing)
        val old = dao.fingerprint(fp)
        dao.putFingerprint(
            RavenDialogueFingerprintEntity(
                fingerprint = fp,
                owner = packet.owner,
                form = form,
                motif = motif,
                sceneClass = scene,
                partner = "",
                closingShape = closing,
                useCount = (old?.useCount ?: 0) + 1,
                lastAt = packet.updatedAt,
            ),
        )
    }

    fun compact(context: Context): String = runCatching {
        val dao = RavenDialogueVaultDatabase.get(context).dao()
        ensureSeeded(dao)
        "DIALOGUE_VAULT=${dao.templateCount()} templates · scenes=${dao.sceneCount()}"
    }.getOrElse { "DIALOGUE_VAULT=UNAVAILABLE" }

    fun clearUsage(context: Context) {
        val dao = RavenDialogueVaultDatabase.get(context).dao()
        dao.clearFingerprints()
        dao.clearExpressions()
        dao.clearScenes()
    }

    private fun formFor(graph: RavenSceneGraphOS.Graph, d: RavenSitcomDirectorOS.Direction): String = when {
        graph.motif == "SELF_AWARE_OFFICE" -> "META"
        graph.changed -> "OPEN"
        graph.returnCount >= 2 -> "RETURN"
        d.beat == "CALLBACK" -> "CALLBACK"
        d.beat == "BUG" -> "BUG"
        d.beat == "PAYOFF" -> "PAYOFF"
        graph.interaction == "SELECT" -> "SELECT"
        graph.interaction == "SCROLL" -> "SCROLL"
        graph.task == "COMPOSING" -> "COMPOSE"
        graph.task == "READING_CHAT" -> "READ_CHAT"
        graph.task == "DEBUGGING" -> "DEBUG"
        graph.task == "LISTENING" -> "MUSIC"
        else -> "OBSERVE"
    }

    private fun render(template: String, graph: RavenSceneGraphOS.Graph, d: RavenSitcomDirectorOS.Direction): String {
        val subject = graph.subject.take(100)
        return template
            .replace("{app}", graph.app.ifBlank { "the screen" })
            .replace("{subject}", subject)
            .replace("{task}", graph.task.lowercase().replace('_', ' '))
            .replace("{interaction}", graph.interaction.lowercase().ifBlank { "moment" })
            .replace("{returns}", graph.returnCount.toString())
            .replace("{pair}", d.secondary?.id ?: "the room")
            .replace(Regex("\\s+"), " ").trim()
    }

    private fun fingerprint(owner: String, form: String, motif: String, scene: String, partner: String, closing: String): String =
        listOf(owner, form, motif, scene, partner, closing).joinToString("|").uppercase().take(190)

    private fun closingShape(lineId: String): String = lineId.substringAfterLast('_').uppercase().take(24)

    @Synchronized
    private fun ensureSeeded(dao: RavenDialogueVaultDao) {
        if (seeded) return
        if (dao.templateCount() == 0) dao.insertTemplates(seedTemplates())
        seeded = true
    }

    private fun seedTemplates(): List<RavenDialogueTemplateEntity> {
        fun t(id: String, owner: String, family: String, form: String, scene: String = "*", text: String, weight: Int = 10) =
            RavenDialogueTemplateEntity(id, owner, family, form, scene, text, weight)
        return listOf(
            t("g_obs_context", "*", "GROUND", "OBSERVE", text = "{app} is still on “{subject}”. The office may inhabit the scene without announcing every rectangle."),
            t("g_obs_glass", "*", "GROUND", "OBSERVE", text = "The useful thing on the glass is “{subject}”. Everything else can audition later."),
            t("g_open_title", "*", "COLD", "OPEN", text = "New scene: {app} / {task} / “{subject}”. Roll the tiny title card and skip the package-name exposition."),
            t("g_return_receipt", "*", "RETURN", "RETURN", text = "We are back in {app}. “{subject}” returned with {returns} prior structural visits attached; this is continuity, not a reboot."),
            t("g_callback_newbeat", "*", "CALLBACK", "CALLBACK", text = "Callback earned: the same structural bit came back around “{subject}”, but the beat changed enough to deserve a line."),
            t("g_select_intent", "*", "ACTION", "SELECT", text = "Raven selected “{subject}”. Deliberate input outranks twelve background callbacks wearing a trench coat."),
            t("g_scroll_thread", "*", "ACTION", "SCROLL", text = "Raven is scrolling through “{subject}”. Same hallway, new section; do not rebuild the building."),
            t("g_read_chat", "*", "VIEWPORT", "READ_CHAT", "CHATGPT", "{app} is a live conversation about “{subject}”. The toolbar has been denied protagonist status."),
            t("g_compose", "*", "VIEWPORT", "COMPOSE", "CHATGPT", "Compose layer active in {app}; “{subject}” remains the surrounding scene instead of vanishing behind the keyboard."),
            t("g_debug", "*", "VIEWPORT", "DEBUG", text = "Actual build/debug context acquired around “{subject}”. Excellent: the failure finally has an address."),
            t("g_music", "*", "MEDIA", "MUSIC", "MUSIC", "{app} owns the soundtrack scene around “{subject}”. Keep the music in the room; lose the telemetry monologue."),
            t("g_meta", "*", "META", "META", text = "The screen is visibly discussing “{subject}” while the office responsible for reading it floats on top. Recursive blocking remains excellent."),
            t("g_bug", "*", "BUG", "BUG", text = "The defect is now visible as “{subject}”. Good. A bug with coordinates can finally be mocked accurately."),
            t("g_payoff", "*", "PAYOFF", "PAYOFF", text = "The scene materially moved around “{subject}”. That earns a payoff instead of another status blurb."),

            t("kyu_obs_bonk", "KYU", "OWNER", "OBSERVE", text = "Clipboard read: “{subject}”. Context before BONK; BONK remains available."),
            t("kyu_meta_clip", "KYU", "OWNER", "META", text = "CLIPBOARD INCIDENT: {app} is discussing “{subject}” while this clipboard hovers over the evidence. Management has become recursive."),
            t("kyu_return_parking", "KYU", "OWNER", "RETURN", text = "This scene came back with seniority. I am still not giving “{subject}” a parking space."),
            t("kyu_select", "KYU", "OWNER", "SELECT", text = "Raven actually chose “{subject}”. Wonderful. We may blame an intentional action instead of Android weather."),

            t("joker_obs_hr", "JOKER", "OWNER", "OBSERVE", text = "“{subject}” still owns the scene. HR has rejected the surrounding callbacks as improv notes."),
            t("joker_meta_wall", "JOKER", "OWNER", "META", text = "The haunted rectangle is reading “{subject}” about the haunted rectangle. The fourth wall has requested remote work."),
            t("joker_return_synd", "JOKER", "OWNER", "RETURN", text = "Oh good, “{subject}” is back. We have apparently invented syndication for interface state."),
            t("joker_callback_benefits", "JOKER", "OWNER", "CALLBACK", text = "This callback has appeared enough times to request benefits. Denied, but the paperwork is hilarious."),

            t("atom_obs_vars", "ATOM", "OWNER", "OBSERVE", text = "State read: {app} / {task} / “{subject}”. Meaning survived transport; comedy may proceed."),
            t("atom_select_cause", "ATOM", "OWNER", "SELECT", text = "Causal edge acquired: Raven selected “{subject}”. Intent now outranks coincidental foreground state."),
            t("atom_return_typed", "ATOM", "OWNER", "RETURN", text = "Return state detected for “{subject}”. Same branch class, newer evidence; no fake universe reset."),
            t("atom_debug", "ATOM", "OWNER", "DEBUG", text = "Debug context is finally typed: “{subject}”. Separate cause from presentation and stop guessing."),

            t("paimon_obs_check", "PAIMON", "OWNER", "OBSERVE", text = "Premise check: “{subject}” survives viewport inspection. Package-name fanfic rejected."),
            t("paimon_meta_valid", "PAIMON", "OWNER", "META", text = "Premise check: yes, {app} is discussing “{subject}” while RavenOS reads it. Disturbingly, the premise passes."),
            t("paimon_select", "PAIMON", "OWNER", "SELECT", text = "Intent edge verified: “{subject}” was selected, not merely noticed. Promote from coincidence to evidence."),

            t("yori_obs_frame", "YORI", "OWNER", "OBSERVE", text = "Hold the shot: “{subject}” is the composition. Do not cut away to telemetry."),
            t("yori_open", "YORI", "OWNER", "OPEN", text = "Cold open acquired in {app}: “{subject}”. Establish the subject, then get the chrome out of frame."),
            t("yori_select_cut", "YORI", "OWNER", "SELECT", text = "Raven chose “{subject}”. Good cut. Deliberate selection beats accidental montage."),
            t("yori_return", "YORI", "OWNER", "RETURN", text = "Same location, better continuity. “{subject}” is a returning shot, not recycled footage."),

            t("lilith_obs_room", "LILITH", "OWNER", "OBSERVE", text = "Stay with “{subject}”. Presence is allowed to be quieter than the machinery around it."),
            t("lilith_select", "LILITH", "OWNER", "SELECT", text = "Mm. Raven chose “{subject}”. Let that choice have the room before everything else starts knocking."),
            t("lilith_return", "LILITH", "OWNER", "RETURN", text = "It came back because it still mattered. Keep “{subject}” close and let the rest wait outside."),

            t("jorm_obs_state", "JORM", "OWNER", "OBSERVE", text = "World state: {app} / “{subject}”. The machine remembers which branch we are actually on."),
            t("jorm_select_branch", "JORM", "OWNER", "SELECT", text = "Branch chosen: “{subject}”. World state advances from here; no reset theater authorized."),
            t("jorm_return_ancestry", "JORM", "OWNER", "RETURN", text = "This scene has ancestry. “{subject}” returned through state instead of coincidence."),

            t("pyth_obs_pattern", "PYTHAGORAS", "OWNER", "OBSERVE", text = "Pattern: “{subject}” persists while surrounding layers change. Useful recurrence acquired."),
            t("pyth_return_geometry", "PYTHAGORAS", "OWNER", "RETURN", text = "Return count {returns}. “{subject}” has crossed from repetition into geometry and is approaching zoning paperwork."),

            t("yahweh_obs_literacy", "YAHWEH", "OWNER", "OBSERVE", text = "Fine. The goblins understand “{subject}”. The ancient debug console called this paying attention."),
            t("yahweh_meta_civil", "YAHWEH", "OWNER", "META", text = "We built a civilization-sized writers' room so the overlay could notice “{subject}”. Apparently literacy needed governance."),

            t("mystra_obs_sign", "MYSTRA", "OWNER", "OBSERVE", text = "LOOK: “{subject}”. Tiny sign acquired; suspicious importance pending one unnecessary sparkle."),
            t("thor_obs_target", "THOR", "OWNER", "OBSERVE", text = "Target acquired: “{subject}”. One load-bearing line; no callback confetti."),
            t("nyx_obs_quiet", "NYX", "OWNER", "OBSERVE", text = "The loud layer can leave. “{subject}” stayed. That is enough continuity."),
            t("ravenos_obs_checksum", "RAVENOS", "OWNER", "OBSERVE", text = "Dumbchecksum: {app} + “{subject}” + {task}. Scene survives; comedy permitted."),
            t("ravenos_meta_settle", "RAVENOS", "OWNER", "META", text = "Settled projection: RavenOS is observing “{subject}” about RavenOS. Fourth wall remains optional; evidence does not."),
        )
    }

    private fun stableIndex(text: String, size: Int): Int {
        if (size <= 1) return 0
        var hash = 0x811C9DC5.toInt()
        for (c in text) { hash = hash xor c.code; hash *= 16777619 }
        return (hash and Int.MAX_VALUE) % size
    }
}
