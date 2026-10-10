#!/usr/bin/env python3
"""Live check of the Sefaria Psalms API against the integrity rules in docs/SPEC.md (A3).

Run in CI (the development sandbox cannot reach sefaria.org). Fails (exit 1) on any violation; always prints
a machine-readable summary line `VERSE_COUNTS=<json>` that is used to build the golden verse-count table.
"""
import json
import re
import sys
import time
import urllib.request

BASE = "https://www.sefaria.org/api/texts/Psalms.{n}?lang=he&commentary=0&context=0"
HEBREW = re.compile("[א-ת]")
TAG = re.compile(r"<[^>]*>")


def flatten(x):
    if isinstance(x, list):
        for y in x:
            yield from flatten(y)
    elif x is None:
        yield ""
    else:
        yield str(x)


def fetch(n):
    last = None
    for attempt in range(4):
        try:
            req = urllib.request.Request(BASE.format(n=n), headers={"Accept": "application/json", "User-Agent": "tehilim-daily-probe"})
            with urllib.request.urlopen(req, timeout=30) as r:
                return json.load(r)
        except Exception as e:  # noqa: BLE001 - report and retry
            last = e
            time.sleep(2 * (attempt + 1))
    raise RuntimeError(f"Psalms.{n}: {last}")


def main():
    errors, counts, titles = [], {}, set()
    for n in range(1, 151):
        d = fetch(n)
        he = d.get("he")
        verses = [TAG.sub("", v).strip() for v in flatten(he)] if he else []
        if not verses:
            errors.append(f"Psalms.{n}: empty Hebrew")
            continue
        bad = [i + 1 for i, v in enumerate(verses) if not HEBREW.search(v)]
        if bad:
            errors.append(f"Psalms.{n}: verses without Hebrew letters: {bad[:5]}")
        # NB: Sefaria's "length" is the number of chapters in the book (150), not this chapter's verse count.
        counts[n] = len(verses)
        titles.add(d.get("heVersionTitle") or d.get("versionTitle") or "?")
        time.sleep(0.25)
    total = sum(counts.values())
    print("VERSE_COUNTS=" + json.dumps([counts.get(i) for i in range(1, 151)]))
    print("TOTAL_VERSES", total)
    print("VERSION_TITLES", sorted(titles))
    if counts.get(119) != 176:
        errors.append(f"Psalms.119 has {counts.get(119)} verses, expected 176")
    if errors:
        print("\n".join(errors))
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
