#!/usr/bin/env python3
"""Shape of whole-chapter commentary replies for Psalms (live check, run in CI)."""
import json, time, urllib.parse, urllib.request

BASE = "https://www.sefaria.org/api/"
NAMES = ["Rashi", "Metzudat David", "Ibn Ezra", "Radak", "Malbim", "Meiri", "Steinsaltz"]


def get(path):
    last = None
    for _ in range(3):
        try:
            with urllib.request.urlopen(urllib.request.Request(BASE + path, headers={"Accept": "application/json"}), timeout=60) as r:
                raw = r.read()
                return json.loads(raw.decode()), len(raw)
        except Exception as e:  # noqa: BLE001
            last = e
            time.sleep(2)
    return {"_error": str(last)}, 0


def shape(x, depth=0):
    if isinstance(x, list):
        return "[" + ",".join(shape(i, depth + 1) for i in x[:12]) + ("…" if len(x) > 12 else "") + "]"
    if isinstance(x, str):
        return "s%d" % len(x)
    return type(x).__name__


for name in NAMES:
    for ch in (1, 2, 119):
        q = urllib.parse.quote(f"{name} on Psalms {ch}", safe="")
        for label, path in (("v1", f"texts/{q}?lang=he&commentary=0&context=0"), ("v3", f"v3/texts/{q}?version=hebrew")):
            d, size = get(path)
            if "_error" in d:
                print(f"{name} {ch} {label}: ERROR {d['_error'][:70]}")
                continue
            if label == "v1":
                print(f"{name} {ch} v1 bytes={size} sections={d.get('sections')} toSections={d.get('toSections')} textDepth={d.get('textDepth')} length={d.get('length')} he={shape(d.get('he'))}")
            else:
                vs = d.get("versions") or []
                print(f"{name} {ch} v3 bytes={size} versions={len(vs)} " + " | ".join(f"{v.get('versionTitle')!r}:{v.get('language')}:{shape(v.get('text'))}" for v in vs[:3]) + f" err={d.get('error')}")
