package com.ohadsam.tehilim.share

import com.ohadsam.tehilim.domain.PsalmRef
import com.ohadsam.tehilim.favorites.Favorite
import com.ohadsam.tehilim.favorites.FavoritesState
import com.ohadsam.tehilim.favorites.Person
import com.ohadsam.tehilim.favorites.PersonKind

/** Hebrew-only app: the label travels inside the share, so it is fixed text rather than a resource of the receiver's locale. */
fun partLabel(index: Int, total: Int) = "חלק $index מתוך $total"

fun csvOf(refs: List<PsalmRef>): String = refs.joinToString(",") { it.compact }

/** "תהילים לרפואת פלוני" / "…לעילוי נשמת פלוני" / "תהילים" — the default title when the user leaves it empty. */
fun defaultShareTitle(kind: ShareKind, personName: String, division: Boolean): String {
    val base = if (division) "חלוקת תהילים" else "תהילים"
    val name = personName.trim()
    return when {
        kind == ShareKind.REFUAH && name.isNotEmpty() -> "$base לרפואת $name"
        kind == ShareKind.ILUI_NISHMAT && name.isNotEmpty() -> "$base לעילוי נשמת $name"
        else -> base
    }
}

fun shareKindOf(kind: PersonKind): ShareKind = if (kind == PersonKind.REFUAH) ShareKind.REFUAH else ShareKind.ILUI_NISHMAT

/** An existing favorite as a one-part package; a favorite linked to exactly one person is sent as that person's reading. */
fun Favorite.toSharePackage(state: FavoritesState): SharePackage {
    val owner = personIds.singleOrNull()?.let(state::person)
    return owner?.toSharePackage(title = name, chapters = chapters)
        ?: SharePackage(title = name, parts = listOf(SharePart(chapters = csvOf(chapters))))
}

fun Person.toSharePackage(title: String, chapters: List<PsalmRef>): SharePackage = SharePackage(
    title = title,
    kind = shareKindOf(kind),
    personName = name,
    motherName = motherName,
    yahrzeitMonth = yahrzeitMonth,
    yahrzeitDay = yahrzeitDay,
    parts = listOf(SharePart(chapters = csvOf(chapters))),
)

/** The package to send for a person: their own chapter list when they have one, else the first linked favorite; null when none. */
fun Person.toSharePackage(state: FavoritesState): SharePackage? {
    val source = state.personalFavorite(id) ?: state.favoritesOf(this).firstOrNull() ?: return null
    return toSharePackage(defaultShareTitle(shareKindOf(kind), name, division = false), source.chapters)
}
