package com.ohadsam.tehilim.data

import com.ohadsam.tehilim.data.db.FavoriteDao
import com.ohadsam.tehilim.data.db.FavoriteEntity
import com.ohadsam.tehilim.data.db.PersonEntity
import com.ohadsam.tehilim.domain.PsalmRef
import com.ohadsam.tehilim.domain.parsePsalmRef
import com.ohadsam.tehilim.favorites.Favorite
import com.ohadsam.tehilim.favorites.FavoritesState
import com.ohadsam.tehilim.favorites.Person
import com.ohadsam.tehilim.favorites.PersonKind
import com.ohadsam.tehilim.log.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Mapping between rows and domain objects, kept separate so it can be unit tested without Room. */
object FavoriteMapping {
    fun encodeChapters(refs: List<PsalmRef>): String = refs.joinToString(",") { it.compact }

    fun decodeChapters(csv: String): List<PsalmRef> = csv.split(",").mapNotNull { if (it.isBlank()) null else parsePsalmRef(it) }

    fun toEntity(f: Favorite) =
        FavoriteEntity(f.id, f.name, encodeChapters(f.chapters), f.colorIndex, f.sortOrder, f.createdAt, f.nameSpec?.encode())

    fun toEntity(p: Person) = PersonEntity(
        p.id, p.name, p.motherName, p.kind.name, p.yahrzeitDay, p.yahrzeitMonth, p.yahrzeitYear, p.note, p.createdAt,
    )

    fun toDomain(e: PersonEntity) = Person(
        e.id, e.name, e.motherName, runCatching { PersonKind.valueOf(e.kind) }.getOrDefault(PersonKind.REFUAH),
        e.yahrzeitDay, e.yahrzeitMonth, e.yahrzeitYear, e.note, e.createdAt,
    )

    fun toDomain(e: FavoriteEntity, personIds: List<Long>) =
        Favorite(e.id, e.name, decodeChapters(e.chaptersCsv), e.colorIndex, e.sortOrder, personIds, e.createdAt, com.ohadsam.tehilim.favorites.NameSpec.decode(e.nameSpec))
}

class FavoritesRepository(
    private val dao: FavoriteDao,
    private val logger: AppLogger,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    val state: Flow<FavoritesState> = combine(dao.favorites(), dao.persons(), dao.links()) { favs, persons, links ->
        val byFavorite = links.groupBy({ it.favoriteId }, { it.personId })
        FavoritesState(
            favorites = favs.map { FavoriteMapping.toDomain(it, byFavorite[it.id].orEmpty()) },
            persons = persons.map(FavoriteMapping::toDomain),
        )
    }

    suspend fun saveFavorite(favorite: Favorite): Long {
        require(favorite.chapters.isNotEmpty()) { "a favorite needs at least one chapter" }
        val entity = FavoriteMapping.toEntity(
            favorite.copy(
                createdAt = if (favorite.createdAt == 0L) clock() else favorite.createdAt,
                sortOrder = if (favorite.id == 0L) dao.nextSortOrder() else favorite.sortOrder,
            ),
        )
        val id = dao.saveFavorite(entity, favorite.personIds)
        logger.i(TAG, "favorite saved id=$id chapters=${favorite.chapters.size} persons=${favorite.personIds.size}")
        return id
    }

    suspend fun deleteFavorite(id: Long) {
        dao.removeFavorite(id)
        logger.i(TAG, "favorite deleted id=$id")
    }

    suspend fun savePerson(person: Person): Long {
        val entity = FavoriteMapping.toEntity(person.copy(createdAt = if (person.createdAt == 0L) clock() else person.createdAt))
        val id = if (person.id == 0L) dao.insertPerson(entity) else person.id.also { dao.updatePerson(entity) }
        logger.i(TAG, "person saved id=$id kind=${person.kind}") // never log the name itself
        return id
    }

    suspend fun deletePerson(id: Long) {
        dao.removePerson(id)
        logger.i(TAG, "person deleted id=$id")
    }

    /** ★ in the reader: adds / removes a plain single-chapter favorite. */
    suspend fun toggleStar(ref: PsalmRef, name: String, current: FavoritesState) {
        val existing = current.starFor(ref)
        if (existing != null) deleteFavorite(existing.id) else saveFavorite(Favorite(name = name, chapters = listOf(ref)))
    }

    private companion object {
        const val TAG = "Favorites"
    }
}
