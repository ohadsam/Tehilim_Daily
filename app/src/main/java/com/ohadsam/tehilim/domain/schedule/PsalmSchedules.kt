package com.ohadsam.tehilim.domain.schedule

import com.ohadsam.tehilim.domain.PSALMS_COUNT
import com.ohadsam.tehilim.domain.PsalmRef
import java.time.DayOfWeek

/**
 * The three ways of dividing Tehilim. Data only — covered by golden tests (every chapter exactly once).
 * Monthly: the standard 30-day division; chapter 119 is split into two days (verses 1–88 / 89–176).
 * Weekly: Sun 1–29, Mon 30–50, Tue 51–72, Wed 73–89, Thu 90–106, Fri 107–119, Shabbat 120–150.
 */
object PsalmSchedules {
    private fun chapters(range: IntRange): List<PsalmRef> = range.map { PsalmRef(it) }

    private val monthlyRanges: List<IntRange> = listOf(
        1..9, 10..17, 18..22, 23..28, 29..34, 35..38, 39..43, 44..48, 49..54, 55..59,
        60..65, 66..68, 69..71, 72..76, 77..78, 79..82, 83..87, 88..89, 90..96, 97..103,
        104..105, 106..107, 108..112, 113..118, 119..119, 119..119, 120..134, 135..139, 140..144, 145..150,
    )

    /** Index 0 = day 1 of the Hebrew month … index 29 = day 30. */
    val MONTHLY: List<List<PsalmRef>> = monthlyRanges.mapIndexed { i, r ->
        when (i + 1) {
            25 -> listOf(PsalmRef(119, 1, 88))
            26 -> listOf(PsalmRef(119, 89, 176))
            else -> chapters(r)
        }
    }

    private val weeklyRanges = listOf(1..29, 30..50, 51..72, 73..89, 90..106, 107..119, 120..150)

    /** Index 0 = Sunday … 6 = Shabbat. */
    val WEEKLY: List<List<PsalmRef>> = weeklyRanges.map(::chapters)

    val SEQUENTIAL: List<PsalmRef> = chapters(1..PSALMS_COUNT)

    /** Chapters to read on day [dayOfMonth] (1..30). In a 29-day month the 29th also carries the 30th's chapters. */
    fun monthly(dayOfMonth: Int, monthLength: Int): List<PsalmRef> {
        require(monthLength in 29..30) { "monthLength must be 29 or 30" }
        require(dayOfMonth in 1..monthLength) { "dayOfMonth out of range: $dayOfMonth" }
        val base = MONTHLY[dayOfMonth - 1]
        return if (monthLength == 29 && dayOfMonth == 29) base + MONTHLY[29] else base
    }

    fun weekly(day: DayOfWeek): List<PsalmRef> = WEEKLY[day.value % 7] // Monday=1 … Sunday=7 → Sunday=0
}

val DayOfWeek.hebrewName: String
    get() = when (this) {
        DayOfWeek.SUNDAY -> "יום ראשון"
        DayOfWeek.MONDAY -> "יום שני"
        DayOfWeek.TUESDAY -> "יום שלישי"
        DayOfWeek.WEDNESDAY -> "יום רביעי"
        DayOfWeek.THURSDAY -> "יום חמישי"
        DayOfWeek.FRIDAY -> "יום שישי"
        DayOfWeek.SATURDAY -> "שבת קודש"
    }
