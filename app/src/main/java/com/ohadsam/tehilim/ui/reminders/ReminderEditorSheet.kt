package com.ohadsam.tehilim.ui.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ohadsam.tehilim.AppContainer
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.bookmarks.Bookmark
import com.ohadsam.tehilim.domain.schedule.hebrewName
import com.ohadsam.tehilim.favorites.FavoritesState
import com.ohadsam.tehilim.reminders.AlertConfig
import com.ohadsam.tehilim.reminders.HolyDayRule
import com.ohadsam.tehilim.reminders.Reminder
import com.ohadsam.tehilim.reminders.ReminderTarget
import com.ohadsam.tehilim.reminders.Repeat
import com.ohadsam.tehilim.reminders.SnoozeConfig
import com.ohadsam.tehilim.ui.common.formatMoment
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

private enum class TargetKind { FAVORITE, PERSON, GROUP, MONTH_DAY, WEEKDAY, BOOKMARK }

private enum class RepeatKind { DAILY, WEEKLY, MONTHLY, YEARLY, ONCE }

private fun kindOf(t: ReminderTarget) = when (t) {
    is ReminderTarget.Favorite -> TargetKind.FAVORITE
    is ReminderTarget.Person -> TargetKind.PERSON
    is ReminderTarget.Group -> TargetKind.GROUP
    ReminderTarget.MonthDayToday -> TargetKind.MONTH_DAY
    ReminderTarget.WeekdayToday -> TargetKind.WEEKDAY
    is ReminderTarget.BookmarkTarget -> TargetKind.BOOKMARK
}

/** Full reminder editor: target, any number of times, repeat, date range, alerts, snooze, Shabbat/Yom Tov rule, live preview. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderEditorSheet(
    container: AppContainer,
    favorites: FavoritesState,
    bookmarks: List<Bookmark>,
    existing: Reminder?,
    onDismiss: () -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var kind by remember { mutableStateOf(existing?.target?.let(::kindOf) ?: TargetKind.MONTH_DAY) }
    var favoriteId by remember { mutableStateOf((existing?.target as? ReminderTarget.Favorite)?.id ?: (existing?.target as? ReminderTarget.Group)?.favoriteId ?: favorites.favorites.firstOrNull()?.id) }
    var personId by remember { mutableStateOf((existing?.target as? ReminderTarget.Person)?.id ?: (existing?.target as? ReminderTarget.Group)?.personId ?: favorites.persons.firstOrNull()?.id) }
    var bookmarkId by remember { mutableStateOf((existing?.target as? ReminderTarget.BookmarkTarget)?.id ?: bookmarks.firstOrNull()?.id) }
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var times by remember { mutableStateOf(existing?.times ?: listOf(LocalTime.of(8, 0))) }
    var repeatKind by remember {
        mutableStateOf(
            when (existing?.repeat) {
                is Repeat.Weekly -> RepeatKind.WEEKLY
                is Repeat.MonthlyHebrew -> RepeatKind.MONTHLY
                is Repeat.YearlyHebrew -> RepeatKind.YEARLY
                is Repeat.Once -> RepeatKind.ONCE
                else -> RepeatKind.DAILY
            },
        )
    }
    var weekDays by remember { mutableStateOf((existing?.repeat as? Repeat.Weekly)?.days ?: setOf(DayOfWeek.SUNDAY)) }
    var hebrewDay by remember { mutableStateOf(((existing?.repeat as? Repeat.MonthlyHebrew)?.day ?: 1).toString()) }
    var yearDay by remember { mutableStateOf(((existing?.repeat as? Repeat.YearlyHebrew)?.day ?: 1).toString()) }
    var yearMonth by remember { mutableStateOf((existing?.repeat as? Repeat.YearlyHebrew)?.month ?: 7) }
    var onceDate by remember { mutableStateOf((existing?.repeat as? Repeat.Once)?.date ?: LocalDate.now()) }
    var startDate by remember { mutableStateOf(existing?.startDate) }
    var endDate by remember { mutableStateOf(existing?.endDate) }
    var sound by remember { mutableStateOf(existing?.alert?.sound ?: true) }
    var vibrate by remember { mutableStateOf(existing?.alert?.vibrate ?: true) }
    var snoozeOn by remember { mutableStateOf(existing?.snooze?.enabled ?: true) }
    var snoozeMinutes by remember { mutableStateOf(existing?.snooze?.minutes ?: 10) }
    var snoozeMax by remember { mutableStateOf(existing?.snooze?.maxCount ?: 3) }
    var rule by remember { mutableStateOf(existing?.holyDayRule ?: HolyDayRule.BOTH) }

    var timeDialog by remember { mutableStateOf<Int?>(null) } // index being edited, -1 = new
    var dateDialog by remember { mutableStateOf<String?>(null) } // "start" | "end" | "once"

    val target: ReminderTarget? = when (kind) {
        TargetKind.FAVORITE -> favoriteId?.let { ReminderTarget.Favorite(it) }
        TargetKind.PERSON -> personId?.let { ReminderTarget.Person(it) }
        TargetKind.GROUP -> if (favoriteId != null && personId != null) ReminderTarget.Group(favoriteId!!, personId!!) else null
        TargetKind.MONTH_DAY -> ReminderTarget.MonthDayToday
        TargetKind.WEEKDAY -> ReminderTarget.WeekdayToday
        TargetKind.BOOKMARK -> bookmarkId?.let { ReminderTarget.BookmarkTarget(it) }
    }
    val repeat: Repeat? = when (repeatKind) {
        RepeatKind.DAILY -> Repeat.Daily
        RepeatKind.WEEKLY -> if (weekDays.isNotEmpty()) Repeat.Weekly(weekDays) else null
        RepeatKind.MONTHLY -> hebrewDay.toIntOrNull()?.takeIf { it in 1..30 }?.let { Repeat.MonthlyHebrew(it) }
        RepeatKind.YEARLY -> yearDay.toIntOrNull()?.takeIf { it in 1..com.ohadsam.tehilim.ui.common.maxDayOfHebrewMonth(yearMonth) }?.let { Repeat.YearlyHebrew(yearMonth, it) }
        RepeatKind.ONCE -> Repeat.Once(onceDate)
    }
    val draft: Reminder? = if (target != null && repeat != null && times.isNotEmpty()) {
        Reminder(
            id = existing?.id ?: 0, name = name.ifBlank { null }, target = target, times = times.sorted().distinct(), repeat = repeat,
            startDate = startDate, endDate = endDate, alert = AlertConfig(sound, vibrate),
            snooze = SnoozeConfig(snoozeOn, snoozeMinutes, snoozeMax), holyDayRule = rule, enabled = existing?.enabled ?: true,
        )
    } else {
        null
    }
    val rangeStart = startDate
    val rangeEnd = endDate
    val rangeValid = rangeStart == null || rangeEnd == null || !rangeEnd.isBefore(rangeStart)
    val preview = remember(draft) {
        draft?.let { d -> runCatching { container.reminderScheduler.planner().next(d, Instant.now(), limit = 5) }.getOrDefault(emptyList()) }.orEmpty()
    }
    val zone = remember { container.locations.resolved().place.zone }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(if (existing == null) R.string.rem_new else R.string.rem_edit), style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(name, { name = it }, singleLine = true, label = { Text(stringResource(R.string.rem_name_optional)) }, modifier = Modifier.fillMaxWidth())

            Section(R.string.rem_section_target)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TargetKind.entries.forEach { k ->
                    FilterChip(kind == k, { kind = k }, { Text(stringResource(targetLabel(k))) })
                }
            }
            if (kind == TargetKind.FAVORITE || kind == TargetKind.GROUP) {
                if (favorites.favorites.isEmpty()) Hint(R.string.rem_no_favorites)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    favorites.favorites.forEach { f -> FilterChip(favoriteId == f.id, { favoriteId = f.id }, { Text(f.name) }) }
                }
            }
            if (kind == TargetKind.PERSON || kind == TargetKind.GROUP) {
                if (favorites.persons.isEmpty()) Hint(R.string.rem_no_persons)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    favorites.persons.forEach { p -> FilterChip(personId == p.id, { personId = p.id }, { Text(p.name) }) }
                }
            }
            if (kind == TargetKind.BOOKMARK) {
                if (bookmarks.isEmpty()) Hint(R.string.rem_no_bookmarks)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    bookmarks.forEach { b -> FilterChip(bookmarkId == b.id, { bookmarkId = b.id }, { Text(b.label) }) }
                }
            }

            Section(R.string.rem_section_times)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                times.forEachIndexed { i, t ->
                    InputChip(
                        selected = false, onClick = { timeDialog = i },
                        label = { Text("%02d:%02d".format(t.hour, t.minute)) },
                        trailingIcon = if (times.size > 1) {
                            { Text("✕", modifier = Modifier.padding(start = 4.dp).clickableNoRipple { times = times - t }) }
                        } else {
                            null
                        },
                    )
                }
                OutlinedButton(onClick = { timeDialog = -1 }) { Text(stringResource(R.string.rem_add_time)) }
            }

            Section(R.string.rem_section_repeat)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RepeatKind.entries.forEach { r -> FilterChip(repeatKind == r, { repeatKind = r }, { Text(stringResource(repeatLabel(r))) }) }
            }
            when (repeatKind) {
                RepeatKind.WEEKLY -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
                        .forEach { d ->
                            FilterChip(d in weekDays, { weekDays = if (d in weekDays) weekDays - d else weekDays + d }, { Text(d.hebrewName.removePrefix("יום ")) })
                        }
                }
                RepeatKind.MONTHLY -> OutlinedTextField(
                    hebrewDay, { hebrewDay = it.take(2) }, singleLine = true, label = { Text(stringResource(R.string.rem_hebrew_day)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = hebrewDay.toIntOrNull() !in 1..30,
                )
                RepeatKind.YEARLY -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        yearDay, { yearDay = it.take(2) }, singleLine = true, label = { Text(stringResource(R.string.rem_hebrew_day)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
                    )
                    com.ohadsam.tehilim.ui.common.HebrewMonthPicker(yearMonth, stringResource(R.string.person_month), { yearMonth = it }, Modifier.weight(1.5f))
                }
                RepeatKind.ONCE -> OutlinedButton(onClick = { dateDialog = "once" }) { Text(onceDate.toString()) }
                RepeatKind.DAILY -> Unit
            }

            Section(R.string.rem_section_range)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { dateDialog = "start" }) { Text(startDate?.let { stringResource(R.string.rem_from, it.toString()) } ?: stringResource(R.string.rem_from_any)) }
                OutlinedButton(onClick = { dateDialog = "end" }) { Text(endDate?.let { stringResource(R.string.rem_until, it.toString()) } ?: stringResource(R.string.rem_until_any)) }
                if (startDate != null || endDate != null) TextButton(onClick = { startDate = null; endDate = null }) { Text(stringResource(R.string.rem_clear)) }
            }
            if (!rangeValid) Text(stringResource(R.string.rem_range_invalid), color = MaterialTheme.colorScheme.error)

            Section(R.string.rem_section_alert)
            ToggleRow(R.string.rem_sound, sound) { sound = it }
            ToggleRow(R.string.rem_vibrate, vibrate) { vibrate = it }
            Hint(R.string.rem_alert_hint)

            Section(R.string.rem_section_snooze)
            ToggleRow(R.string.rem_snooze_enable, snoozeOn) { snoozeOn = it }
            if (snoozeOn) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15, 30, 60).forEach { m -> FilterChip(snoozeMinutes == m, { snoozeMinutes = m }, { Text(stringResource(R.string.rem_minutes, m)) }) }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 3, 5).forEach { n -> FilterChip(snoozeMax == n, { snoozeMax = n }, { Text(stringResource(R.string.rem_max_times, n)) }) }
                }
            }

            Section(R.string.rem_section_holy)
            HolyDayRule.entries.forEach { r ->
                Row(Modifier.fillMaxWidth().clickableNoRipple { rule = r }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = rule == r, onClick = { rule = r })
                    Text(stringResource(holyLabel(r)))
                }
            }
            Hint(R.string.rem_holy_hint)

            Section(R.string.rem_section_preview)
            if (preview.isEmpty()) Hint(R.string.rem_preview_none)
            preview.forEach { Text("• " + formatMoment(Instant.ofEpochMilli(it.fireAtMillis), zone)) }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                Button(
                    enabled = draft != null && rangeValid,
                    onClick = {
                        val d = draft ?: return@Button
                        scope.launch {
                            val id = container.reminders.save(d)
                            container.reminderScheduler.schedule(d.copy(id = id))
                            sheet.hide()
                            onDismiss()
                        }
                    },
                ) { Text(stringResource(R.string.action_save)) }
            }
        }
    }

    timeDialog?.let { idx ->
        TimePickerDialog(
            initial = times.getOrNull(idx) ?: LocalTime.of(9, 0),
            onDismiss = { timeDialog = null },
            onConfirm = { t ->
                times = if (idx in times.indices) times.toMutableList().also { it[idx] = t } else times + t
                timeDialog = null
            },
        )
    }
    dateDialog?.let { which ->
        DatePickerDialogCompat(
            initial = when (which) { "start" -> startDate; "end" -> endDate; else -> onceDate },
            onDismiss = { dateDialog = null },
            onConfirm = { d ->
                when (which) { "start" -> startDate = d; "end" -> endDate = d; else -> onceDate = d }
                dateDialog = null
            },
        )
    }
}

@Composable
private fun Section(res: Int) = Text(stringResource(res), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)

@Composable
private fun Hint(res: Int) = Text(stringResource(res), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun ToggleRow(label: Int, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(label))
        Switch(checked = value, onCheckedChange = onChange)
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.then(
    Modifier.clickable(interactionSource = null, indication = null, onClick = onClick),
)

private fun targetLabel(k: TargetKind) = when (k) {
    TargetKind.FAVORITE -> R.string.rem_target_favorite
    TargetKind.PERSON -> R.string.rem_target_person
    TargetKind.GROUP -> R.string.rem_target_group
    TargetKind.MONTH_DAY -> R.string.rem_target_month
    TargetKind.WEEKDAY -> R.string.rem_target_weekday
    TargetKind.BOOKMARK -> R.string.rem_target_bookmark
}

private fun repeatLabel(r: RepeatKind) = when (r) {
    RepeatKind.DAILY -> R.string.rem_repeat_daily
    RepeatKind.WEEKLY -> R.string.rem_repeat_weekly
    RepeatKind.MONTHLY -> R.string.rem_repeat_monthly
    RepeatKind.YEARLY -> R.string.rem_repeat_yearly
    RepeatKind.ONCE -> R.string.rem_repeat_once
}

private fun holyLabel(r: HolyDayRule) = when (r) {
    HolyDayRule.BOTH -> R.string.rem_holy_both
    HolyDayRule.BEFORE -> R.string.rem_holy_before
    HolyDayRule.AFTER -> R.string.rem_holy_after
    HolyDayRule.SKIP -> R.string.rem_holy_skip
    HolyDayRule.NORMAL -> R.string.rem_holy_normal
}
