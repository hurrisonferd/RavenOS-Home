#!/usr/bin/env python3
"""Wire RavenOS haunted ecology surfaces into the native Home shell."""
from __future__ import annotations

import sys
from pathlib import Path


def replace_once(path: Path, marker: str, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if marker in text:
        print(f"RavenOS ecology already present: {path}")
        return
    if old not in text:
        raise SystemExit(f"RavenOS ecology anchor missing in {path}: {old[:160]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")
    print(f"RavenOS ecology patch: {path}")


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("usage: patch-ravenos-ecology.py <iappyx-root>")
    root = Path(sys.argv[1]).resolve()
    home = root / "src/launcher/app/src/main/java/com/iappyx/launcher/ravenos/RavenHomeActivity.kt"
    office = root / "src/launcher/app/src/main/java/com/iappyx/launcher/ravenos/RavenOfficeBarService.kt"
    manifest = root / "src/launcher/app/src/main/AndroidManifest.xml"

    replace_once(
        home,
        "RAVENOS ECOLOGY: refresh morph on Office event",
        '''        override fun onReceive(context: Context?, intent: Intent?) {\n            renderResident(force = false)\n        }''',
        '''        override fun onReceive(context: Context?, intent: Intent?) {\n            renderResident(force = false)\n            // RAVENOS ECOLOGY: refresh morph on Office event\n            RavenMorphSurface.refresh(this@RavenHomeActivity)\n        }''',
    )

    replace_once(
        home,
        "RAVENOS ECOLOGY: attach morph + ghost hotspots",
        '''        RavenHomeAura.attach(this)\n        ContextCompat.registerReceiver(''',
        '''        RavenHomeAura.attach(this)\n        // RAVENOS ECOLOGY: attach morph + ghost hotspots\n        RavenMorphSurface.attach(this)\n        RavenGhostHotspots.attach(this)\n        ContextCompat.registerReceiver(''',
    )

    replace_once(
        home,
        "RAVENOS ECOLOGY: evaluate cheap local requirements",
        '''        requestAppCatalog()\n        // Let the HOME frame win the race; Office service work is never on the critical first draw.\n        mainHandler.post { signalHomeIfNeeded() }''',
        '''        requestAppCatalog()\n        // RAVENOS ECOLOGY: evaluate cheap local requirements after the frame-critical work.\n        mainHandler.post {\n            RavenHauntRequirements.evaluateAndSignal(this)\n            RavenMorphSurface.refresh(this)\n        }\n        // Let the HOME frame win the race; Office service work is never on the critical first draw.\n        mainHandler.post { signalHomeIfNeeded() }''',
    )

    replace_once(
        home,
        "RAVENOS ECOLOGY: resident summons wheel",
        '''        residentCard = LinearLayout(this).apply {\n            orientation = LinearLayout.VERTICAL\n            setPadding(dp(12), dp(10), dp(12), dp(10))\n            elevation = dp(6).toFloat()\n        }''',
        '''        residentCard = LinearLayout(this).apply {\n            orientation = LinearLayout.VERTICAL\n            setPadding(dp(12), dp(10), dp(12), dp(10))\n            elevation = dp(6).toFloat()\n            // RAVENOS ECOLOGY: resident summons wheel\n            setOnLongClickListener { RavenSummoningWheel.show(this@RavenHomeActivity); true }\n            setOnClickListener { RavenOfficeFeed.show(this@RavenHomeActivity) }\n        }''',
    )

    replace_once(
        home,
        "RAVENOS ECOLOGY: command keyboard",
        '''            setOnClickListener { showAppUniverse(true) }''',
        '''            // RAVENOS ECOLOGY: command keyboard\n            setOnClickListener { RavenCommandPalette.show(this@RavenHomeActivity) }\n            setOnLongClickListener { showAppUniverse(true); true }''',
    )

    replace_once(
        home,
        "RAVENOS ECOLOGY: possessed app popup",
        '''            setOnClickListener {\n                launch(entry)\n                afterLaunch?.invoke()\n            }''',
        '''            // RAVENOS ECOLOGY: possessed app popup\n            setOnLongClickListener {\n                RavenPossessedPopup.show(this@RavenHomeActivity, entry)\n                true\n            }\n            setOnClickListener {\n                launch(entry)\n                afterLaunch?.invoke()\n            }''',
    )

    replace_once(
        home,
        "RAVENOS ECOLOGY: menu entries",
        '''        val items = arrayOf(\n            "Apps",\n            "Sound Deck",\n            "RavenOS Studio",\n            "Office Auto",\n            "Cycle Haunt",\n            "Sleep Office",\n        )''',
        '''        val items = arrayOf(\n            "Summoning Wheel",\n            "Command Palette",\n            "Apps",\n            "Sound Deck",\n            "Office Feed",\n            "Ghost Hotspots",\n            "Tasker / Automation",\n            "RavenOS Studio",\n            "Office Auto",\n            "Cycle Haunt",\n            "Sleep Office",\n        ) // RAVENOS ECOLOGY: menu entries''',
    )
    replace_once(
        home,
        "RAVENOS ECOLOGY: menu routing",
        '''                when (which) {\n                    0 -> showAppUniverse(false)\n                    1 -> showSoundDeck()\n                    2 -> openStudio()\n                    3 -> RavenOfficeBarService.auto(this)\n                    4 -> RavenOfficeBarService.cycleHaunt(this)\n                    5 -> RavenOfficeBarService.disable(this)\n                }''',
        '''                when (which) {\n                    0 -> RavenSummoningWheel.show(this)\n                    1 -> RavenCommandPalette.show(this)\n                    2 -> showAppUniverse(false)\n                    3 -> showSoundDeck()\n                    4 -> RavenOfficeFeed.show(this)\n                    5 -> RavenGhostHotspots.toggle(this)\n                    6 -> RavenTaskerBridge.showSetup(this)\n                    7 -> openStudio()\n                    8 -> RavenOfficeBarService.auto(this)\n                    9 -> RavenOfficeBarService.cycleHaunt(this)\n                    10 -> RavenOfficeBarService.disable(this)\n                } // RAVENOS ECOLOGY: menu routing''',
    )

    replace_once(
        office,
        "RAVENOS ECOLOGY: outbound automation event",
        '''        RavenOfficeTraceStore.record(this, member, signal, detail, note, hauntMode)\n''',
        '''        RavenOfficeTraceStore.record(this, member, signal, detail, note, hauntMode)\n        // RAVENOS ECOLOGY: outbound automation event (sanitized metadata only).\n        RavenTaskerBridge.emit(this, "office_state", "owner:${member.id}|signal:$signal|haunt:${hauntMode.label}")\n''',
    )

    replace_once(
        manifest,
        "RAVENOS ECOLOGY: token-gated Tasker command receiver",
        '''    </application>''',
        '''        <!-- RAVENOS ECOLOGY: token-gated Tasker command receiver. Exported intentionally;\n             every request must carry the locally-generated RavenOS token. -->\n        <receiver\n            android:name=".ravenos.RavenTaskerReceiver"\n            android:enabled="true"\n            android:exported="true">\n            <intent-filter>\n                <action android:name="com.ravenos.launcher.TASKER_COMMAND" />\n            </intent-filter>\n        </receiver>\n\n    </application>''',
    )

    print("RAVENOS_ECOLOGY=true")


if __name__ == "__main__":
    main()
