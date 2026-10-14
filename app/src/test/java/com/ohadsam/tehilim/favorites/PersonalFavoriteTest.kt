package com.ohadsam.tehilim.favorites

import com.ohadsam.tehilim.domain.PsalmRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PersonalFavoriteTest {
    private val person = Person(id = 5, name = "x", kind = PersonKind.REFUAH)

    @Test
    fun findsOnlyTheFavoriteLinkedExclusivelyToThePerson() {
        val shared = Favorite(id = 1, name = "shared", chapters = listOf(PsalmRef(20)), personIds = listOf(5, 6))
        val letters = Favorite(id = 2, name = "letters", chapters = listOf(PsalmRef(119, 1, 8)), personIds = listOf(5), nameSpec = NameSpec(listOf("א"), false))
        val own = Favorite(id = 3, name = "own", chapters = listOf(PsalmRef(1), PsalmRef(3)), personIds = listOf(5))
        val state = FavoritesState(listOf(shared, letters, own), listOf(person))
        assertEquals(own, state.personalFavorite(5))
        assertNull(state.personalFavorite(6))
        assertNull(FavoritesState(listOf(shared, letters), listOf(person)).personalFavorite(5))
    }
}
