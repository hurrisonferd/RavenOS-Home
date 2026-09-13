package com.iappyx.launcher.ravenos

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

/**
 * Broker between Goblin Brain and online knowledge.
 * Local phone evidence always outranks this layer; external knowledge is labeled and cached.
 */
object RavenKnowledgeBrokerOS {
    data class ContextualKnowledge(
        val summary: String,
        val provider: String,
        val source: String,
        val ageMs: Long,
        val scope: String,
    ) {
        val available: Boolean get() = summary.isNotBlank()
    }

    fun observe(context: Context, packet: RavenReactionPacket): ContextualKnowledge {
        val app = context.applicationContext
        val mode = RavenKnowledgePolicyOS.mode(app)
        if (mode == RavenKnowledgePolicyOS.Mode.OFF) return empty()

        ensureSelfRepoRefresh(app)
        if (mode == RavenKnowledgePolicyOS.Mode.CONTEXTUAL) {
            contextualTopic(packet)?.let { requestContextual(app, it) }
        }

        val contextual = RavenKnowledgeStateStore.read(app, "CONTEXTUAL")?.takeIf { it.fresh() }
        val self = RavenKnowledgeStateStore.read(app, "SELF_REPO")?.takeIf { it.fresh() }
        val chosen = contextual?.takeIf { topicMatches(packet, it.topic) } ?: self ?: return empty()
        return ContextualKnowledge(
            summary = chosen.summary,
            provider = chosen.provider,
            source = chosen.source,
            ageMs = (System.currentTimeMillis() - chosen.fetchedAt).coerceAtLeast(0L),
            scope = chosen.scope,
        )
    }

    fun ensureSelfRepoRefresh(context: Context) {
        if (RavenKnowledgePolicyOS.mode(context) == RavenKnowledgePolicyOS.Mode.OFF) return
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val request = PeriodicWorkRequestBuilder<RavenKnowledgeWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInputData(workDataOf(
                RavenKnowledgeWorker.KEY_QUERY to "RavenOS Launcher",
                RavenKnowledgeWorker.KEY_SCOPE to "SELF_REPO",
            ))
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            "ravenos_knowledge_self_repo",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            request,
        )

        val current = RavenKnowledgeStateStore.read(context, "SELF_REPO")
        if (current == null || !current.fresh()) {
            val immediate = OneTimeWorkRequestBuilder<RavenKnowledgeWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(
                    RavenKnowledgeWorker.KEY_QUERY to "RavenOS Launcher",
                    RavenKnowledgeWorker.KEY_SCOPE to "SELF_REPO",
                ))
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                "ravenos_knowledge_self_repo_now",
                ExistingWorkPolicy.KEEP,
                immediate,
            )
        }
    }

    private fun requestContextual(context: Context, topic: String) {
        val current = RavenKnowledgeStateStore.read(context, "CONTEXTUAL")
        if (current != null && current.fresh() && current.topic.equals(topic, ignoreCase = true)) return
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val request = OneTimeWorkRequestBuilder<RavenKnowledgeWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(
                RavenKnowledgeWorker.KEY_QUERY to topic.take(120),
                RavenKnowledgeWorker.KEY_SCOPE to "CONTEXTUAL",
            ))
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "ravenos_knowledge_context_${topic.lowercase().hashCode().toUInt().toString(16)}",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    private fun contextualTopic(packet: RavenReactionPacket): String? {
        if (packet.signal !in setOf("SCREEN_TEXT", "SCREEN_SEMANTIC", "FOREGROUND_APP", "APP_LAUNCH")) return null
        val selected = field(packet.detail, "screen_selected")
        val title = field(packet.detail, "screen_title")
        val app = field(packet.detail, "app")
        return listOf(selected, title, app)
            .firstOrNull { safeTopic(it) }
            ?.trim()
            ?.take(120)
    }

    private fun topicMatches(packet: RavenReactionPacket, topic: String): Boolean {
        if (topic.equals("RavenOS Launcher development", true)) return true
        val haystack = packet.detail.lowercase()
        val words = topic.lowercase().split(Regex("\\s+")).filter { it.length >= 4 }
        return words.take(4).any { it in haystack }
    }

    private fun safeTopic(value: String?): Boolean {
        val v = value.orEmpty().trim()
        if (v.length !in 3..120) return false
        if ('@' in v || Regex("\\b\\d{5,}\\b").containsMatchIn(v)) return false
        if (v.contains("password", true) || v.contains("token", true) || v.contains("secret", true)) return false
        return v.count { it.isLetterOrDigit() } >= 3
    }

    private fun field(detail: String, name: String): String? = Regex("(?:^|\\|)${Regex.escape(name)}:([^|]*)")
        .find(detail)?.groupValues?.getOrNull(1)?.trim()?.takeIf(String::isNotBlank)

    private fun empty() = ContextualKnowledge("", "", "", Long.MAX_VALUE, "")
}
