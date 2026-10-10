package com.ohadsam.tehilim.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.ohadsam.tehilim.AppContainer
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.calendar.HebrewCalendar
import com.ohadsam.tehilim.calendar.HebrewDate
import com.ohadsam.tehilim.domain.HebrewNumerals
import com.ohadsam.tehilim.domain.PSALMS_COUNT
import com.ohadsam.tehilim.domain.title
import com.ohadsam.tehilim.favorites.FavoritesState
import kotlinx.coroutines.launch
import com.ohadsam.tehilim.domain.schedule.PsalmSchedules
import com.ohadsam.tehilim.domain.schedule.hebrewName
import com.ohadsam.tehilim.ui.bookmarks.BookmarkHost
import com.ohadsam.tehilim.ui.reader.ChipStrip
import com.ohadsam.tehilim.said.SaidContext
import com.ohadsam.tehilim.ui.reader.DayReader
import com.ohadsam.tehilim.ui.reader.SaidBar
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class DayMode { MONTH, WEEK }

private val gregorianFormat = DateTimeFormatter.ofPattern("d 'ב'MMMM yyyy", Locale("he"))

/**
 * Landing screen: today's chapters by Hebrew day-of-month or by weekday, with free navigation to any other
 * date/day. Follows "today" across midnight as long as the user has not navigated away from it.
 */
@Composable
fun TodayScreen(container: AppContainer, initialWeek: Boolean = false, onContinue: (Int) -> Unit = {}) {
    var mode by rememberSaveable { mutableStateOf(if (initialWeek) DayMode.WEEK else DayMode.MONTH) }
    var todayEpoch by rememberSaveable { mutableLongStateOf(container.locations.logicalToday().toEpochDay()) }
    var selectedEpoch by rememberSaveable { mutableLongStateOf(todayEpoch) }
    var startAtEnd by rememberSaveable { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val now = container.locations.logicalToday().toEpochDay()
        if (now != todayEpoch) {
            if (selectedEpoch == todayEpoch) selectedEpoch = now // was following "today": keep following
            todayEpoch = now
        }
    }

    // The Hebrew day can turn over at sunset / nightfall while the screen is open, so re-check once a minute.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            val now = container.locations.logicalToday().toEpochDay()
            if (now != todayEpoch) {
                if (selectedEpoch == todayEpoch) selectedEpoch = now
                todayEpoch = now
            }
        }
    }
    val selected = LocalDate.ofEpochDay(selectedEpoch)
    val hebrew = HebrewCalendar.fromLocalDate(selected)
    val settings by container.readerPrefs.settings.collectAsState()
    val isToday = selectedEpoch == todayEpoch
    val favState by container.favorites.state.collectAsState(initial = FavoritesState(emptyList(), emptyList()))
    val scope = rememberCoroutineScope()
    val saidEntries by container.said.entries.collectAsState(initial = emptyList())

    fun goTo(date: LocalDate, atEnd: Boolean = false) {
        startAtEnd = atEnd
        selectedEpoch = date.toEpochDay()
    }

    Column(Modifier.fillMaxSize()) {
        Header(hebrew, selected, isToday, onToday = { goTo(LocalDate.ofEpochDay(todayEpoch)) })
        val lastChapter = container.readerPrefs.lastChapter
        if (lastChapter > 0) {
            AssistChip(
                onClick = { onContinue(lastChapter) },
                label = { Text(stringResource(R.string.continue_reading, HebrewNumerals.toHebrew(lastChapter))) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        PrefetchCard(container)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            DayMode.entries.forEachIndexed { i, m ->
                SegmentedButton(
                    selected = mode == m,
                    onClick = { mode = m },
                    shape = SegmentedButtonDefaults.itemShape(i, DayMode.entries.size),
                ) { Text(stringResource(if (m == DayMode.MONTH) R.string.mode_month_day else R.string.mode_weekday)) }
            }
        }
        when (mode) {
            DayMode.MONTH -> MonthStrip(hebrew, onPick = { goTo(it.toLocalDate()) })
            DayMode.WEEK -> WeekStrip(selected, onPick = { goTo(it) })
        }

        val chapters = when (mode) {
            DayMode.MONTH -> PsalmSchedules.monthly(hebrew.day.coerceAtMost(hebrew.daysInMonth), hebrew.daysInMonth)
            DayMode.WEEK -> PsalmSchedules.weekly(selected.dayOfWeek)
        }
        // A new key (date + mode) rebuilds the pager so it starts on the right chapter.
        val saidContext = when (mode) {
            DayMode.MONTH -> SaidContext.monthDay(hebrew.day)
            DayMode.WEEK -> SaidContext.weekday(selected)
        }
        val realToday = LocalDate.ofEpochDay(todayEpoch)
        val said = saidEntries.any { it.date == realToday && it.context == saidContext }
        androidx.compose.runtime.key(selectedEpoch, mode, startAtEnd) {
            BookmarkHost(container, Modifier.weight(1f)) { hooks ->
            DayReader(
                bookmarks = hooks,
                repo = container.psalms,
                chapters = chapters,
                settings = settings,
                startAtEnd = startAtEnd,
                onPreviousDay = { goTo(selected.minusDays(1), atEnd = true) },
                onNextDay = { goTo(selected.plusDays(1)) },
                footer = { SaidBar(said, onToggle = { scope.launch { container.said.toggle(realToday, saidContext) } }) },
                isStarred = { favState.starFor(it) != null },
                onToggleStar = { ref -> scope.launch { container.favorites.toggleStar(ref, ref.title(), favState) } },
            )
            }
        }
    }
}

@Composable
private fun Header(hebrew: HebrewDate, date: LocalDate, isToday: Boolean, onToday: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(hebrew.format(), style = MaterialTheme.typography.titleLarge)
            Text(
                "${date.dayOfWeek.hebrewName} · ${gregorianFormat.format(date)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!isToday) AssistChip(onClick = onToday, label = { Text(stringResource(R.string.action_today)) })
    }
}

/** Month navigation (‹ month year ›) over a strip of the month's days; day 29 shows "כ״ט + ל׳" in 29-day months. */
@Composable
private fun MonthStrip(current: HebrewDate, onPick: (HebrewDate) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { onPick(current.sameDayInAdjacentMonth(-1)) }) {
                Icon(Icons.AutoMirrored.Filled.NavigateBefore, stringResource(R.string.nav_previous_month))
            }
            Text(
                "${current.monthName} ${HebrewCalendar.yearLabel(current.year)}",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            IconButton(onClick = { onPick(current.sameDayInAdjacentMonth(1)) }) {
                Icon(Icons.AutoMirrored.Filled.NavigateNext, stringResource(R.string.nav_next_month))
            }
        }
        val length = current.daysInMonth
        ChipStrip(
            labels = (1..length).map { d ->
                if (length == 29 && d == 29) "${HebrewNumerals.toHebrew(29)} + ${HebrewNumerals.toHebrew(30)}" else HebrewNumerals.toHebrew(d)
            },
            selectedIndex = current.day - 1,
            onSelect = { onPick(current.copy(day = it + 1)) },
        )
    }
}

@Composable
private fun WeekStrip(selected: LocalDate, onPick: (LocalDate) -> Unit) {
    val sunday = selected.minusDays((selected.dayOfWeek.value % 7).toLong())
    val days = (0..6).map { sunday.plusDays(it.toLong()) }
    ChipStrip(
        labels = days.map { it.dayOfWeek.hebrewName.removePrefix("יום ") },
        selectedIndex = selected.dayOfWeek.value % 7,
        onSelect = { onPick(days[it]) },
    )
}

/** One-time offer to download every psalm so the app works fully offline; shows progress while running. */
@Composable
private fun PrefetchCard(container: AppContainer) {
    val state by container.prefetch.state.collectAsState()
    var cached by androidx.compose.runtime.remember { mutableStateOf(PSALMS_COUNT) }
    var dismissed by androidx.compose.runtime.remember { mutableStateOf(container.readerPrefs.prefetchPromptDismissed) }
    androidx.compose.runtime.LaunchedEffect(state == null) { cached = container.psalms.cachedChapters().size }
    val running = state
    if (running != null) {
        androidx.compose.material3.Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text(stringResource(R.string.prefetch_running, running.done, PSALMS_COUNT), style = MaterialTheme.typography.bodyMedium)
                androidx.compose.material3.LinearProgressIndicator(progress = { running.done / PSALMS_COUNT.toFloat() }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        }
    } else if (!dismissed && cached < PSALMS_COUNT) {
        androidx.compose.material3.Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text(stringResource(R.string.prefetch_offer, cached, PSALMS_COUNT), style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.material3.TextButton(onClick = {
                        container.readerPrefs.prefetchPromptDismissed = true
                        dismissed = true
                    }) { Text(stringResource(R.string.prefetch_later)) }
                    androidx.compose.material3.Button(onClick = { container.prefetch.start() }) { Text(stringResource(R.string.prefetch_download)) }
                }
            }
        }
    }
}
