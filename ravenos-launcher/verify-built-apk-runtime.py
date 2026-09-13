#!/usr/bin/env python3
"""Fail-closed RavenOS APK runtime inventory verifier.

Usage:
  python3 ravenos-launcher/verify-built-apk-runtime.py <apk> [--require-next-wave]

The authority list lives in APK-BINARY-RECOVERY-MANIFEST-1a660aea.json.
This verifies compiled DEX presence, not merely source-file presence.
"""
from __future__ import annotations
import argparse, json, re, struct, sys, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent
MANIFEST = ROOT / "APK-BINARY-RECOVERY-MANIFEST-1a660aea.json"
PREFIX = "Lcom/iappyx/launcher/ravenos/"


def uleb(data: bytes, off: int) -> tuple[int, int]:
    value = 0
    shift = 0
    while True:
        if off >= len(data):
            raise ValueError("truncated uleb128")
        b = data[off]
        off += 1
        value |= (b & 0x7F) << shift
        if not (b & 0x80):
            return value, off
        shift += 7
        if shift > 35:
            raise ValueError("invalid uleb128")


def dex_class_descriptors(data: bytes) -> list[str]:
    if not data.startswith(b"dex\n"):
        raise ValueError("not a dex file")
    string_size, string_off = struct.unpack_from("<II", data, 0x38)
    type_size, type_off = struct.unpack_from("<II", data, 0x40)
    class_size, class_off = struct.unpack_from("<II", data, 0x60)
    offsets = [struct.unpack_from("<I", data, string_off + i * 4)[0] for i in range(string_size)]
    strings: list[str] = []
    for off in offsets:
        _, p = uleb(data, off)
        end = data.find(b"\0", p)
        if end < 0:
            raise ValueError("unterminated dex string")
        strings.append(data[p:end].decode("utf-8", "replace"))
    types = [strings[struct.unpack_from("<I", data, type_off + i * 4)[0]] for i in range(type_size)]
    out: list[str] = []
    for i in range(class_size):
        class_idx = struct.unpack_from("<I", data, class_off + i * 32)[0]
        if class_idx < len(types):
            out.append(types[class_idx])
    return out


def raven_roots(apk: Path) -> tuple[set[str], int, int]:
    descriptors: set[str] = set()
    with zipfile.ZipFile(apk) as zf:
        dex_names = [n for n in zf.namelist() if re.fullmatch(r"classes\d*\.dex", n)]
        for name in dex_names:
            for desc in dex_class_descriptors(zf.read(name)):
                if desc.startswith(PREFIX):
                    descriptors.add(desc)
    roots = {d.rsplit("/", 1)[-1].rstrip(";").split("$", 1)[0] for d in descriptors}
    return roots, len(descriptors), len(dex_names)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("apk", type=Path)
    parser.add_argument("--require-next-wave", action="store_true")
    args = parser.parse_args()
    if not args.apk.is_file():
        print(f"APK_RUNTIME_VERIFY_FAIL missing_apk={args.apk}", file=sys.stderr)
        return 2
    authority = json.loads(MANIFEST.read_text(encoding="utf-8"))
    required = set(authority["compiled_raven_roots"])
    roots, class_defs, dex_count = raven_roots(args.apk)
    missing = sorted(required - roots)
    if missing:
        print("APK_RUNTIME_VERIFY_FAIL baseline_organs_missing=" + ",".join(missing), file=sys.stderr)
        return 31
    if args.require_next_wave:
        next_required = set(authority["source_only_next_wave_not_in_this_apk"])
        next_missing = sorted(next_required - roots)
        if next_missing:
            print("APK_RUNTIME_VERIFY_FAIL next_wave_missing=" + ",".join(next_missing), file=sys.stderr)
            return 32
    print(
        "APK_RUNTIME_VERIFY_PASS "
        f"dex={dex_count} raven_class_defs={class_defs} raven_roots={len(roots)} "
        f"baseline_required={len(required)} next_wave_required={'yes' if args.require_next_wave else 'no'}"
    )
    extras = sorted(roots - required)
    if extras:
        print("APK_RUNTIME_NEW_ORGANS " + ",".join(extras))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
