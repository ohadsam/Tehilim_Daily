package com.ohadsam.tehilim.domain

const val PSALMS_COUNT = 150

/** A chapter, or a verse range inside one (e.g. the two halves of chapter 119 in the monthly schedule). */
data class PsalmRef(val chapter: Int, val fromVerse: Int? = null, val toVerse: Int? = null) {
    init {
        require(chapter in 1..PSALMS_COUNT) { "chapter out of range: $chapter" }
        require((fromVerse == null) == (toVerse == null)) { "fromVerse/toVerse must both be set or both be null" }
        if (fromVerse != null && toVerse != null) require(fromVerse >= 1 && toVerse >= fromVerse) { "bad verse range" }
    }

    val isWhole: Boolean get() = fromVerse == null

    /** Canonical Sefaria-style reference, also usable as a stable key. */
    val key: String get() = if (isWhole) "Psalms.$chapter" else "Psalms.$chapter:$fromVerse-$toVerse"
}

data class Verse(val number: Int, val text: String)

/** A whole chapter exactly as stored locally (the unit of caching). */
data class PsalmChapter(
    val chapter: Int,
    val verses: List<Verse>,
    val versionTitle: String?,
    val fetchedAt: Long,
)

/** What the UI shows: a chapter or a slice of one. */
data class PsalmText(
    val ref: PsalmRef,
    val verses: List<Verse>,
    val versionTitle: String?,
    val fromCache: Boolean,
)

enum class PsalmErrorKind { OFFLINE, SERVER, NOT_FOUND, INVALID_DATA }

class PsalmException(val kind: PsalmErrorKind, message: String, cause: Throwable? = null) : Exception(message, cause)

/** Returns the verses of [ref] from this chapter; throws INVALID_DATA when the range does not fit. */
fun PsalmChapter.slice(ref: PsalmRef): List<Verse> {
    if (ref.isWhole) return verses
    val from = ref.fromVerse!!
    val to = ref.toVerse!!
    if (to > verses.size) {
        throw PsalmException(
            PsalmErrorKind.INVALID_DATA,
            "range ${ref.key} exceeds chapter ${chapter} length ${verses.size}",
        )
    }
    return verses.filter { it.number in from..to }
}
