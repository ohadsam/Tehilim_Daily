package com.ohadsam.tehilim.favorites

import com.ohadsam.tehilim.domain.PsalmRef

enum class PersonKind { REFUAH, ILUI_NISHMAT }

/** [yahrzeitMonth]/[yahrzeitDay] use HebrewCalendar numbering (Nisan = 1 … Adar = 12, Adar II = 13). Year 0 = unknown. */
data class Person(
    val id: Long = 0,
    val name: String,
    val motherName: String? = null,
    val kind: PersonKind,
    val yahrzeitDay: Int? = null,
    val yahrzeitMonth: Int? = null,
    val yahrzeitYear: Int = 0,
    val note: String? = null,
    val createdAt: Long = 0,
) {
    /** "פלוני בן פלונית" — the form used in the prayer header. */
    fun displayName(): String = if (motherName.isNullOrBlank()) name else "$name ${if (kind == PersonKind.REFUAH) "בן/בת" else "בן/בת"} $motherName"
}

data class Favorite(
    val id: Long = 0,
    val name: String,
    val chapters: List<PsalmRef>,
    val colorIndex: Int = 0,
    val sortOrder: Int = 0,
    val personIds: List<Long> = emptyList(),
    val createdAt: Long = 0,
)

data class FavoritesState(val favorites: List<Favorite>, val persons: List<Person>) {
    fun person(id: Long): Person? = persons.firstOrNull { it.id == id }

    fun personsOf(f: Favorite): List<Person> = f.personIds.mapNotNull(::person)

    fun favoritesOf(p: Person): List<Favorite> = favorites.filter { p.id in it.personIds }

    /** A single-chapter favorite created by the ★ toggle in the reader. */
    fun starFor(ref: PsalmRef): Favorite? = favorites.firstOrNull { it.chapters == listOf(ref) && it.personIds.isEmpty() }
}
