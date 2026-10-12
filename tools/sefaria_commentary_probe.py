#!/usr/bin/env python3
"""Which commentaries does Sefaria have on Psalms, and what shape does a whole-chapter reply have? (live check, run in CI)"""
import json, sys, time, urllib.parse, urllib.request

BASE = "https://www.sefaria.org/api/"
CANDIDATES = ["Rashi", "Metzudat David", "Metzudat Zion", "Ibn Ezra", "Radak", "Malbim", "Meiri", "Ramban", "Rabbeinu Bahya", "Sforno",
              "Ibn Ezra, Psalms", "Radak on Psalms", "Ohr HaChaim", "Targum", "Midrash Tehillim", "Alshich", "Rashbam", "Abarbanel"]


def get(path):
    last = None
    for _ in range(3):
        try:
            with urllib.request.urlopen(urllib.request.Request(BASE + path, headers={"Accept": "application/json"}), timeout=40) as r:
                return json.loads(r.read().decode())
        except Exception as e:  # noqa: BLE001
            last = e
            time.sleep(1.5)
    return {"_error": str(last)}


def flat(x, out=None):
    out = [] if out is None else out
    if isinstance(x, list):
        for i in x:
            flat(i, out)
    elif isinstance(x, str) and x.strip():
        out.append(x)
    return out


def depth(x):
    d = 0
    while isinstance(x, list) and x:
        d += 1
        x = x[0]
    return d


print("== index of commentaries (related) for Psalms.1 ==")
rel = get("related/" + urllib.parse.quote("Psalms.1", safe=".")) 
if "_error" in rel:
    print("related failed", rel["_error"])
else:
    titles = {}
    for l in rel.get("links", []):
        if l.get("category") == "Commentary":
            t = l.get("collectiveTitle", {}).get("en") or l.get("index_title")
            titles[t] = titles.get(t, 0) + 1
    for t, n in sorted(titles.items(), key=lambda kv: -kv[1]):
        print(f"COMMENTARY {t}: {n} links on Psalms.1")

print("== per-chapter text API ==")
for name in CANDIDATES:
    for ch in (1, 23, 119):
        ref = f"{name} on Psalms {ch}"
        d = get("texts/" + urllib.parse.quote(ref, safe=",.:;-_") + "?lang=he&commentary=0&context=0")
        if "_error" in d:
            print(f"{ref!r}: ERROR {d['_error'][:80]}")
            continue
        if d.get("error"):
            print(f"{ref!r}: sefaria error {d['error'][:80]}")
            break
        he = d.get("he")
        segs = flat(he)
        nverses = len(he) if isinstance(he, list) else 0
        withc = sum(1 for v in he if flat(v)) if isinstance(he, list) else 0
        print(f"{ref!r}: shape depth={depth(he)} verses={nverses} withComments={withc} comments={len(segs)} version={d.get('heVersionTitle')!r} title={d.get('heTitle')!r}")
        if segs and ch == 1:
            print("   SAMPLE:", segs[0][:160].replace("\n", " "))
