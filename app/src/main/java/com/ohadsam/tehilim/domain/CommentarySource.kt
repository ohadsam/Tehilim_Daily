package com.ohadsam.tehilim.domain

/**
 * Commentaries on Psalms that Sefaria serves verse by verse (checked live with tools/sefaria_commentary_probe.py:
 * `GET /api/v3/texts/{title} on Psalms {chapter}?version=hebrew` → `versions[0].text[verse-1]` = list of comments).
 */
enum class CommentarySource(val key: String, val sefariaTitle: String, val hebrewName: String) {
    METZUDAT_DAVID("metzudat_david", "Metzudat David", "מצודת דוד"),
    RASHI("rashi", "Rashi", "רש״י"),
    IBN_EZRA("ibn_ezra", "Ibn Ezra", "אבן עזרא"),
    RADAK("radak", "Radak", "רד״ק"),
    MALBIM("malbim", "Malbim", "מלבי״ם"),
    MEIRI("meiri", "Meiri", "מאירי"),
    STEINSALTZ("steinsaltz", "Steinsaltz", "שטיינזלץ");

    companion object {
        val DEFAULT = METZUDAT_DAVID

        fun fromKey(key: String?): CommentarySource = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}
