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
        val app = trustedLabel(packageName, appLabel)
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
        val launcher = pkg == "com.sec.android.app.launcher" || pkg.contains("launcher") && has("home")
        val github = pkg.contains("github") || has("github", "commits", "pull request", "actions")
        val reddit = pkg.contains("reddit") || has("reddit", "subreddit")
        val playStore = pkg.contains("vending") || has("google play", "play store")
        val files = pkg.contains("myfiles") || pkg.contains("documentsui") || has("my files", "downloads")
        val terminal = pkg.contains("termux") || has("terminal", "shell", "bash")
        val gallery = pkg.contains("gallery") || has("gallery", "photos")
        val camera = pkg.contains("camera")

        return when {
            chatGpt || (browser && has("chatgpt")) -> {
                val mode = when {
                    RavenMetaRecursionOS.detect(text) && has("dialogue", "goblin", "ravenos", "follow-me", "follow me") -> "RavenOS / Goblin Vision conversation"
                    has("dialogue bank", "dialogue", "statements", "commentary") -> "dialogue design conversation"
                    has("commit", "compile", "apk", "kotlin", "branch", "github") -> "build/code conversation"
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
                    has("quick settings", "device control") -> "quick settings"
                    else -> "System UI"
                }
                Scene("SYSTEM_UI", "System UI", mode, setOf("SYSTEM"))
            }
            keyboard -> Scene("KEYBOARD", "Keyboard", "typing layer", setOf("INPUT"))
            launcher -> Scene("HOME", app.ifBlank { "Home" }, "launcher/home screen", setOf("HOME"))
            github -> Scene("CODE", "GitHub", when {
                has("actions", "workflow", "run") -> "CI / workflow"
                has("commit") -> "commit/source history"
                has("pull request", "files changed") -> "pull request"
                else -> "source repository"
            }, setOf("BUILD", "WEB"))
            terminal -> Scene("TERMINAL", app.ifBlank { "Terminal" }, "shell / command surface", setOf("BUILD", "SYSTEM"))
            suno -> Scene("MUSIC", "Suno", if (keyboardLike) "music app + input" else "music playback/browsing", setOf("MUSIC"))
            youtube -> Scene("VIDEO", "YouTube", if (has("comments")) "video + comments" else "video surface", setOf("VIDEO", "MEDIA"))
            gmail -> Scene("MAIL", "Gmail", when {
                keyboardLike || has("compose", "send") -> "email compose"
                has("inbox") -> "inbox"
                else -> "mail"
            }, setOf("COMMUNICATION"))
            messages -> Scene("MESSAGING", app.ifBlank { "Messages" }, if (keyboardLike) "message compose" else "messages", setOf("COMMUNICATION") + if (keyboardLike) setOf("INPUT") else emptySet())
            reddit -> Scene("COMMUNITY", "Reddit", if (has("comments")) "thread + comments" else "community feed/thread", setOf("WEB", "DISCOVERY"))
            playStore -> Scene("STORE", "Play Store", "app discovery/install surface", setOf("APP", "DISCOVERY"))
            files -> Scene("FILES", app.ifBlank { "Files" }, "file browser", setOf("FILES"))
            gallery -> Scene("GALLERY", app.ifBlank { "Gallery" }, "image/media gallery", setOf("MEDIA"))
            camera -> Scene("CAMERA", app.ifBlank { "Camera" }, "camera surface", setOf("MEDIA"))
            browser -> Scene("BROWSER", app.ifBlank { "Browser" }, when {
                has("search", "address") -> "web browsing/search"
                has("docs", "documentation", "reference") -> "documentation/reference"
                else -> "web page"
            }, setOf("WEB"))
            else -> Scene("APP", app.ifBlank { "App" }, if (keyboardLike) "app + input" else "app surface", emptySet())
        }
    }

    private fun trustedLabel(packageName: String?, candidate: String?): String {
        val label = candidate.orEmpty().trim()
        val lower = label.lowercase()
        val transient = lower.contains("system deck") || lower.contains("system ui") ||
            lower.contains("smartcapture") || lower.contains("smart capture") ||
            lower.contains("keyboard") || lower.contains("honeyboard")
        return when {
            packageName.isNullOrBlank() -> label
            transient -> friendlyPackage(packageName)
            label.isBlank() -> friendlyPackage(packageName)
            else -> label
        }
    }

    private fun friendlyPackage(pkg: String): String = when (pkg) {
        "com.android.systemui" -> "System UI"
        "com.samsung.android.honeyboard" -> "Samsung Keyboard"
        "com.sec.android.app.launcher" -> "One UI Home"
        "com.android.chrome" -> "Chrome"
        else -> pkg.substringAfterLast('.').replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
