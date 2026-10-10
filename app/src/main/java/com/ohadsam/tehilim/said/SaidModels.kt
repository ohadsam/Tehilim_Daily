package com.ohadsam.tehilim.said

import java.time.LocalDate

enum class SaidType { MONTH_DAY, WEEKDAY, FAVORITE, PERSON }

/**
 * What was said: the schedule day-of-month (id "1".."30"), a weekday (id "0".."6", Sunday = 0),
 * or a favorite / person (id = row id).
 */
data class SaidContext(val type: SaidType, val id: String) {
    companion object {
        fun monthDay(day: Int) = SaidContext(SaidType.MONTH_DAY, day.toString())

        fun weekday(date: LocalDate) = SaidContext(SaidType.WEEKDAY, (date.dayOfWeek.value % 7).toString())

        fun favorite(id: Long) = SaidContext(SaidType.FAVORITE, id.toString())

        fun person(id: Long) = SaidContext(SaidType.PERSON, id.toString())
    }
}

/** "On [date] the user marked [context] as said." */
data class SaidEntry(val date: LocalDate, val context: SaidContext, val markedAt: Long)

const val HISTORY_DAYS = 30
