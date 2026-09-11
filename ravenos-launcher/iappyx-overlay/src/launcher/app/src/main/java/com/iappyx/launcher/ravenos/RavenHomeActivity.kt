package com.iappyx.launcher.ravenos

import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.iappyx.launcher.LauncherActivity
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * RavenOS-native HOME surface.
 *
 * This intentionally does NOT inherit the donor workspace UI. The mature iappyx machinery
 * remains reachable as RavenOS Studio, while Android HOME lands on this utility-first shell.
 */
class RavenHomeActivity : AppCompatActivity() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val density by lazy { resources.displayMetrics.density }

    private lateinit var clockView: TextView
    private lateinit var dateView: TextView
    private lateinit var residentOwner: TextView
    private lateinit var residentNote: TextView
    private lateinit var residentCard: LinearLayout
    private lateinit var favoritesGrid: GridLayout
    private lateinit var mediaLabel: TextView
    private lateinit var mediaSeek: SeekBar

    private val officeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            renderResident()
        }
    }

    private val tick = object : Runnable {
        override fun run() {
            renderClock()
            renderResident()
            mainHandler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(buildHome())
        RavenHomeAura.attach(this)
        RavenOfficeBarService.signal(this, "HOME", "raven-native-home")
        ContextCompat.registerReceiver(
            this,
            officeReceiver,
            IntentFilter(RavenOfficeStateStore.ACTION_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        mainHandler.post(tick)
    }

    override fun onResume() {
        super.onResume()
        RavenOfficeBarService.signal(this, "HOME", "raven-native-home")
        rebuildFavorites()
        refreshAudio()
        renderResident()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(tick)
        try { unregisterReceiver(officeReceiver) } catch (_: Throwable) {}
        super.onDestroy()
    }

    private fun buildHome(): View {
        val root = FrameLayout(this).apply { setBackgroundColor(Color.TRANSPARENT) }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(50), dp(18), dp(20))
        }
        root.addView(column, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
        }
        val timeColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        clockView = text("--:--", 39f, Color.WHITE, true)
        dateView = text("", 15f, 0xFFD8D8E4.toInt(), false)
        timeColumn.addView(clockView)
        timeColumn.addView(dateView)
        top.addView(timeColumn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        residentCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            elevation = dp(6).toFloat()
        }
        residentOwner = text("♡ RAVENOS", 16f, Color.WHITE, true)
        residentNote = text("office waking…", 11f, Color.WHITE, false).apply { maxLines = 2 }
        residentCard.addView(residentOwner)
        residentCard.addView(residentNote, top(4))
        top.addView(residentCard, LinearLayout.LayoutParams(dp(220), ViewGroup.LayoutParams.WRAP_CONTENT))
        column.addView(top)

        column.addView(View(this), LinearLayout.LayoutParams(1, 0, 1f))

        val utility = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(10))
            background = rounded(0x9A15121B.toInt(), 24, 0x55FFFFFF)
        }
        val utilityTitle = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        utilityTitle.addView(text("RAVEN DECK", 13f, 0xFFFF64B4.toInt(), true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        utilityTitle.addView(smallButton("Apps") { showAppUniverse(true) })
        utilityTitle.addView(smallButton("Studio") { openStudio() }, left(6))
        utility.addView(utilityTitle)

        mediaLabel = text("MEDIA", 12f, Color.WHITE, true)
        utility.addView(mediaLabel, top(8))
        mediaSeek = SeekBar(this).apply {
            max = 100
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    mediaLabel.text = "MEDIA  $progress%"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    RavenSystemDeck.setMediaPercent(this@RavenHomeActivity, progress)
                    RavenOfficeBarService.signal(this@RavenHomeActivity, "SYSTEM_DECK", "media:$progress")
                }
            })
        }
        utility.addView(mediaSeek)

        val modes = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        modes.addView(modeButton("Normal", AudioManager.RINGER_MODE_NORMAL), weight())
        modes.addView(modeButton("Vibrate", AudioManager.RINGER_MODE_VIBRATE), weight(6))
        modes.addView(modeButton("Silent", AudioManager.RINGER_MODE_SILENT), weight(6))
        utility.addView(modes, top(3))
        column.addView(utility)

        column.addView(text("FAVORITES", 11f, 0xFFD8D8E4.toInt(), true), top(14))
        favoritesGrid = GridLayout(this).apply {
            columnCount = 4
            alignmentMode = GridLayout.ALIGN_BOUNDS
            useDefaultMargins = false
        }
        column.addView(favoritesGrid, top(5))

        val search = TextView(this).apply {
            text = "⌕  Search apps and RavenOS"
            textSize = 16f
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(Color.WHITE)
            setPadding(dp(18), 0, dp(18), 0)
            background = rounded(0xC6282730.toInt(), 28, 0x55FFFFFF)
            setOnClickListener { showAppUniverse(true) }
        }
        column.addView(search, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)).apply { topMargin = dp(10) })

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        nav.addView(navButton("▦\nApps") { showAppUniverse(false) }, weight())
        nav.addView(navButton("♫\nSound") { showSoundDeck() }, weight())
        nav.addView(navButton("✦\nStudio") { openStudio() }, weight())
        nav.addView(navButton("R\nMenu") { showRavenMenu() }, weight())
        column.addView(nav, top(10))

        return root
    }

    private fun renderClock() {
        val now = LocalDateTime.now()
        clockView.text = now.format(DateTimeFormatter.ofPattern("H:mm"))
        dateView.text = now.format(DateTimeFormatter.ofPattern("EEE · MMM d"))
    }

    private fun renderResident() {
        val snapshot = RavenOfficeStateStore.read(this)
        if (snapshot == null) {
            residentOwner.text = "♡ RAVENOS"
            residentNote.text = "office waking…"
            residentCard.background = rounded(0xB3241A2A.toInt(), 22, 0x55FFFFFF)
            return
        }
        residentOwner.text = "${snapshot.emoji} ${snapshot.owner}"
        residentNote.text = snapshot.note
        val color = Color.argb(205, Color.red(snapshot.accent), Color.green(snapshot.accent), Color.blue(snapshot.accent))
        residentCard.background = rounded(color, 22, 0x66FFFFFF)
        val textColor = contrastText(snapshot.accent)
        residentOwner.setTextColor(textColor)
        residentNote.setTextColor(textColor)
    }

    private fun refreshAudio() {
        val state = RavenSystemDeck.snapshot(this)
        mediaSeek.progress = state.media.percent
        mediaLabel.text = "MEDIA  ${state.media.percent}%"
    }

    private fun rebuildFavorites() {
        favoritesGrid.removeAllViews()
        val apps = launchableApps()
        val preferredPackages = listOf(
            "com.openai.chatgpt",
            "com.android.chrome",
            "com.sec.android.app.sbrowser",
            "com.google.android.gm",
            "com.google.android.youtube",
            "com.android.vending",
            "com.samsung.android.messaging",
            "com.google.android.apps.messaging",
            "com.samsung.android.dialer",
            "com.google.android.dialer",
            "com.sec.android.app.camera",
            "com.google.android.GoogleCamera",
        )
        val picked = mutableListOf<AppEntry>()
        preferredPackages.forEach { pkg -> apps.firstOrNull { it.packageName == pkg }?.let { if (picked.none { p -> p.packageName == pkg }) picked.add(it) } }
        apps.forEach { if (picked.size < 8 && picked.none { p -> p.packageName == it.packageName }) picked.add(it) }
        picked.take(8).forEach { favoritesGrid.addView(appTile(it, compact = true), gridParams()) }
    }

    private fun showAppUniverse(focusSearch: Boolean) {
        RavenOfficeBarService.signal(this, "APP_UNIVERSE", "raven-native-drawer")
        val dialog = Dialog(this)
        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(42), dp(18), dp(18))
            setBackgroundColor(0xF515141A.toInt())
        }
        val titleRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        titleRow.addView(text("APP UNIVERSE", 28f, Color.WHITE, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        titleRow.addView(smallButton("×") { dialog.dismiss() })
        shell.addView(titleRow)
        val search = EditText(this).apply {
            hint = "Search apps"
            setHintTextColor(0xFF8F8F9D.toInt())
            setTextColor(Color.WHITE)
            textSize = 16f
            singleLine = true
            setPadding(dp(16), 0, dp(16), 0)
            background = rounded(0xFF2A2931.toInt(), 22, 0x55FFFFFF)
        }
        shell.addView(search, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)).apply { topMargin = dp(14) })
        val scroll = ScrollView(this)
        val grid = GridLayout(this).apply { columnCount = 4; alignmentMode = GridLayout.ALIGN_BOUNDS }
        scroll.addView(grid)
        shell.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(12) })
        val apps = launchableApps()
        fun render(query: String) {
            grid.removeAllViews()
            val q = query.trim().lowercase()
            apps.asSequence()
                .filter { q.isBlank() || it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
                .take(240)
                .forEach { grid.addView(appTile(it, compact = false) { dialog.dismiss() }, gridParams()) }
        }
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { render(s?.toString().orEmpty()) }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        render("")
        dialog.setContentView(shell)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        dialog.setOnShowListener {
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            if (focusSearch) {
                search.requestFocus()
                search.post { getSystemService(InputMethodManager::class.java)?.showSoftInput(search, InputMethodManager.SHOW_IMPLICIT) }
            }
        }
        dialog.show()
    }

    private fun showSoundDeck() {
        val state = RavenSystemDeck.snapshot(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(4))
        }
        fun addSlider(label: String, value: Int, setter: (Int) -> Unit) {
            val t = text("$label  $value%", 13f, Color.WHITE, true)
            box.addView(t, top(8))
            box.addView(SeekBar(this).apply {
                max = 100
                progress = value
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { t.text = "$label  $progress%" }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) { setter(progress) }
                })
            })
        }
        addSlider("MEDIA", state.media.percent) { RavenSystemDeck.setMediaPercent(this, it) }
        addSlider("RING", state.ring.percent) { RavenSystemDeck.setRingPercent(this, it) }
        addSlider("ALARM", state.alarm.percent) { RavenSystemDeck.setAlarmPercent(this, it) }
        AlertDialog.Builder(this)
            .setTitle("Raven Sound Deck")
            .setView(box)
            .setPositiveButton("Done", null)
            .show()
        RavenOfficeBarService.signal(this, "SYSTEM_DECK", "sound-deck")
    }

    private fun showRavenMenu() {
        val items = arrayOf(
            "Apps",
            "Sound Deck",
            "RavenOS Studio",
            "Office Auto",
            "Cycle Haunt",
            "Sleep Office",
        )
        AlertDialog.Builder(this)
            .setTitle("RavenOS")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> showAppUniverse(false)
                    1 -> showSoundDeck()
                    2 -> openStudio()
                    3 -> RavenOfficeBarService.auto(this)
                    4 -> RavenOfficeBarService.cycleHaunt(this)
                    5 -> RavenOfficeBarService.disable(this)
                }
            }
            .show()
    }

    private fun openStudio() {
        RavenOfficeBarService.signal(this, "STUDIO", "open-workshop")
        startActivity(
            Intent(this, LauncherActivity::class.java)
                .putExtra(EXTRA_OPEN_STUDIO, true)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
    }

    private fun launch(entry: AppEntry) {
        RavenOfficeBarService.signal(this, "APP_LAUNCH", "package:${entry.packageName}")
        try {
            startActivity(Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(entry.packageName, entry.activityName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Throwable) {}
    }

    private fun launchableApps(): List<AppEntry> {
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(query, 0)
            .asSequence()
            .mapNotNull { info ->
                val ai = info.activityInfo ?: return@mapNotNull null
                if (ai.packageName == packageName && ai.name.contains("RavenHomeActivity")) return@mapNotNull null
                val label = info.loadLabel(packageManager)?.toString()?.trim().orEmpty().ifBlank { ai.packageName }
                AppEntry(label, ai.packageName, ai.name, info.loadIcon(packageManager))
            }
            .distinctBy { "${it.packageName}/${it.activityName}" }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    private fun appTile(entry: AppEntry, compact: Boolean, afterLaunch: (() -> Unit)? = null): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(if (compact) 4 else 10), dp(4), dp(if (compact) 4 else 10))
            val icon = ImageView(this@RavenHomeActivity).apply {
                setImageDrawable(entry.icon)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            addView(icon, LinearLayout.LayoutParams(dp(if (compact) 48 else 54), dp(if (compact) 48 else 54)))
            addView(text(entry.label, if (compact) 10f else 11f, Color.WHITE, false).apply {
                gravity = Gravity.CENTER
                maxLines = 1
            }, top(4))
            setOnClickListener { launch(entry); afterLaunch?.invoke() }
        }
    }

    private fun modeButton(label: String, mode: Int): Button = smallButton(label) {
        RavenSystemDeck.setRingerMode(this, mode)
        RavenOfficeBarService.signal(this, "SYSTEM_DECK", "ringer:$label")
    }

    private fun navButton(label: String, click: () -> Unit): Button = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 11f
        setTextColor(Color.WHITE)
        background = ColorDrawable(Color.TRANSPARENT)
        setOnClickListener { click() }
    }

    private fun smallButton(label: String, click: () -> Unit): Button = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 11f
        minWidth = 0
        minimumWidth = 0
        setTextColor(Color.WHITE)
        background = rounded(0x662D2A34, 16, 0x44FFFFFF)
        setPadding(dp(10), 0, dp(10), 0)
        setOnClickListener { click() }
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun rounded(color: Int, radiusDp: Int, strokeColor: Int): GradientDrawable = GradientDrawable().apply {
        cornerRadius = dp(radiusDp).toFloat()
        setColor(color)
        setStroke(dp(1), strokeColor)
    }

    private fun gridParams(): GridLayout.LayoutParams = GridLayout.LayoutParams().apply {
        width = 0
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        setMargins(dp(2), dp(2), dp(2), dp(2))
    }

    private fun top(value: Int): LinearLayout.LayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(value) }
    private fun left(value: Int): LinearLayout.LayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(42)).apply { marginStart = dp(value) }
    private fun weight(startMargin: Int = 0): LinearLayout.LayoutParams = LinearLayout.LayoutParams(0, dp(42), 1f).apply { marginStart = dp(startMargin) }
    private fun dp(value: Int): Int = (value * density).toInt()

    private fun contrastText(color: Int): Int {
        val perceived = (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000
        return if (perceived >= 175) Color.BLACK else Color.WHITE
    }

    private data class AppEntry(
        val label: String,
        val packageName: String,
        val activityName: String,
        val icon: android.graphics.drawable.Drawable,
    )

    companion object {
        const val EXTRA_OPEN_STUDIO = "RAVEN_OPEN_STUDIO"
    }
}
