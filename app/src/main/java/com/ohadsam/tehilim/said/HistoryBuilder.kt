package com.ohadsam.tehilim.said

import com.ohadsam.tehilim.calendar.HebrewCalendar
import com.ohadsam.tehilim.domain.HebrewNumerals
import com.ohadsam.tehilim.domain.schedule.hebrewName
import com.ohadsam.tehilim.favorites.FavoritesState
import java.time.LocalDate

data class HistoryRow(val context: SaidContext, val label: String, val said: Boolean)

data class DayHistory(val date: LocalDate, val hebrewLabel: String, val rows: List<HistoryRow>)

/** Builds the "was it said?" overview, newest day first. Always lists the day's month-day and weekday, plus anything else marked. */
object HistoryBuilder {
    fun build(today: LocalDate, entries: List<SaidEntry>, favorites: FavoritesState?, days: Int = HISTORY_DAYS): List<DayHistory> {
        val byDate = entries.groupBy { it.date }
        return (0..days).map { offset ->
            val date = today.minusDays(offset.toLong())
            val hebrew = HebrewCalendar.fromLocalDate(date)
            val saidThatDay = byDate[date].orEmpty().map { it.context }.toSet()
            val month = SaidContext.monthDay(hebrew.day)
            val weekday = SaidContext.weekday(date)
            val rows = mutableListOf(
                HistoryRow(month, "יום בחודש · ${HebrewNumerals.toHebrew(hebrew.day)}", month in saidThatDay),
                HistoryRow(weekday, "יום בשבוע · ${date.dayOfWeek.hebrewName}", weekday in saidThatDay),
            )
            saidThatDay.filter { it.type == SaidType.FAVORITE || it.type == SaidType.PERSON }
                .sortedBy { it.type.ordinal }
                .forEach { ctx -> rows += HistoryRow(ctx, labelOf(ctx, favorites), true) }
            DayHistory(date, "${date.dayOfWeek.hebrewName} · ${hebrew.format()}", rows)
        }
    }

    private fun labelOf(ctx: SaidContext, state: FavoritesState?): String {
        val id = ctx.id.toLongOrNull()
        return when (ctx.type) {
            SaidType.FAVORITE -> "מועדף · " + (id?.let { i -> state?.favorites?.firstOrNull { it.id == i }?.name } ?: "מועדף שנמחק")
            SaidType.PERSON -> "שם · " + (id?.let { i -> state?.person(i)?.name } ?: "שם שנמחק")
            else -> ctx.id
        }
    }

    /** Free-text filter over the date header and row labels; [onlyStatus] null = all, true = said, false = not said. */
    fun filter(history: List<DayHistory>, query: String, onlyStatus: Boolean?): List<DayHistory> {
        val q = query.trim()
        return history.mapNotNull { day ->
            val dayMatches = q.isNotEmpty() && (day.hebrewLabel.contains(q) || day.date.toString().contains(q))
            val rows = day.rows.filter { r ->
                (onlyStatus == null || r.said == onlyStatus) && (q.isEmpty() || dayMatches || r.label.contains(q))
            }
            if (rows.isEmpty()) null else day.copy(rows = rows)
        }
    }
}
