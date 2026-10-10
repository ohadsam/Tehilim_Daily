package com.ohadsam.tehilim.reminders

import com.ohadsam.tehilim.data.ReminderCodec
import com.ohadsam.tehilim.domain.PsalmRef
import com.ohadsam.tehilim.favorites.Favorite
import com.ohadsam.tehilim.favorites.FavoritesState
import com.ohadsam.tehilim.favorites.Person
import com.ohadsam.tehilim.favorites.PersonKind
import com.ohadsam.tehilim.said.SaidContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class ReminderCodecAndContentTest {
    private fun sample(target: ReminderTarget, repeat: Repeat) = Reminder(
        id = 4, name = "בוקר", target = target, times = listOf(LocalTime.of(21, 5), LocalTime.of(7, 30)), repeat = repeat,
        startDate = LocalDate.of(2026, 10, 1), endDate = LocalDate.of(2027, 1, 1),
        alert = AlertConfig(sound = false, vibrate = true), snooze = SnoozeConfig(true, 15, 2), holyDayRule = HolyDayRule.AFTER,
    )

    @Test
    fun everyTargetAndRepeatRoundTrips() {
        val targets = listOf(
            ReminderTarget.Favorite(3), ReminderTarget.Person(4), ReminderTarget.Group(5, 6),
            ReminderTarget.MonthDayToday, ReminderTarget.WeekdayToday, ReminderTarget.BookmarkTarget(9),
        )
        val repeats = listOf(
            Repeat.Daily, Repeat.Once(LocalDate.of(2026, 12, 25)),
            Repeat.Weekly(setOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY, DayOfWeek.FRIDAY)), Repeat.MonthlyHebrew(15),
        )
        for (t in targets) for (r in repeats) {
            val original = sample(t, r)
            val back = ReminderCodec.decode(ReminderCodec.encode(original))
            // times are stored sorted
            assertEquals(original.copy(times = original.times.sorted()), back)
        }
    }

    @Test
    fun corruptRowDecodesToNull() {
        val e = ReminderCodec.encode(sample(ReminderTarget.MonthDayToday, Repeat.Daily))
        assertNull(ReminderCodec.decode(e.copy(targetType = "???")))
        assertNull(ReminderCodec.decode(e.copy(holyRule = "NOPE")))
        assertNull(ReminderCodec.decode(e.copy(timesCsv = "25:99")))
    }

    @Test
    fun alertChannelKeyReflectsSoundAndVibration() {
        assertEquals("s1v0", AlertConfig(sound = true, vibrate = false).channelKey)
        assertEquals("s0v0", AlertConfig(sound = false, vibrate = false).channelKey)
    }

    private val fav = Favorite(2, "ישועה", listOf(PsalmRef(20), PsalmRef(21)), personIds = listOf(7))
    private val person = Person(7, "דוד", "שרה", PersonKind.REFUAH)
    private val state = FavoritesState(listOf(fav), listOf(person))
    private val friday = LocalDate.of(2026, 10, 9)
    private val shabbat = LocalDate.of(2026, 10, 10)

    @Test
    fun monthDayContentUsesTheIntendedDate() {
        val m = ReminderContent.build(
            sample(ReminderTarget.MonthDayToday, Repeat.Daily).copy(name = null), shabbat, friday, Shift.BEFORE_HOLY, state, emptyList(),
        )!!
        assertTrue(m.title.contains("כ״ט")) // 29 Tishrei 5787 is Saturday 10 Oct
        assertTrue(m.body.contains("שבת"))
        assertEquals(SaidContext.monthDay(29), m.saidContext)
        assertEquals("today", m.route)
    }

    @Test
    fun favoritePersonAndGroupTargets() {
        val f = ReminderContent.build(sample(ReminderTarget.Favorite(2), Repeat.Daily).copy(name = null), friday, friday, Shift.NONE, state, emptyList())!!
        assertEquals("ישועה", f.title)
        assertEquals("favorite/2", f.route)
        val p = ReminderContent.build(sample(ReminderTarget.Person(7), Repeat.Daily).copy(name = null), friday, friday, Shift.NONE, state, emptyList())!!
        assertTrue(p.title.contains("לרפואת דוד"))
        assertEquals(SaidContext.person(7), p.saidContext)
        val g = ReminderContent.build(sample(ReminderTarget.Group(2, 7), Repeat.Daily).copy(name = null), friday, friday, Shift.NONE, state, emptyList())!!
        assertTrue(g.title.contains("ישועה") && g.title.contains("דוד"))
    }

    @Test
    fun customNameOverridesTitleAndMissingTargetsReturnNull() {
        assertEquals("בוקר", ReminderContent.build(sample(ReminderTarget.Favorite(2), Repeat.Daily), friday, friday, Shift.NONE, state, emptyList())!!.title)
        assertNull(ReminderContent.build(sample(ReminderTarget.Favorite(99), Repeat.Daily), friday, friday, Shift.NONE, state, emptyList()))
        assertNotNull(ReminderContent.build(sample(ReminderTarget.WeekdayToday, Repeat.Daily), friday, friday, Shift.NONE, state, emptyList()))
    }
}
