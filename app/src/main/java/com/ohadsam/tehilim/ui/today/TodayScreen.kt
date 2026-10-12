package com.ohadsam.tehilim.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.ohadsam.tehilim.ui.reader.CommentaryToggle
import com.ohadsam.tehilim.ui.reader.ReaderToolbar
import com.ohadsam.tehilim.ui.reader.ToolbarAction
import com.ohadsam.tehilim.ui.reader.rememberAutoScrollController
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    container: AppContainer,
    initialWeek: Boolean = false,
    onContinue: (Int) -> Unit = {},
    onOpenChapter: (chapter: Int, verse: Int) -> Unit = { _, _ -> },
    onOpenBookmarks: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenReminders: () -> Unit = {},
) {
    var mode by rememberSaveable { mutableStateOf(if (initialWeek) DayMode.WEEK else DayMode.MONTH) }
    var todayEpoch by rememberSaveable { mutableLongStateOf(container.locations.logicalToday().toEpochDay()) }
    var selectedEpoch by rememberSaveable { mutableLongStateOf(todayEpoch) }
    var startAtEnd by rememberSaveable { mutableStateOf(false) }
    var panelExpanded by rememberSaveable { mutableStateOf(container.readerPrefs.topPanelExpanded) }
    var focusMode by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val autoScroll = rememberAutoScrollController(container.autoScrollPrefs)

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

    val chapters = when (mode) {
        DayMode.MONTH -> PsalmSchedules.monthly(hebrew.day.coerceAtMost(hebrew.daysInMonth), hebrew.daysInMonth)
        DayMode.WEEK -> PsalmSchedules.weekly(selected.dayOfWeek)
    }
    val saidContext = when (mode) {
        DayMode.MONTH -> SaidContext.monthDay(hebrew.day)
        DayMode.WEEK -> SaidContext.weekday(selected)
    }
    val realToday = LocalDate.ofEpochDay(todayEpoch)
    val said = saidEntries.any { it.date == realToday && it.context == saidContext }
    val modeLabel = stringResource(if (mode == DayMode.MONTH) R.string.mode_month_day else R.string.mode_weekday)

    BookmarkHost(container) { hooks ->
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                if (!focusMode) {
                    ReaderToolbar(
                        title = hebrew.format(),
                        subtitle = "$modeLabel · ${selected.dayOfWeek.hebrewName} · ${gregorianFormat.format(selected)}",
                        expanded = panelExpanded,
                        onToggleExpanded = {
                            panelExpanded = !panelExpanded
                            container.readerPrefs.topPanelExpanded = panelExpanded
                        },
                        bookmarks = hooks,
                        onOpenBookmark = { onOpenChapter(it.chapter, it.verse) },
                        onOpenAllBookmarks = onOpenBookmarks,
                        extra = {
                            CommentaryToggle(settings.showCommentary) { container.readerPrefs.update { it.copy(showCommentary = !it.showCommentary) } }
                            if (!isToday) TextButton(onClick = { goTo(LocalDate.ofEpochDay(todayEpoch)) }) { Text(stringResource(R.string.action_today)) }
                        },
                        actions = listOf(
                            ToolbarAction(stringResource(R.string.tb_jump_date)) { showDatePicker = true },
                            ToolbarAction(stringResource(R.string.tb_open_chapters)) { onOpenChapter(container.readerPrefs.lastChapter.coerceAtLeast(1), 0) },
                            ToolbarAction(stringResource(R.string.tb_search), onOpenSearch),
                            ToolbarAction(stringResource(R.string.tb_reminders), onOpenReminders),
                            ToolbarAction(stringResource(R.string.tb_focus)) { focusMode = true },
                        ),
                    )
                    AnimatedVisibility(panelExpanded) {
                        Column {
                            val lastChapter = container.readerPrefs.lastChapter
                            if (lastChapter > 0) {
                                AssistChip(
                                    onClick = { onContinue(lastChapter) },
                                    label = { Text(stringResource(R.string.continue_reading, HebrewNumerals.toHebrew(lastChapter))) },
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                            PrefetchCard(container)
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                                DayMode.entries.forEachIndexed { i, m ->
                                    SegmentedButton(
                                        selected = mode == m,
                                        onClick = { mode = m },
                                        shape = SegmentedButtonDefaults.itemShape(i, DayMode.entries.size),
                                    ) { Text(stringResource(if (m == DayMode.MONTH) R.string.mode_month_day else R.string.mode_weekday)) }
                                }
                            }
                            Text(
                                stringResource(if (mode == DayMode.MONTH) R.string.strip_pick_month_day else R.string.strip_pick_weekday),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                            )
                            when (mode) {
                                DayMode.MONTH -> MonthStrip(hebrew, onPick = { goTo(it.toLocalDate()) })
                                DayMode.WEEK -> WeekStrip(selected, onPick = { goTo(it) })
                            }
                            HorizontalDivider(Modifier.padding(top = 4.dp))
                        }
                    }
                }
                // A new key (date + mode) rebuilds the pager so it starts on the right chapter.
                androidx.compose.runtime.key(selectedEpoch, mode, startAtEnd) {
                    DayReader(
                        modifier = Modifier.weight(1f),
                        bookmarks = hooks,
                        positions = container.positions,
                        positionKey = when (mode) {
                            DayMode.MONTH -> com.ohadsam.tehilim.data.prefs.PositionStore.key("m", 0, selected)
                            DayMode.WEEK -> com.ohadsam.tehilim.data.prefs.PositionStore.key("w", 0, selected)
                        },
                        repo = container.psalms,
                        chapters = chapters,
                        settings = settings,
                        startAtEnd = startAtEnd,
                        onPreviousDay = { goTo(selected.minusDays(1), atEnd = true) },
                        onNextDay = { goTo(selected.plusDays(1)) },
                        footer = { SaidBar(said, onToggle = { scope.launch { container.said.toggle(realToday, saidContext) } }) },
                        isStarred = { favState.starFor(it) != null },
                        onToggleStar = { ref -> scope.launch { container.favorites.toggleStar(ref, ref.title(), favState) } },
                        autoScroll = autoScroll,
                        focusMode = focusMode,
                        commentary = container.commentary,
                    )
                }
            }
            if (focusMode) {
                SmallFloatingActionButton(
                    onClick = { focusMode = false },
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ) { Icon(Icons.Filled.FullscreenExit, stringResource(R.string.tb_exit_focus)) }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = selected.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { goTo(java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate()) }
                    showDatePicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) { DatePicker(pickerState) }
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
