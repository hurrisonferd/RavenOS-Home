package com.iappyx.launcher.ravenos

import android.content.Context

/** Deterministic app/surface interpretation from package identity + owner-authorized visible text. */
object RavenAppSemanticsOS {
    data class Scene(
        val kind: String,
        val label: String,
        val summary: String,
        val tags: Set<String>,
    )

    fun interpret(
        context: Context,
        packageName: String?,
        appLabel: String?,
        visibleText: String?,
        keyboardLike: Boolean,
    ): Scene {
        val pkg = packageName.orEmpty().lowercase()
        val app = appLabel?.takeIf { it.isNotBlank() } ?: friendlyPackage(packageName.orEmpty())
        val text = visibleText.orEmpty().lowercase()

        fun has(vararg terms: String) = terms.any(text::contains)
        val chatGpt = pkg.contains("openai") || has("chatgpt", "ask chatgpt", "chatgpt can make mistakes")
        val browser = listOf("chrome", "firefox", "opera", "browser", "edge").any(pkg::contains)
        val settings = pkg.contains("settings") || pkg == "com.android.settings"
        val messages = pkg.contains("messag") || pkg.contains("mms")
        val gmail = pkg.contains("gmail")
        val suno = pkg.contains("suno") || has("suno")
        val youtube = pkg.contains("youtube") || has("youtube")
        val systemUi = pkg == "com.android.systemui"
        val keyboard = pkg.contains("honeyboard") || pkg.contains("inputmethod") || keyboardLike

        return when {
            chatGpt || (browser && has("chatgpt")) -> {
                val mode = when {
                    keyboardLike || has("ask chatgpt") -> "conversation + input"
                    has("regenerate", "copy", "share") -> "conversation"
                    else -> "ChatGPT page"
                }
                Scene("CHATGPT", "ChatGPT", mode, setOf("AI", "CONVERSATION") + if (keyboardLike) setOf("INPUT") else emptySet())
            }
            settings -> {
                val mode = when {
                    has("accessibility") -> "Accessibility settings"
                    has("appear on top", "display over other apps") -> "overlay permission settings"
                    has("notification access") -> "notification access settings"
                    has("battery", "never sleeping") -> "battery/background settings"
                    has("permission") -> "permissions"
                    else -> "Android settings"
                }
                Scene("SETTINGS", "Settings", mode, setOf("SYSTEM", "BOUNDARY"))
            }
            systemUi -> {
                val mode = when {
                    has("notifications", "clear") -> "notification shade"
                    has("media output") -> "media controls"
                    else -> "System UI"
                }
                Scene("SYSTEM_UI", "System UI", mode, setOf("SYSTEM"))
            }
            keyboard -> Scene("KEYBOARD", "Keyboard", "typing layer", setOf("INPUT"))
            suno -> Scene("MUSIC", "Suno", if (keyboardLike) "music app + input" else "music playback/browsing", setOf("MUSIC"))
            youtube -> Scene("VIDEO", "YouTube", if (has("comments")) "video + comments" else "video surface", setOf("VIDEO", "MEDIA"))
            gmail -> Scene("MAIL", "Gmail", when {
                keyboardLike || has("compose", "send") -> "email compose"
                has("inbox") -> "inbox"
                else -> "mail"
            }, setOf("COMMUNICATION"))
            messages -> Scene("MESSAGING", app.ifBlank { "Messages" }, if (keyboardLike) "message compose" else "messages", setOf("COMMUNICATION") + if (keyboardLike) setOf("INPUT") else emptySet())
            browser -> Scene("BROWSER", app.ifBlank { "Browser" }, when {
                has("search", "address") -> "web browsing/search"
                else -> "web page"
            }, setOf("WEB"))
            else -> Scene("APP", app.ifBlank { "App" }, if (keyboardLike) "app + input" else "app surface", emptySet())
        }
    }

    private fun friendlyPackage(pkg: String): String = when (pkg) {
        "com.android.systemui" -> "System UI"
        "com.samsung.android.honeyboard" -> "Samsung Keyboard"
        "com.sec.android.app.launcher" -> "One UI Home"
        else -> pkg.substringAfterLast('.').replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
