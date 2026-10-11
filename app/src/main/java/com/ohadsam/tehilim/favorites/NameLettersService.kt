package com.ohadsam.tehilim.favorites

import com.ohadsam.tehilim.data.FavoritesRepository
import com.ohadsam.tehilim.data.RemindersRepository
import com.ohadsam.tehilim.reminders.HolyDayRule
import com.ohadsam.tehilim.reminders.Reminder
import com.ohadsam.tehilim.reminders.ReminderScheduler
import com.ohadsam.tehilim.reminders.ReminderTarget
import com.ohadsam.tehilim.reminders.Repeat
import java.time.LocalTime

data class YahrzeitDate(val month: Int, val day: Int)

/** Creates everything a "name letters" reading needs: the person, the favorite, and (optionally) the yearly yahrzeit reminder. */
class NameLettersService(
    private val favorites: FavoritesRepository,
    private val reminders: RemindersRepository,
    private val scheduler: ReminderScheduler,
) {
    /** @return the id of the new favorite. [yahrzeit] with [reminderTime] creates the yearly reminder that opens the reading. */
    suspend fun save(
        spec: NameSpec,
        existing: Person?,
        kind: PersonKind,
        motherName: String?,
        yahrzeit: YahrzeitDate?,
        reminderTime: LocalTime?,
        favoriteName: String,
    ): Long {
        val full = spec.fullName
        val person = (existing ?: Person(name = full, kind = kind)).copy(
            name = full.ifBlank { existing?.name.orEmpty() },
            kind = kind,
            motherName = motherName?.trim()?.ifBlank { null },
            yahrzeitDay = yahrzeit?.day ?: existing?.yahrzeitDay,
            yahrzeitMonth = yahrzeit?.month ?: existing?.yahrzeitMonth,
        )
        val personId = favorites.savePerson(person)
        val favoriteId = favorites.saveFavorite(
            Favorite(name = favoriteName, chapters = NameLetterPlan.refs(spec), personIds = listOf(personId), nameSpec = spec),
        )
        if (yahrzeit != null && reminderTime != null) {
            val id = reminders.save(
                Reminder(
                    target = ReminderTarget.Group(favoriteId, personId), times = listOf(reminderTime),
                    repeat = Repeat.YearlyHebrew(yahrzeit.month, yahrzeit.day), holyDayRule = HolyDayRule.BOTH,
                ),
            )
            reminders.get(id)?.let { scheduler.schedule(it) }
        }
        return favoriteId
    }
}
