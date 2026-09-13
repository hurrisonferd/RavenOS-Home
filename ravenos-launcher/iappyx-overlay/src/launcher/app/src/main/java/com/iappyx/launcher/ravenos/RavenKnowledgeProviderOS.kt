package com.iappyx.launcher.ravenos

import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

/** No-key online providers used by KnowledgeOS. */
object RavenKnowledgeProviderOS {
    private val client = OkHttpClient.Builder()
        .callTimeout(7, TimeUnit.SECONDS)
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    fun fetch(query: String, scope: String): RavenKnowledgeStateStore.Snapshot? = when (scope.uppercase()) {
        "SELF_REPO" -> fetchSelfRepo()
        else -> fetchWikipedia(query)
    }

    private fun fetchSelfRepo(): RavenKnowledgeStateStore.Snapshot? {
        val url = HttpUrl.Builder()
            .scheme("https")
            .host("api.github.com")
            .addPathSegment("repos")
            .addPathSegment("hurrisonferd")
            .addPathSegment("RavenOS-Home")
            .addPathSegment("commits")
            .addQueryParameter("sha", "ravenos-launcher-whole-phone")
            .addQueryParameter("per_page", "1")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "RavenOS-Launcher-KnowledgeOS")
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string().orEmpty()
                val item = JSONArray(body).optJSONObject(0) ?: return null
                val sha = item.optString("sha").take(8)
                val message = item.optJSONObject("commit")?.optString("message")
                    ?.lineSequence()?.firstOrNull().orEmpty().take(180)
                if (sha.isBlank() || message.isBlank()) return null
                RavenKnowledgeStateStore.Snapshot(
                    topic = "RavenOS Launcher development",
                    summary = "Public RavenOS launcher branch $sha: $message",
                    provider = "GITHUB_PUBLIC",
                    source = "github.com/hurrisonferd/RavenOS-Home",
                    url = item.optString("html_url"),
                    scope = "SELF_REPO",
                    fetchedAt = System.currentTimeMillis(),
                )
            }
        }.getOrNull()
    }

    private fun fetchWikipedia(query: String): RavenKnowledgeStateStore.Snapshot? {
        val q = query.trim().replace(Regex("\\s+"), " ").take(120)
        if (q.length < 2) return null
        val url = HttpUrl.Builder()
            .scheme("https")
            .host("en.wikipedia.org")
            .addPathSegment("w")
            .addPathSegment("api.php")
            .addQueryParameter("action", "opensearch")
            .addQueryParameter("search", q)
            .addQueryParameter("limit", "3")
            .addQueryParameter("namespace", "0")
            .addQueryParameter("format", "json")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "RavenOS-Launcher-KnowledgeOS")
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val root = JSONArray(response.body?.string().orEmpty())
                val titles = root.optJSONArray(1) ?: return null
                val descriptions = root.optJSONArray(2) ?: return null
                val urls = root.optJSONArray(3) ?: return null
                var index = -1
                for (i in 0 until titles.length()) {
                    if (descriptions.optString(i).isNotBlank()) { index = i; break }
                }
                if (index < 0 && titles.length() > 0) index = 0
                if (index < 0) return null
                val title = titles.optString(index).trim()
                val description = descriptions.optString(index).trim()
                if (title.isBlank()) return null
                val summary = if (description.isBlank()) "Wikipedia result: $title" else "$title — $description"
                RavenKnowledgeStateStore.Snapshot(
                    topic = q,
                    summary = summary.take(320),
                    provider = "WIKIPEDIA",
                    source = "en.wikipedia.org",
                    url = urls.optString(index),
                    scope = "CONTEXTUAL",
                    fetchedAt = System.currentTimeMillis(),
                )
            }
        }.getOrNull()
    }
}
