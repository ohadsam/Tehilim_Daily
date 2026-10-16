package com.ohadsam.tehilim.share

import com.ohadsam.tehilim.domain.PSALMS_COUNT
import com.ohadsam.tehilim.domain.PsalmRef
import com.ohadsam.tehilim.domain.PsalmVerseCounts
import com.ohadsam.tehilim.favorites.ChapterSetParser
import com.ohadsam.tehilim.favorites.PersonKind
import kotlinx.serialization.Serializable

/** What the receiver's copy becomes: a plain favorite, or a reading for a person (refuah / ilui nishmat). */
enum class ShareKind {
    FAVORITE,
    REFUAH,
    ILUI_NISHMAT,
    ;

    val personKind: PersonKind? get() = when (this) {
        FAVORITE -> null
        REFUAH -> PersonKind.REFUAH
        ILUI_NISHMAT -> PersonKind.ILUI_NISHMAT
    }
}

/** One person's share of a division (or the single list of a plain list): [chapters] is the compact CSV, e.g. "20,21,119:1-96". */
@Serializable
data class SharePart(val label: String = "", val chapters: String)

/**
 * A shareable reading: one list ([parts] has one element) or a division among several people. It travels as a file or as a
 * text code (see [ShareCodec]) and needs no server. Only chapter references travel — never psalm text.
 */
@Serializable
data class SharePackage(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val id: String = "",
    val title: String,
    val kind: ShareKind = ShareKind.FAVORITE,
    val personName: String? = null,
    val motherName: String? = null,
    val yahrzeitMonth: Int? = null,
    val yahrzeitDay: Int? = null,
    val parts: List<SharePart>,
    val createdAt: Long = 0,
) {
    val isDivision: Boolean get() = parts.size > 1

    /** The same package reduced to one person's part (the "send only my part" share). */
    fun onlyPart(index: Int): SharePackage = copy(parts = listOf(parts[index]))

    companion object {
        const val FORMAT = "tehilim-daily-share"
        const val VERSION = 1
        const val MAX_PARTS = 200
        const val MAX_TITLE = 120
        const val MAX_LABEL = 60
        const val MAX_NAME = 80
    }
}

/** Chapter list of a part; [validate] guarantees every token is a real chapter / verse range. */
fun SharePart.refs(): List<PsalmRef> = ChapterSetParser.parse(chapters).refs

/**
 * Strict check of a package that came from outside. Returns the reason in English (for the log), or null when it is usable.
 * Verse ranges are checked against the known verse counts so a corrupted code can never point past the end of a chapter.
 */
fun SharePackage.validate(): String? {
    if (format != SharePackage.FORMAT) return "not a share package"
    if (version > SharePackage.VERSION) return "made by a newer version"
    if (title.isBlank() || title.length > SharePackage.MAX_TITLE) return "bad title"
    if (parts.isEmpty() || parts.size > SharePackage.MAX_PARTS) return "bad part count"
    if (kind != ShareKind.FAVORITE) {
        val name = personName?.trim().orEmpty()
        if (name.isEmpty() || name.length > SharePackage.MAX_NAME) return "bad person name"
        if ((motherName?.length ?: 0) > SharePackage.MAX_NAME) return "bad mother name"
    }
    yahrzeitMonth?.let { if (it !in 1..13) return "bad yahrzeit month" }
    yahrzeitDay?.let { if (it !in 1..30) return "bad yahrzeit day" }
    parts.forEach { p ->
        if (p.label.length > SharePackage.MAX_LABEL) return "bad label"
        val parsed = ChapterSetParser.parse(p.chapters)
        if (parsed.invalidTokens.isNotEmpty() || parsed.refs.isEmpty()) return "bad chapters"
        parsed.refs.forEach { r ->
            if (r.chapter !in 1..PSALMS_COUNT) return "chapter out of range"
            if (!r.isWhole && r.toVerse!! > PsalmVerseCounts.of(r.chapter)) return "verse out of range"
        }
    }
    return null
}
