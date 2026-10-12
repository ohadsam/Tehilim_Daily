package com.ohadsam.tehilim.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.data.PsalmsRepository
import com.ohadsam.tehilim.data.prefs.ReaderSettings
import com.ohadsam.tehilim.domain.HebrewNumerals
import com.ohadsam.tehilim.domain.PsalmRef
import com.ohadsam.tehilim.domain.shortLabel
import com.ohadsam.tehilim.ui.bookmarks.BookmarkHooks
import kotlinx.coroutines.launch

/**
 * The chapters of one schedule day (or any list): a labelled, collapsible chapter strip, swipeable pages, and one compact
 * bottom row — previous | [footer, e.g. "said"] | next. Prev/next at the edges call [onPreviousDay]/[onNextDay]; when those
 * are null the button is simply not shown (never a broken button). [focusMode] hides everything except the text.
 */
@Composable
fun DayReader(
    repo: PsalmsRepository,
    chapters: List<PsalmRef>,
    settings: ReaderSettings,
    startAtEnd: Boolean,
    onPreviousDay: (() -> Unit)?,
    onNextDay: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onPageSettled: (Int) -> Unit = {},
    isStarred: (PsalmRef) -> Boolean = { false },
    onToggleStar: ((PsalmRef) -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    bookmarks: BookmarkHooks? = null,
    positions: com.ohadsam.tehilim.data.prefs.PositionStore? = null,
    positionKey: String? = null,
    headings: List<List<androidx.compose.ui.text.AnnotatedString>>? = null,
    autoScroll: AutoScrollController? = null,
    focusMode: Boolean = false,
    stripLabel: String? = null,
) {
    if (chapters.isEmpty()) return
    val saved = if (positions != null && positionKey != null) positions.get(positionKey) else 0
    val pager = rememberPagerState(
        initialPage = if (startAtEnd) chapters.lastIndex else saved.coerceIn(0, chapters.lastIndex),
        pageCount = { chapters.size },
    )
    val scope = rememberCoroutineScope()
    val current = pager.currentPage
    var stripOpen by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect {
            if (positions != null && positionKey != null) positions.set(positionKey, it)
            onPageSettled(it)
        }
    }

    val nextDay by androidx.compose.runtime.rememberUpdatedState(onNextDay)
    if (autoScroll != null) {
        androidx.compose.runtime.DisposableEffect(autoScroll, pager) {
            // Auto-scroll hands over to the next chapter, then to the next day, exactly like swiping would.
            autoScroll.advance = {
                val now = pager.currentPage
                when {
                    now < chapters.lastIndex -> { scope.launch { pager.animateScrollToPage(now + 1) }; true }
                    nextDay != null -> { nextDay?.invoke(); true }
                    else -> false
                }
            }
            onDispose { autoScroll.advance = null }
        }
        AutoScrollPlayback(autoScroll)
    }

    Column(modifier.fillMaxSize()) {
        if (chapters.size > 1 && !focusMode) {
            Row(
                Modifier.fillMaxWidth().clickable { stripOpen = !stripOpen }.padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    (stripLabel ?: stringResource(R.string.reader_strip_label)) + " · " +
                        stringResource(R.string.reader_position, HebrewNumerals.toHebrew(current + 1), HebrewNumerals.toHebrew(chapters.size)),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    if (stripOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = stringResource(if (stripOpen) R.string.action_collapse else R.string.action_expand),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (stripOpen) {
                ChipStrip(
                    labels = chapters.map { it.shortLabel() },
                    selectedIndex = current,
                    onSelect = { scope.launch { pager.animateScrollToPage(it) } },
                )
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            HorizontalPager(
                state = pager,
                modifier = Modifier.fillMaxSize(),
                key = { "$it:${chapters[it].key}" }, // index included: a name-letters reading can repeat a stanza
                beyondViewportPageCount = 1,
            ) { page ->
                val ref = chapters[page]
                ChapterPage(
                    repo, ref, settings,
                    starred = isStarred(ref),
                    onToggleStar = onToggleStar?.let { toggle -> { toggle(ref) } },
                    bookmarks = bookmarks,
                    headingLines = headings?.getOrNull(page).orEmpty(),
                    autoScroll = autoScroll,
                    active = page == pager.currentPage,
                )
            }
            if (autoScroll != null) {
                AutoScrollToolbar(autoScroll, settings.keepScreenOn, Modifier.align(Alignment.BottomStart))
            }
        }
        if (!focusMode) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val hasPrev = current > 0 || onPreviousDay != null
                val hasNext = current < chapters.lastIndex || onNextDay != null
                if (hasPrev) {
                    TextButton(onClick = {
                        if (current > 0) scope.launch { pager.animateScrollToPage(current - 1) } else onPreviousDay?.invoke()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = null)
                        Text(stringResource(if (current > 0) R.string.nav_previous_chapter else R.string.nav_previous_day), maxLines = 1)
                    }
                } else {
                    IconButton(onClick = {}, enabled = false, content = {})
                }
                footer?.invoke()
                if (hasNext) {
                    TextButton(onClick = {
                        if (current < chapters.lastIndex) scope.launch { pager.animateScrollToPage(current + 1) } else onNextDay?.invoke()
                    }) {
                        Text(stringResource(if (current < chapters.lastIndex) R.string.nav_next_chapter else R.string.nav_next_day), maxLines = 1)
                        Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null)
                    }
                } else {
                    IconButton(onClick = {}, enabled = false, content = {})
                }
            }
        }
    }
}
