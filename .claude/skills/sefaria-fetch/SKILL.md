---
name: sefaria-fetch
description: Verified Sefaria Psalms fetching behavior (URL, ref format, response shapes, pitfalls) taken from kodesh-app, plus the local-first caching contract. Use when touching data/sefaria, PsalmsRepository, or anything that fetches psalm text.
---
# Sefaria Psalms fetch (ported from ohadsam/kodesh-app)
- URL: `GET https://www.sefaria.org/api/texts/{ref}?lang=he&commentary=0&context=0` (v2). Ref `Psalms.N`; a verse range is `Psalms.119:1-96`. Use URL-path encoding that keeps `.` `:` `-` (equivalent of JS `encodeURI`).
- `he` shape is unstable: a string, a flat list, or list-of-lists (`he[0]` is a list). Flatten recursively, drop blanks. v3 fallback: `versions[]` entry with `language=="he"`.
- Sefaria may return the WHOLE chapter for a mid-chapter range: read `sections`/`toSections` for the true verse span, and slice locally if needed.
- Text contains HTML (`<b>`, `<i>`, `<small>`, `&nbsp;`, `&thinsp;`): clean to plain text/AnnotatedString; keep nikud; strip tags.
- **`length` in the response is the number of chapters in the book (150), NOT the verses of this chapter.** Never validate against it (found by the live probe; it would have rejected every chapter). Verse counts are validated against the golden table `PsalmVerseCounts` (2,527 verses total, captured from the `Sefaria probe` workflow; version "Miqra according to the Masorah").
- Always fetch the WHOLE chapter (`Psalms.N`) and slice ranges locally (`PsalmChapter.slice`); one cache row per chapter.
- Empty `he` or HTTP error ⇒ NOT cached, show a Hebrew error with Retry. Never persist empty/error bodies.
- Repository contract: `get(chapter, range?)` → Room hit ⇒ return; miss ⇒ API ⇒ store (key = canonical ref) ⇒ return; API failure with no row ⇒ typed error. Offer "prefetch all 150" (WorkManager, throttled ~350ms between calls, resumable) and "clear cache".
- Chapter 119 is split in the daily schedule as `119:1-96` and `119:97-176`.
- The sandbox cannot reach sefaria.org: verify refs on CI (a scheduled/manual "sefaria-probe" job) and add `// verified YYYY-MM-DD` next to each ref family.

## Audit against `sefaria-api` skill (copied from kodesh-app)
Checked: endpoint/params identical (`texts/Psalms.N?lang=he&commentary=0&context=0`); `he` string/flat/nested handled; v3 `versions[]` fallback;
empty `he` with HTTP 200 = failure; only non-empty text cached; serial prefetch with 350 ms delay; whole chapter fetched, ranges sliced locally
(avoids the "mid-chapter range returns whole chapter" pitfall); `length` not used. Live verification runs in CI (`sefaria-probe.yml`, both probes) since the sandbox blocks sefaria.org.

## Commentary on Psalms (verified live, see tools/sefaria_commentary_probe.py)
- Use `GET /api/v3/texts/{Name} on Psalms {N}?version=hebrew` (Name = Rashi, Metzudat David, Ibn Ezra, Radak, Malbim, Meiri, Steinsaltz). `versions[0].text` is indexed by verse (index+1); each entry is a list of comments (a plain string for Steinsaltz); empty verses are `[]`. Comments carry `<b>` for the opening words.
- Do NOT use v1 `texts/{Name} on Psalms N` (returns only the first verse section) or `links/Psalms.N?with_text=1` (4 MB, 504 on Psalms.119).
