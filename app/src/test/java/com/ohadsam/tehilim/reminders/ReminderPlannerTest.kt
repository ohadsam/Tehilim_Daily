package com.ohadsam.tehilim.reminders

import com.ohadsam.tehilim.calendar.TzeitMethod
import com.ohadsam.tehilim.calendar.ZmanimHolyTimes
import com.ohadsam.tehilim.location.Places
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderPlannerTest {
    private val place = Places.JERUSALEM
    private val zone: ZoneId = place.zone
    private val holy = ZmanimHolyTimes(place, TzeitMethod.DEGREES_8_5)
    private val planner = ReminderPlanner(zone, holy)

    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int): Instant = ZonedDateTime.of(y, m, d, h, min, 0, 0, zone).toInstant()

    private fun reminder(rule: HolyDayRule, repeat: Repeat = Repeat.Daily, vararg times: LocalTime) =
        Reminder(target = ReminderTarget.MonthDayToday, times = times.toList().ifEmpty { listOf(LocalTime.of(10, 0)) }, repeat = repeat, holyDayRule = rule)

    private fun local(o: Occurrence) = Instant.ofEpochMilli(o.fireAtMillis).atZone(zone)

    @Test
    fun ordinaryWeekdayFiresAtTheChosenTime() {
        val o = planner.next(reminder(HolyDayRule.BOTH), at(2026, 10, 11, 8, 0)).single() // Sunday
        assertEquals(at(2026, 10, 11, 10, 0).toEpochMilli(), o.fireAtMillis)
        assertEquals(Shift.NONE, o.shift)
        assertEquals(LocalDate.of(2026, 10, 11), o.intendedDate)
    }

    @Test
    fun shabbatMorningMovesBeforeCandleLightingMinusThirty() {
        val o = planner.next(reminder(HolyDayRule.BEFORE, Repeat.Once(LocalDate.of(2026, 10, 10))), at(2026, 10, 8, 0, 0)).first() // Shabbat 10:00 -> Friday
        val l = local(o)
        assertEquals(DayOfWeek.FRIDAY, l.dayOfWeek)
        assertEquals(Shift.BEFORE_HOLY, o.shift)
        assertEquals(LocalDate.of(2026, 10, 10), o.intendedDate) // still Shabbat's content
        val candle = holy.candleLighting(LocalDate.of(2026, 10, 9))!!
        assertEquals(candle.minus(Duration.ofMinutes(30)).toEpochMilli(), o.fireAtMillis)
        assertTrue(l.hour in 15..17)
    }

    @Test
    fun shabbatMorningMovesAfterHavdalahPlusThirty() {
        val o = planner.next(reminder(HolyDayRule.AFTER, Repeat.Once(LocalDate.of(2026, 10, 10))), at(2026, 10, 8, 0, 0)).first()
        val l = local(o)
        assertEquals(DayOfWeek.SATURDAY, l.dayOfWeek)
        assertEquals(Shift.AFTER_HOLY, o.shift)
        assertEquals(holy.havdalah(LocalDate.of(2026, 10, 10))!!.plus(Duration.ofMinutes(30)).toEpochMilli(), o.fireAtMillis)
        assertTrue(l.hour in 18..20)
    }

    @Test
    fun bothGivesTwoOccurrencesAndSkipGivesNone() {
        val both = planner.next(reminder(HolyDayRule.BOTH, Repeat.Once(LocalDate.of(2026, 10, 10))), at(2026, 10, 8, 0, 0), limit = 5)
        assertEquals(listOf(Shift.BEFORE_HOLY, Shift.AFTER_HOLY), both.map { it.shift })
        assertTrue(planner.next(reminder(HolyDayRule.SKIP, Repeat.Once(LocalDate.of(2026, 10, 10))), at(2026, 10, 9, 0, 0), limit = 5).isEmpty())
    }

    @Test
    fun normalRuleStillFiresOnShabbat() {
        val o = planner.next(reminder(HolyDayRule.NORMAL, Repeat.Once(LocalDate.of(2026, 10, 10))), at(2026, 10, 9, 0, 0)).single()
        assertEquals(at(2026, 10, 10, 10, 0).toEpochMilli(), o.fireAtMillis)
    }

    @Test
    fun fridayEveningInsideTheBufferIsMovedEarlier() {
        val candle = holy.candleLighting(LocalDate.of(2026, 10, 9))!!
        val tenBefore = candle.minus(Duration.ofMinutes(10)).atZone(zone)
        val r = reminder(HolyDayRule.BEFORE, Repeat.Once(LocalDate.of(2026, 10, 9)), LocalTime.of(tenBefore.hour, tenBefore.minute))
        val o = planner.next(r, at(2026, 10, 9, 0, 0)).single()
        assertEquals(candle.minus(Duration.ofMinutes(30)).toEpochMilli(), o.fireAtMillis)
    }

    @Test
    fun yomTovAndShabbatChainUsesFirstEveAndLastHavdalah() {
        // Rosh Hashana 5787: Sat 12 Sep 2026 (1 Tishrei) and Sun 13 Sep, followed by a regular Monday.
        val before = planner.next(reminder(HolyDayRule.BEFORE, Repeat.Once(LocalDate.of(2026, 9, 13))), at(2026, 9, 10, 0, 0)).single()
        assertEquals(DayOfWeek.FRIDAY, local(before).dayOfWeek)
        val after = planner.next(reminder(HolyDayRule.AFTER, Repeat.Once(LocalDate.of(2026, 9, 12))), at(2026, 9, 10, 0, 0)).single()
        assertEquals(LocalDate.of(2026, 9, 13), local(after).toLocalDate()) // after the LAST day of the chain
    }

    @Test
    fun rangesWeeklyAndHebrewMonthly() {
        val weekly = reminder(HolyDayRule.NORMAL, Repeat.Weekly(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)))
            .copy(startDate = LocalDate.of(2026, 10, 12), endDate = LocalDate.of(2026, 10, 19))
        val all = planner.next(weekly, at(2026, 10, 1, 0, 0), limit = 10)
        assertEquals(listOf(12, 14, 19), all.map { local(it).dayOfMonth })

        val monthly = reminder(HolyDayRule.NORMAL, Repeat.MonthlyHebrew(30))
        // 29 Tishrei 5787 = 10 Oct 2026 is the last day of a 30-day month; 30 Tishrei is 11 Oct 2026.
        assertEquals(LocalDate.of(2026, 10, 11), local(planner.next(monthly, at(2026, 10, 10, 12, 0)).single()).toLocalDate())
    }

    @Test
    fun disabledOrExpiredRemindersHaveNoOccurrences() {
        assertTrue(planner.next(reminder(HolyDayRule.NORMAL).copy(enabled = false), at(2026, 1, 1, 0, 0)).isEmpty())
        assertTrue(planner.next(reminder(HolyDayRule.NORMAL).copy(endDate = LocalDate.of(2026, 1, 1)), at(2026, 6, 1, 0, 0)).isEmpty())
    }

    @Test
    fun multipleTimesPerDayAreAllScheduledInOrder() {
        val r = reminder(HolyDayRule.NORMAL, Repeat.Daily, LocalTime.of(9, 0), LocalTime.of(21, 0))
        val list = planner.next(r, at(2026, 10, 11, 0, 0), limit = 3)
        assertEquals(listOf(9, 21, 9), list.map { local(it).hour })
    }

    @Test
    fun yearlyHebrewDateFiresOnTheNextOccurrence() {
        val r = reminder(HolyDayRule.NORMAL, Repeat.YearlyHebrew(7, 29)) // 29 Tishrei
        val first = planner.next(r, at(2026, 10, 11, 12, 0)).single()
        assertEquals(com.ohadsam.tehilim.calendar.HebrewDate(5788, 7, 29).toLocalDate(), local(first).toLocalDate())
    }

    @Test
    fun adarYahrzeitMovesToAdarIIInALeapYearAndBackToAdarOtherwise() {
        val adar = reminder(HolyDayRule.NORMAL, Repeat.YearlyHebrew(12, 10))
        // 5787 is a leap year → 10 Adar II 5787
        assertEquals(com.ohadsam.tehilim.calendar.HebrewDate(5787, 13, 10).toLocalDate(), local(planner.next(adar, at(2026, 10, 11, 12, 0)).single()).toLocalDate())
        // 5788 is not a leap year → 10 Adar 5788
        val after = at(2027, 4, 1, 0, 0)
        assertEquals(com.ohadsam.tehilim.calendar.HebrewDate(5788, 12, 10).toLocalDate(), local(planner.next(adar, after).single()).toLocalDate())
        // a yahrzeit stored in Adar II falls in plain Adar in a non-leap year
        val adar2 = reminder(HolyDayRule.NORMAL, Repeat.YearlyHebrew(13, 10))
        assertEquals(com.ohadsam.tehilim.calendar.HebrewDate(5788, 12, 10).toLocalDate(), local(planner.next(adar2, after).single()).toLocalDate())
    }
}
