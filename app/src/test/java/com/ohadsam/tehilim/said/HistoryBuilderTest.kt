package com.ohadsam.tehilim.said

import com.ohadsam.tehilim.favorites.Favorite
import com.ohadsam.tehilim.favorites.FavoritesState
import com.ohadsam.tehilim.domain.PsalmRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HistoryBuilderTest {
    private val today = LocalDate.of(2026, 10, 10) // Shabbat, 29 Tishrei 5787

    @Test
    fun coversThirtyOneDaysNewestFirstAndAlwaysListsMonthAndWeekday() {
        val h = HistoryBuilder.build(today, emptyList(), null)
        assertEquals(HISTORY_DAYS + 1, h.size)
        assertEquals(today, h.first().date)
        assertEquals(today.minusDays(HISTORY_DAYS.toLong()), h.last().date)
        assertTrue(h.all { d -> d.rows.take(2).map { it.context.type } == listOf(SaidType.MONTH_DAY, SaidType.WEEKDAY) })
        assertTrue(h.all { d -> d.rows.none { it.said } })
    }

    @Test
    fun marksMatchTheirDayAndContext() {
        val entries = listOf(
            SaidEntry(today, SaidContext.monthDay(29), 1),
            SaidEntry(today.minusDays(1), SaidContext.weekday(today.minusDays(1)), 2),
        )
        val h = HistoryBuilder.build(today, entries, null)
        val t = h[0].rows
        assertTrue(t[0].said)
        assertFalse(t[1].said)
        val y = h[1].rows
        assertFalse(y[0].said)
        assertTrue(y[1].said)
    }

    @Test
    fun favoritesAppearOnlyOnTheDayTheyWereSaidAndDeletedOnesAreLabelled() {
        val fav = Favorite(5, "ישועה", listOf(PsalmRef(20)))
        val state = FavoritesState(listOf(fav), emptyList())
        val entries = listOf(
            SaidEntry(today, SaidContext.favorite(5), 1),
            SaidEntry(today, SaidContext.favorite(99), 1),
        )
        val rows = HistoryBuilder.build(today, entries, state)[0].rows
        assertEquals(4, rows.size)
        assertTrue(rows.any { it.label.contains("ישועה") && it.said })
        assertTrue(rows.any { it.label.contains("שנמחק") })
        assertEquals(2, HistoryBuilder.build(today, entries, state)[1].rows.size)
    }

    @Test
    fun filterByStatusAndText() {
        val entries = listOf(SaidEntry(today, SaidContext.monthDay(29), 1))
        val h = HistoryBuilder.build(today, entries, null)
        val saidOnly = HistoryBuilder.filter(h, "", true)
        assertEquals(1, saidOnly.size)
        assertEquals(1, saidOnly[0].rows.size)
        assertEquals(h.size * 2 - 1, HistoryBuilder.filter(h, "", false).sumOf { it.rows.size })
        assertTrue(HistoryBuilder.filter(h, "שבת", null).isNotEmpty())
        assertTrue(HistoryBuilder.filter(h, "zzzz", null).isEmpty())
    }
}
