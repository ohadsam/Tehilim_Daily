---
name: sefaria-fetch
description: Verified Sefaria Psalms fetching behavior (URL, ref format, response shapes, pitfalls) taken from kodesh-app, plus the local-first caching contract. Use when touching data/sefaria, PsalmsRepository, or anything that fetches psalm text.
---
# Sefaria Psalms fetch (ported from ohadsam/kodesh-app)
- URL: `GET https://www.sefaria.org/api/texts/{ref}?lang=he&commentary=0&context=0` (v2). Ref `Psalms.N`; a verse range is `Psalms.119:1-88`. Use URL-path encoding that keeps `.` `:` `-` (equivalent of JS `encodeURI`).
- `he` shape is unstable: a string, a flat list, or list-of-lists (`he[0]` is a list). Flatten recursively, drop blanks. v3 fallback: `versions[]` entry with `language=="he"`.
- Sefaria may return the WHOLE chapter for a mid-chapter range: read `sections`/`toSections` for the true verse span, and slice locally if needed.
- Text contains HTML (`<b>`, `<i>`, `<small>`, `&nbsp;`, `&thinsp;`): clean to plain text/AnnotatedString; keep nikud; strip tags.
- Empty `he` or HTTP error ⇒ NOT cached, show a Hebrew error with Retry. Never persist empty/error bodies.
- Repository contract: `get(chapter, range?)` → Room hit ⇒ return; miss ⇒ API ⇒ store (key = canonical ref) ⇒ return; API failure with no row ⇒ typed error. Offer "prefetch all 150" (WorkManager, throttled ~350ms between calls, resumable) and "clear cache".
- Chapter 119 is split in the daily schedule as `119:1-88` and `119:89-176`.
- The sandbox cannot reach sefaria.org: verify refs on CI (a scheduled/manual "sefaria-probe" job) and add `// verified YYYY-MM-DD` next to each ref family.
