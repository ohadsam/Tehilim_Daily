package com.ohadsam.tehilim.share

import com.ohadsam.tehilim.data.FavoritesRepository
import com.ohadsam.tehilim.domain.PsalmRef
import com.ohadsam.tehilim.favorites.Favorite
import com.ohadsam.tehilim.favorites.Person
import com.ohadsam.tehilim.favorites.PersonKind
import kotlinx.coroutines.flow.first

data class ImportResult(val created: Int, val skipped: Int, val firstFavoriteId: Long?)

/** Turns a received [SharePackage] into favorites (and a person for refuah / ilui nishmat), never overwriting what exists. */
class ShareImporter(private val favorites: FavoritesRepository) {
    /** Name of the favorite made from [part]: "title · label", or just the title when the part has no label. */
    fun favoriteName(pkg: SharePackage, part: SharePart): String =
        if (part.label.isBlank()) pkg.title else "${pkg.title} · ${part.label}"

    /** Loads only the chapters the person picked from the whole share, as one favorite named "title · [label]". */
    suspend fun importSelection(pkg: SharePackage, refs: List<PsalmRef>, label: String): ImportResult =
        import(pkg.copy(parts = listOf(SharePart(label, csvOf(refs)))), listOf(0))

    suspend fun import(pkg: SharePackage, partIndexes: List<Int>): ImportResult {
        val state = favorites.state.first()
        val personId = pkg.kind.personKind?.let { kind ->
            val name = pkg.personName.orEmpty().trim()
            val mother = pkg.motherName?.trim()?.ifEmpty { null }
            matchPerson(state.persons, kind, name, mother)?.id
                ?: favorites.savePerson(
                    Person(name = name, motherName = mother, kind = kind, yahrzeitDay = pkg.yahrzeitDay, yahrzeitMonth = pkg.yahrzeitMonth),
                )
        }
        var created = 0
        var skipped = 0
        var first: Long? = null
        val known = state.favorites.toMutableList()
        for (i in partIndexes.distinct()) {
            val part = pkg.parts.getOrNull(i) ?: continue
            val name = favoriteName(pkg, part)
            val refs = part.refs()
            val links = listOfNotNull(personId)
            val duplicate = findDuplicate(known, name, refs, links)
            if (duplicate != null) {
                skipped++
                if (first == null) first = duplicate.id
                continue
            }
            val id = favorites.saveFavorite(Favorite(name = name, chapters = refs, personIds = links))
            known += Favorite(id = id, name = name, chapters = refs, personIds = links)
            created++
            if (first == null) first = id
        }
        return ImportResult(created, skipped, first)
    }

    companion object {
        /** The same person (kind + name + mother's name) the user already has, so a second share for them does not create a duplicate. */
        fun matchPerson(persons: List<Person>, kind: PersonKind, name: String, mother: String?): Person? =
            persons.firstOrNull { it.kind == kind && it.name == name && it.motherName == mother }

        /** A favorite that is exactly what the share would create — importing the same file twice must not double the list. */
        fun findDuplicate(known: List<Favorite>, name: String, refs: List<PsalmRef>, personIds: List<Long>): Favorite? =
            known.firstOrNull { it.name == name && it.chapters == refs && it.personIds == personIds }
    }
}
