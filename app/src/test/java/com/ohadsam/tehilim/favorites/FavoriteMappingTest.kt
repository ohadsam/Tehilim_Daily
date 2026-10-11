package com.ohadsam.tehilim.favorites

import com.ohadsam.tehilim.data.FavoriteMapping
import com.ohadsam.tehilim.domain.PsalmRef
import org.junit.Assert.assertEquals
import org.junit.Test

class FavoriteMappingTest {
    @Test
    fun favoriteRoundTripsThroughEntity() {
        val f = Favorite(7, "שלום", listOf(PsalmRef(20), PsalmRef(119, 1, 8), PsalmRef(91)), 3, 2, listOf(4L), 99)
        val back = FavoriteMapping.toDomain(FavoriteMapping.toEntity(f), listOf(4L))
        assertEquals(f, back)
    }

    @Test
    fun nameSpecSurvivesTheRoundTrip() {
        val f = Favorite(1, "אותיות", listOf(PsalmRef(119, 1, 8)), nameSpec = NameSpec(listOf("דוד", "יעקב"), true))
        assertEquals(f, FavoriteMapping.toDomain(FavoriteMapping.toEntity(f), emptyList()))
    }

    @Test
    fun personRoundTripsAndUnknownKindFallsBack() {
        val p = Person(3, "דוד", "שרה", PersonKind.ILUI_NISHMAT, 15, 1, 5780, "n", 5)
        val e = FavoriteMapping.toEntity(p)
        assertEquals(p, FavoriteMapping.toDomain(e))
        assertEquals(PersonKind.REFUAH, FavoriteMapping.toDomain(e.copy(kind = "???")).kind)
    }

    @Test
    fun starLookupOnlyMatchesPlainSingleChapterFavorites() {
        val star = Favorite(1, "פרק כג", listOf(PsalmRef(23)))
        val linked = Favorite(2, "כג", listOf(PsalmRef(23)), personIds = listOf(1))
        val state = FavoritesState(listOf(linked, star), emptyList())
        assertEquals(star, state.starFor(PsalmRef(23)))
    }
}
