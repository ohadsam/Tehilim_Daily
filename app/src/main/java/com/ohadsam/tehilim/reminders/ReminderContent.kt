package com.ohadsam.tehilim.reminders

import com.ohadsam.tehilim.bookmarks.Bookmark
import com.ohadsam.tehilim.calendar.HebrewCalendar
import com.ohadsam.tehilim.domain.HebrewNumerals
import com.ohadsam.tehilim.domain.schedule.PsalmSchedules
import com.ohadsam.tehilim.domain.schedule.hebrewName
import com.ohadsam.tehilim.favorites.FavoritesState
import com.ohadsam.tehilim.favorites.PersonKind
import com.ohadsam.tehilim.favorites.summarizeChapters
import com.ohadsam.tehilim.said.SaidContext
import java.time.LocalDate

/** [route] is an in-app navigation route opened by tapping the notification; [saidContext] is what "נאמר" marks. */
data class ReminderMessage(val title: String, val body: String, val route: String, val saidContext: SaidContext?)

object ReminderContent {
    /**
     * @param intendedDate the day whose chapters the reminder is for (differs from [fireDate] when it was shifted around Shabbat/Yom Tov)
     * @return null when the target no longer exists (e.g. the favorite was deleted)
     */
    fun build(
        r: Reminder,
        intendedDate: LocalDate,
        fireDate: LocalDate,
        shift: Shift,
        favorites: FavoritesState,
        bookmarks: List<Bookmark>,
    ): ReminderMessage? {
        val hebrew = HebrewCalendar.fromLocalDate(intendedDate)
        val prefix = when (shift) {
            Shift.NONE -> ""
            Shift.BEFORE_HOLY -> "לקראת שבת/חג · "
            Shift.AFTER_HOLY -> "אחרי שבת/חג · "
        }
        val msg = when (val t = r.target) {
            ReminderTarget.MonthDayToday -> {
                val chapters = PsalmSchedules.monthly(hebrew.day.coerceAtMost(hebrew.daysInMonth), hebrew.daysInMonth)
                ReminderMessage(
                    "תהילים ליום ${HebrewNumerals.toHebrew(hebrew.day)} בחודש",
                    summarizeChapters(chapters), "today", SaidContext.monthDay(hebrew.day),
                )
            }
            ReminderTarget.WeekdayToday -> ReminderMessage(
                "תהילים ל${intendedDate.dayOfWeek.hebrewName}",
                summarizeChapters(PsalmSchedules.weekly(intendedDate.dayOfWeek)), "today?mode=week", SaidContext.weekday(intendedDate),
            )
            is ReminderTarget.Favorite -> favorites.favorites.firstOrNull { it.id == t.id }?.let { f ->
                ReminderMessage(f.name, summarizeChapters(f.chapters), "favorite/${f.id}", SaidContext.favorite(f.id))
            }
            is ReminderTarget.Person -> favorites.person(t.id)?.let { p ->
                val chapters = favorites.favoritesOf(p).flatMap { it.chapters }.distinct()
                ReminderMessage(
                    (if (p.kind == PersonKind.REFUAH) "לרפואת " else "לעילוי נשמת ") + p.displayName(),
                    summarizeChapters(chapters), "person/${p.id}", SaidContext.person(p.id),
                )
            }
            is ReminderTarget.Group -> {
                val f = favorites.favorites.firstOrNull { it.id == t.favoriteId }
                val p = favorites.person(t.personId)
                if (f == null) {
                    null
                } else {
                    val who = p?.let { (if (it.kind == PersonKind.REFUAH) "לרפואת " else "לעילוי נשמת ") + it.displayName() }
                    ReminderMessage(listOfNotNull(f.name, who).joinToString(" · "), summarizeChapters(f.chapters), "favorite/${f.id}", SaidContext.favorite(f.id))
                }
            }
            is ReminderTarget.BookmarkTarget -> bookmarks.firstOrNull { it.id == t.id }?.let { b ->
                ReminderMessage(b.label, b.snippet, "browse?chapter=${b.chapter}&verse=${b.verse}", null)
            }
        } ?: return null
        val dateNote = if (intendedDate != fireDate && shift != Shift.NONE) " (עבור ${intendedDate.dayOfWeek.hebrewName})" else ""
        return msg.copy(title = r.name?.takeIf { it.isNotBlank() } ?: msg.title, body = prefix + msg.body + dateNote)
    }
}
