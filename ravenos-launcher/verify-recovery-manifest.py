#!/usr/bin/env python3
"""Fail closed if recovered RavenOS Launcher organs or wiring disappear."""
from __future__ import annotations
import json
from pathlib import Path
import sys

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
MANIFEST = HERE / "UPGRADE-RECOVERY-MANIFEST.json"

def fail(message: str) -> None:
    print(f"RECOVERY_MANIFEST_FAIL: {message}", file=sys.stderr)
    raise SystemExit(1)

if not MANIFEST.is_file():
    fail(f"missing {MANIFEST.relative_to(ROOT)}")

try:
    data = json.loads(MANIFEST.read_text(encoding="utf-8"))
except Exception as exc:
    fail(f"manifest parse error: {exc}")

required = data.get("required_files")
if not isinstance(required, list) or not required:
    fail("required_files must be a non-empty list")
if len(required) != len(set(required)):
    fail("required_files contains duplicates")

missing = []
empty = []
for rel in required:
    if not isinstance(rel, str) or not rel.strip():
        fail("required_files contains an invalid path")
    target = ROOT / rel
    if not target.is_file():
        missing.append(rel)
    elif target.stat().st_size == 0:
        empty.append(rel)

contains = data.get("required_contains", {})
if not isinstance(contains, dict):
    fail("required_contains must be an object")

marker_failures = []
for rel, markers in contains.items():
    target = ROOT / rel
    if not target.is_file():
        marker_failures.append(f"{rel}: FILE_MISSING")
        continue
    text = target.read_text(encoding="utf-8")
    for marker in markers:
        if marker not in text:
            marker_failures.append(f"{rel}: missing marker {marker!r}")

if missing:
    fail("missing recovered files:\n  " + "\n  ".join(missing))
if empty:
    fail("empty recovered files:\n  " + "\n  ".join(empty))
if marker_failures:
    fail("wiring regressions:\n  " + "\n  ".join(marker_failures))

print(
    "RECOVERY_MANIFEST_PASS "
    f"files={len(required)} "
    f"lineages={len(data.get('recovered_lineages', []))} "
    f"baseline={data.get('current_baseline', {}).get('sha', 'UNKNOWN')[:8]}"
)
