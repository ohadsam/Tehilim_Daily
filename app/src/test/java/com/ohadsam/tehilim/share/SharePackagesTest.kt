package com.ohadsam.tehilim.share

import com.ohadsam.tehilim.domain.PsalmRef
import com.ohadsam.tehilim.favorites.Favorite
import com.ohadsam.tehilim.favorites.FavoritesState
import com.ohadsam.tehilim.favorites.Person
import com.ohadsam.tehilim.favorites.PersonKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SharePackagesTest {
    private val refuah = Person(id = 5, name = "פלוני", motherName = "פלונית", kind = PersonKind.REFUAH)
    private val own = Favorite(id = 1, name = "פלוני", chapters = listOf(PsalmRef(20), PsalmRef(91)), personIds = listOf(5))
    private val plain = Favorite(id = 2, name = "בטחון", chapters = listOf(PsalmRef(23)))
    private val state = FavoritesState(listOf(own, plain), listOf(refuah))

    @Test
    fun favoriteOfOnePersonIsSharedAsThatPersonsReading() {
        val pkg = own.toSharePackage(state)
        assertEquals(ShareKind.REFUAH, pkg.kind)
        assertEquals("פלוני", pkg.personName)
        assertEquals("פלונית", pkg.motherName)
        assertEquals("20,91", pkg.parts.single().chapters)
        assertNull(pkg.validate())
    }

    @Test
    fun plainFavoriteIsAPlainShare() {
        val pkg = plain.toSharePackage(state)
        assertEquals(ShareKind.FAVORITE, pkg.kind)
        assertNull(pkg.personName)
        assertNull(pkg.validate())
    }

    @Test
    fun personShareUsesTheirOwnListAndNeedsOne() {
        val pkg = refuah.toSharePackage(state)
        assertNotNull(pkg)
        assertEquals("תהילים לרפואת פלוני", pkg!!.title)
        assertEquals("20,91", pkg.parts.single().chapters)
        assertNull(refuah.toSharePackage(FavoritesState(emptyList(), listOf(refuah))))
    }

    @Test
    fun defaultTitles() {
        assertEquals("תהילים", defaultShareTitle(ShareKind.FAVORITE, "", false))
        assertEquals("חלוקת תהילים", defaultShareTitle(ShareKind.FAVORITE, "x", true))
        assertEquals("חלוקת תהילים לעילוי נשמת דוד", defaultShareTitle(ShareKind.ILUI_NISHMAT, " דוד ", true))
        assertEquals("תהילים", defaultShareTitle(ShareKind.REFUAH, "", false))
    }

    @Test
    fun importerMatchesExistingPersonAndDuplicates() {
        assertEquals(5L, ShareImporter.matchPerson(listOf(refuah), PersonKind.REFUAH, "פלוני", "פלונית")?.id)
        assertNull(ShareImporter.matchPerson(listOf(refuah), PersonKind.ILUI_NISHMAT, "פלוני", "פלונית"))
        assertNull(ShareImporter.matchPerson(listOf(refuah), PersonKind.REFUAH, "פלוני", null))
        assertEquals(1L, ShareImporter.findDuplicate(listOf(own, plain), "פלוני", listOf(PsalmRef(20), PsalmRef(91)), listOf(5))?.id)
        assertNull(ShareImporter.findDuplicate(listOf(own, plain), "פלוני", listOf(PsalmRef(20)), listOf(5)))
    }
}
