package com.iappyx.launcher.ravenos

import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.iappyx.launcher.LauncherActivity
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * RavenOS-native HOME surface.
 *
 * PERFORMANCE LAW:
 * - first frame does not query every installed application
 * - Office UI is event-driven, not polled every second
 * - app inventory is process-cached and discovered off the UI thread
 * - App Universe virtualizes tiles instead of inflating the whole device at once
 *
 * The mature donor workspace remains RavenOS Studio and is never needed for normal HOME use.
 */
class RavenHomeActivity : AppCompatActivity() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val density by lazy { resources.displayMetrics.density }
    private val clockFormat = DateTimeFormatter.ofPattern("H:mm")
    private val dateFormat = DateTimeFormatter.ofPattern("EEE · MMM d")

    private lateinit var clockView: TextView
    private lateinit var dateView: TextView
    private lateinit var residentOwner: TextView
    private lateinit var residentNote: TextView
    private lateinit var residentCard: LinearLayout
    private lateinit var favoritesGrid: GridLayout
    private lateinit var mediaLabel: TextView
    private lateinit var mediaSeek: SeekBar

    private var lastResidentAt = Long.MIN_VALUE
    private var favoritesFingerprint = ""

    private val officeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            renderResident(force = false)
        }
    }

    private val clockTick = object : Runnable {
        override fun run() {
            renderClock()
            scheduleClockTick()
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
        ContextCompat.registerReceiver(
            this,
            officeReceiver,
            IntentFilter(RavenOfficeStateStore.ACTION_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        // First paint is intentionally cheap. Expensive app discovery happens afterward.
        renderClock()
        renderResident(force = true)
        refreshAudio()
        requestAppCatalog()
        mainHandler.post { signalHomeIfNeeded() }
    }

    override fun onStart() {
        super.onStart()
        scheduleClockTick()
    }

    override fun onResume() {
        super.onResume()
        renderClock()
        renderResident(force = false)
        refreshAudio()
        requestAppCatalog()
        // Let the HOME frame win the race; Office service work is never on the critical first draw.
        mainHandler.post { signalHomeIfNeeded() }
    }

    override fun onStop() {
        mainHandler.removeCallbacks(clockTick)
        super.onStop()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(clockTick)
        try { unregisterReceiver(officeReceiver) } catch (_: Throwable) {}
        super.onDestroy()
    }

    private fun buildHome(): View {
        val root = FrameLayout(this).apply { setBackgroundColor(Color.TRANSPARENT) }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(12))
        }
        root.addView(
            column,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            column.setPadding(dp(18), bars.top + dp(12), dp(18), bars.bottom + dp(8))
            insets
        }

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
        utilityTitle.addView(
            text("RAVEN DECK", 13f, 0xFFFF64B4.toInt(), true),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
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
        column.addView(
            search,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)).apply { topMargin = dp(10) },
        )

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
        val nextClock = now.format(clockFormat)
        val nextDate = now.format(dateFormat)
        if (clockView.text.toString() != nextClock) clockView.text = nextClock
        if (dateView.text.toString() != nextDate) dateView.text = nextDate
    }

    private fun scheduleClockTick() {
        mainHandler.removeCallbacks(clockTick)
        val now = System.currentTimeMillis()
        val delay = (60_000L - (now % 60_000L)).coerceAtLeast(500L)
        mainHandler.postDelayed(clockTick, delay)
    }

    private fun renderResident(force: Boolean) {
        val snapshot = RavenOfficeStateStore.read(this)
        if (snapshot == null) {
            if (!force && lastResidentAt == -1L) return
            lastResidentAt = -1L
            residentOwner.text = "♡ RAVENOS"
            residentNote.text = "office waking…"
            residentCard.background = rounded(0xB3241A2A.toInt(), 22, 0x55FFFFFF)
            return
        }
        if (!force && snapshot.updatedAt == lastResidentAt) return
        lastResidentAt = snapshot.updatedAt
        residentOwner.text = "${snapshot.emoji} ${snapshot.owner}"
        residentNote.text = snapshot.note
        val color = Color.argb(
            205,
            Color.red(snapshot.accent),
            Color.green(snapshot.accent),
            Color.blue(snapshot.accent),
        )
        residentCard.background = rounded(color, 22, 0x66FFFFFF)
        val textColor = contrastText(snapshot.accent)
        residentOwner.setTextColor(textColor)
        residentNote.setTextColor(textColor)
    }

    private fun signalHomeIfNeeded() {
        val snapshot = RavenOfficeStateStore.read(this)
        if (snapshot?.signal == "HOME" && snapshot.detail == "raven-native-home") return
        RavenOfficeBarService.signal(this, "HOME", "raven-native-home")
    }

    private fun refreshAudio() {
        val state = RavenSystemDeck.snapshot(this)
        if (mediaSeek.progress != state.media.percent) mediaSeek.progress = state.media.percent
        mediaLabel.text = "MEDIA  ${state.media.percent}%"
    }

    private fun requestAppCatalog() {
        RavenAppCatalog.current()?.let {
            rebuildFavorites(it)
            return
        }
        RavenAppCatalog.load(this) { apps ->
            if (!isFinishing && !isDestroyed) rebuildFavorites(apps)
        }
    }

    private fun rebuildFavorites(apps: List<RavenAppCatalog.Entry>) {
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
        val picked = mutableListOf<RavenAppCatalog.Entry>()
        preferredPackages.forEach { pkg ->
            apps.firstOrNull { it.packageName == pkg }?.let { candidate ->
                if (picked.none { it.packageName == candidate.packageName }) picked += candidate
            }
        }
        apps.forEach { candidate ->
            if (picked.size < 8 && picked.none { it.packageName == candidate.packageName }) picked += candidate
        }
        val chosen = picked.take(8)
        val fingerprint = chosen.joinToString("|") { it.key }
        if (fingerprint == favoritesFingerprint) return
        favoritesFingerprint = fingerprint
        favoritesGrid.removeAllViews()
        chosen.forEach { favoritesGrid.addView(appTile(it, compact = true), gridParams()) }
    }

    private fun showAppUniverse(focusSearch: Boolean) {
        RavenOfficeBarService.signal(this, "APP_UNIVERSE", "raven-native-drawer")
        val dialog = Dialog(this)
        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(42), dp(18), dp(18))
            setBackgroundColor(0xF515141A.toInt())
        }
        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        titleRow.addView(
            text("APP UNIVERSE", 28f, Color.WHITE, true),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        titleRow.addView(smallButton("×") { dialog.dismiss() })
        shell.addView(titleRow)

        val search = EditText(this).apply {
            hint = "Search apps"
            setHintTextColor(0xFF8F8F9D.toInt())
            setTextColor(Color.WHITE)
            textSize = 16f
            setSingleLine(true)
            setPadding(dp(16), 0, dp(16), 0)
            background = rounded(0xFF2A2931.toInt(), 22, 0x55FFFFFF)
        }
        shell.addView(
            search,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)).apply { topMargin = dp(14) },
        )

        val grid = GridView(this).apply {
            numColumns = 4
            verticalSpacing = dp(4)
            horizontalSpacing = dp(2)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            clipToPadding = false
            setPadding(0, dp(8), 0, dp(12))
        }
        shell.addView(
            grid,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(6) },
        )

        var catalog = RavenAppCatalog.current().orEmpty()
        var visible = catalog
        val adapter = object : BaseAdapter() {
            override fun getCount(): Int = visible.size
            override fun getItem(position: Int): RavenAppCatalog.Entry = visible[position]
            override fun getItemId(position: Int): Long = visible[position].key.hashCode().toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View =
                appTile(visible[position], compact = false) { dialog.dismiss() }
        }
        grid.adapter = adapter

        fun render(query: String) {
            val q = query.trim().lowercase()
            visible = catalog.asSequence()
                .filter { q.isBlank() || it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
                .take(320)
                .toList()
            adapter.notifyDataSetChanged()
        }

        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                render(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        render("")

        if (catalog.isEmpty()) {
            RavenAppCatalog.load(this) { apps ->
                if (!dialog.isShowing && isFinishing) return@load
                catalog = apps
                render(search.text?.toString().orEmpty())
            }
        }

        dialog.setContentView(shell)
        dialog.setOnShowListener {
            dialog.window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
            if (focusSearch) {
                search.requestFocus()
                search.post {
                    getSystemService(InputMethodManager::class.java)
                        ?.showSoftInput(search, InputMethodManager.SHOW_IMPLICIT)
                }
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
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        t.text = "$label  $progress%"
                    }
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

    private fun launch(entry: RavenAppCatalog.Entry) {
        RavenOfficeBarService.signal(this, "APP_LAUNCH", "package:${entry.packageName}")
        try {
            startActivity(Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(entry.packageName, entry.activityName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Throwable) {}
    }

    private fun appTile(
        entry: RavenAppCatalog.Entry,
        compact: Boolean,
        afterLaunch: (() -> Unit)? = null,
    ): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(if (compact) 4 else 10), dp(4), dp(if (compact) 4 else 10))
            val icon = ImageView(this@RavenHomeActivity).apply {
                setImageDrawable(RavenAppCatalog.icon(this@RavenHomeActivity, entry))
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            addView(icon, LinearLayout.LayoutParams(dp(if (compact) 48 else 54), dp(if (compact) 48 else 54)))
            addView(
                text(entry.label, if (compact) 10f else 11f, Color.WHITE, false).apply {
                    gravity = Gravity.CENTER
                    maxLines = 1
                },
                top(4),
            )
            setOnClickListener {
                launch(entry)
                afterLaunch?.invoke()
            }
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

    private fun top(value: Int): LinearLayout.LayoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    ).apply { topMargin = dp(value) }

    private fun left(value: Int): LinearLayout.LayoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        dp(42),
    ).apply { marginStart = dp(value) }

    private fun weight(startMargin: Int = 0): LinearLayout.LayoutParams = LinearLayout.LayoutParams(
        0,
        dp(42),
        1f,
    ).apply { marginStart = dp(startMargin) }

    private fun dp(value: Int): Int = (value * density).toInt()

    private fun contrastText(color: Int): Int {
        val perceived = (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000
        return if (perceived >= 175) Color.BLACK else Color.WHITE
    }

    companion object {
        const val EXTRA_OPEN_STUDIO = "RAVEN_OPEN_STUDIO"
    }
}
