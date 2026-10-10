package com.ohadsam.tehilim.ui.reader

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

/**
 * The chapters of one schedule day (or any list): chip strip, "פרק N מתוך M", swipeable pages and prev/next.
 * Prev/next at the edges call [onPreviousDay]/[onNextDay]; when those are null the button is simply not shown
 * (never a broken button — see CLAUDE.md landmines in kodesh-app).
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
) {
    if (chapters.isEmpty()) return
    val pager = rememberPagerState(initialPage = if (startAtEnd) chapters.lastIndex else 0, pageCount = { chapters.size })
    val scope = rememberCoroutineScope()
    val current = pager.currentPage

    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { onPageSettled(it) }
    }

    Column(modifier.fillMaxSize()) {
        if (chapters.size > 1) {
            ChipStrip(
                labels = chapters.map { it.shortLabel() },
                selectedIndex = current,
                onSelect = { scope.launch { pager.animateScrollToPage(it) } },
            )
            Text(
                stringResource(R.string.reader_position, HebrewNumerals.toHebrew(current + 1), HebrewNumerals.toHebrew(chapters.size)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        HorizontalPager(
            state = pager,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            key = { chapters[it].key },
            beyondViewportPageCount = 1,
        ) { page ->
            val ref = chapters[page]
            ChapterPage(repo, ref, settings, starred = isStarred(ref), onToggleStar = onToggleStar?.let { toggle -> { toggle(ref) } })
        }
        footer?.invoke()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
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
                    Text(stringResource(if (current > 0) R.string.nav_previous_chapter else R.string.nav_previous_day))
                }
            } else {
                IconButton(onClick = {}, enabled = false, content = {})
            }
            if (hasNext) {
                TextButton(onClick = {
                    if (current < chapters.lastIndex) scope.launch { pager.animateScrollToPage(current + 1) } else onNextDay?.invoke()
                }) {
                    Text(stringResource(if (current < chapters.lastIndex) R.string.nav_next_chapter else R.string.nav_next_day))
                    Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null)
                }
            }
        }
    }
}
