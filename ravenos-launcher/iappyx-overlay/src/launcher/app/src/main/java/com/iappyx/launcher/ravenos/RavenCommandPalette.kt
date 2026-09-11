package com.iappyx.launcher.ravenos

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

/** kybd/RETUI-inspired local command keyboard. No model call is required. */
object RavenCommandPalette {
    fun show(activity: Activity, initial: String = "") {
        val density = activity.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        val input = EditText(activity).apply {
            hint = "> command · / app · #feed · #tasker · ♡ KYU · ⚛ ATOM · = math"
            setTextColor(Color.WHITE)
            setHintTextColor(0xFF918A99.toInt())
            setSingleLine(true)
            setText(initial)
        }
        val preview = TextView(activity).apply {
            setTextColor(0xFFD8D8E4.toInt())
            textSize = 11f
            setPadding(0, dp(8), 0, 0)
        }
        root.addView(input, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)))
        root.addView(preview)

        fun updatePreview(raw: String) {
            val q = raw.trim()
            preview.text = when {
                q.isBlank() -> "Prefixes: > local command · / app · #feed · #wheel · #tasker · ♡ KYU · ⚛ ATOM · 🌙 NYX · = 54*1.07"
                q.startsWith("=") -> calculate(q.removePrefix("=")) ?: "Simple math: number + - * / number"
                q.startsWith(">") -> "LOCAL COMMAND · ${q.removePrefix(">").trim()}"
                q == "#feed" -> "Open bounded Office Feed"
                q == "#wheel" -> "Open Raven Summoning Wheel"
                q == "#tasker" -> RavenTaskerBridge.summary(activity)
                q.startsWith("♡") || q.startsWith("💗") -> "💗 KYU namespace · ${q.drop(1).trim()}"
                q.startsWith("⚛️") || q.startsWith("⚛") -> "⚛️ ATOM namespace · ${q.removePrefix("⚛️").removePrefix("⚛").trim()}"
                q.startsWith("🌙") -> "🌙 NYX namespace · ${q.removePrefix("🌙").trim()}"
                else -> {
                    val term = q.removePrefix("/").trim().lowercase()
                    val apps = RavenAppCatalog.current().orEmpty().filter {
                        term.isBlank() || it.label.lowercase().contains(term) || it.packageName.lowercase().contains(term)
                    }.take(5)
                    if (apps.isEmpty()) "No cached app match yet." else apps.joinToString("\n") { "▦ ${it.label}" }
                }
            }
        }

        input.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = updatePreview(s?.toString().orEmpty())
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })
        updatePreview(initial)

        val dialog = AlertDialog.Builder(activity)
            .setTitle("Raven Command")
            .setView(root)
            .setNegativeButton("Close", null)
            .setPositiveButton("Run", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val result = execute(activity, input.text?.toString().orEmpty())
                if (result.close) dialog.dismiss()
                if (result.message.isNotBlank()) Toast.makeText(activity, result.message, Toast.LENGTH_SHORT).show()
            }
            input.requestFocus()
            input.post { activity.getSystemService(InputMethodManager::class.java)?.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT) }
        }
        dialog.show()
        RavenTaskerBridge.emit(activity, "command_palette", "opened")
    }

    private fun execute(activity: Activity, raw: String): ExecResult {
        val q = raw.trim()
        if (q.isBlank()) return ExecResult(false, "Type a command or app.")
        if (q == "#feed") {
            RavenOfficeFeed.show(activity)
            return ExecResult(true, "Office Feed")
        }
        if (q == "#wheel") {
            RavenSummoningWheel.show(activity)
            return ExecResult(true, "Summoning Wheel")
        }
        if (q == "#tasker") {
            RavenTaskerBridge.showSetup(activity)
            return ExecResult(true, "Tasker bridge")
        }
        if (q.startsWith("=")) {
            val answer = calculate(q.removePrefix("=")) ?: return ExecResult(false, "Math format: 54*1.07")
            return ExecResult(false, answer)
        }
        if (q.startsWith(">")) {
            val result = RavenCommandRouter.execute(activity, q.removePrefix(">").trim())
            return ExecResult(result.handled, result.message.ifBlank { if (result.handled) "Done" else "Unknown local command" })
        }

        namespace(q)?.let { (owner, rest) ->
            RavenOfficeBarService.pin(activity, owner)
            if (rest.isNotBlank()) RavenOfficeBarService.signal(activity, "SEARCH", "namespace:$owner|query:${rest.take(120)}")
            return ExecResult(true, "Office → $owner")
        }

        val term = q.removePrefix("/").trim().lowercase()
        val apps = RavenAppCatalog.current()
        if (apps == null) {
            RavenAppCatalog.load(activity) {}
            return ExecResult(false, "App catalog warming…")
        }
        val match = apps.firstOrNull { it.label.equals(term, true) }
            ?: apps.firstOrNull { it.label.lowercase().startsWith(term) }
            ?: apps.firstOrNull { it.label.lowercase().contains(term) || it.packageName.lowercase().contains(term) }
        if (match != null) {
            launch(activity, match)
            return ExecResult(true, match.label)
        }
        return ExecResult(false, "No local match")
    }

    private fun namespace(q: String): Pair<String, String>? = when {
        q.startsWith("💗") -> "KYU" to q.removePrefix("💗").trim()
        q.startsWith("♡") -> "KYU" to q.removePrefix("♡").trim()
        q.startsWith("⚛️") -> "ATOM" to q.removePrefix("⚛️").trim()
        q.startsWith("⚛") -> "ATOM" to q.removePrefix("⚛").trim()
        q.startsWith("🌙") -> "NYX" to q.removePrefix("🌙").trim()
        q.startsWith("🔥") -> "LILITH" to q.removePrefix("🔥").trim()
        q.startsWith("🪐") -> "YORI" to q.removePrefix("🪐").trim()
        else -> null
    }

    private fun launch(activity: Activity, entry: RavenAppCatalog.Entry) {
        try {
            activity.startActivity(Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(entry.packageName, entry.activityName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            RavenOfficeBarService.signal(activity, "APP_LAUNCH", "package:${entry.packageName}")
        } catch (_: Throwable) {}
    }

    private fun calculate(raw: String): String? {
        val m = Regex("^\\s*(-?\\d+(?:\\.\\d+)?)\\s*([+\\-*/])\\s*(-?\\d+(?:\\.\\d+)?)\\s*$").matchEntire(raw) ?: return null
        val a = m.groupValues[1].toDoubleOrNull() ?: return null
        val b = m.groupValues[3].toDoubleOrNull() ?: return null
        val value = when (m.groupValues[2]) {
            "+" -> a + b
            "-" -> a - b
            "*" -> a * b
            "/" -> if (b == 0.0) return "division by zero" else a / b
            else -> return null
        }
        val pretty = if (value % 1.0 == 0.0) value.toLong().toString() else "%.6f".format(value).trimEnd('0').trimEnd('.')
        return "$raw = $pretty"
    }

    private data class ExecResult(val close: Boolean, val message: String)
}
