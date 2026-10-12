#!/usr/bin/env python3
"""Shape of the verse-aligned commentary data Sefaria returns for Psalms (live check, run in CI)."""
import json, sys, time, urllib.parse, urllib.request

BASE = "https://www.sefaria.org/api/"
WANT = ["Rashi", "Metzudat David", "Ibn Ezra", "Radak", "Malbim", "Meiri", "Steinsaltz", "Metzudat Zion"]


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


def flat(x, out=None):
    out = [] if out is None else out
    if isinstance(x, list):
        for i in x:
            flat(i, out)
    elif isinstance(x, str) and x.strip():
        out.append(x)
    return out


for path in ("links/Psalms.1?with_text=1", "links/Psalms.119?with_text=1", "links/Psalms.1.2?with_text=1"):
    d, size = get(path)
    print(f"== {path}: bytes={size} type={type(d).__name__}")
    if isinstance(d, dict):
        print("   ", str(d)[:200])
        continue
    cats = {}
    for l in d:
        cats[l.get("category")] = cats.get(l.get("category"), 0) + 1
    print("   categories:", cats)
    commentary = [l for l in d if l.get("category") == "Commentary"]
    print("   keys of a link:", sorted(commentary[0].keys()) if commentary else None)
    byname = {}
    for l in commentary:
        byname.setdefault((l.get("collectiveTitle") or {}).get("en"), []).append(l)
    for name in WANT:
        ls = byname.get(name, [])
        verses = sorted({l.get("anchorRef") for l in ls})
        print(f"   {name}: links={len(ls)} anchors={verses[:12]}{'...' if len(verses) > 12 else ''}")
        if ls:
            l = ls[0]
            he = l.get("he")
            print(f"      sample ref={l.get('ref')!r} anchorRef={l.get('anchorRef')!r} anchorVerse={l.get('anchorVerse')!r} sourceRef={l.get('sourceRef')!r} he_type={type(he).__name__} he={str(flat(he))[:140]}")
