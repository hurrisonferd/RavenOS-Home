package com.iappyx.launcher.ravenos

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

/** Background network worker for bounded online knowledge refresh. */
class RavenKnowledgeWorker(
    context: Context,
    params: WorkerParameters,
) : Worker(context, params) {
    override fun doWork(): Result {
        val query = inputData.getString(KEY_QUERY).orEmpty()
        val scope = inputData.getString(KEY_SCOPE).orEmpty().ifBlank { "CONTEXTUAL" }
        if (query.isBlank() && scope != "SELF_REPO") return Result.failure()

        val snapshot = RavenKnowledgeProviderOS.fetch(query, scope)
            ?: return Result.retry()
        RavenKnowledgeStateStore.write(applicationContext, snapshot)
        RavenWidgetKnowledgeModule.broadcast(applicationContext, snapshot)
        return Result.success()
    }

    companion object {
        const val KEY_QUERY = "query"
        const val KEY_SCOPE = "scope"
    }
}
