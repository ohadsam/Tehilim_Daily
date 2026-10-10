package com.ohadsam.tehilim.ui.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ohadsam.tehilim.AppContainer
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.domain.HebrewNumerals
import com.ohadsam.tehilim.domain.PSALMS_COUNT
import com.ohadsam.tehilim.domain.PsalmRef
import com.ohadsam.tehilim.domain.title
import com.ohadsam.tehilim.favorites.FavoritesState
import com.ohadsam.tehilim.ui.bookmarks.BookmarkHost
import com.ohadsam.tehilim.ui.reader.ChapterPage
import com.ohadsam.tehilim.ui.reader.ChipStrip
import kotlinx.coroutines.launch

/** Parses "119", "קיט", "קי״ט" → chapter number in 1..150, or null. */
fun parseChapterQuery(query: String): Int? {
    val q = query.trim()
    val n = q.toIntOrNull() ?: HebrewNumerals.fromHebrew(q)
    return n?.takeIf { it in 1..PSALMS_COUNT }
}

/** All 150 chapters in order: swipe between them, jump via a letter grid or search, and see what was visited this session. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(container: AppContainer, initialChapter: Int = 1, initialVerse: Int = 0, onOpenSearch: () -> Unit = {}, onOpenBookmarks: () -> Unit = {}) {
    val pager = rememberPagerState(initialPage = (initialChapter - 1).coerceIn(0, PSALMS_COUNT - 1), pageCount = { PSALMS_COUNT })
    val scope = rememberCoroutineScope()
    val settings by container.readerPrefs.settings.collectAsState()
    var showPicker by remember { mutableStateOf(false) }
    val favState by container.favorites.state.collectAsState(initial = FavoritesState(emptyList(), emptyList()))
    // In-memory only: dies with this screen when the user leaves the tab (see docs/SPEC.md S3).
    var visited by remember { mutableStateOf(listOf<Int>()) }

    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { page ->
            val ch = page + 1
            container.readerPrefs.lastChapter = ch
            if (ch !in visited) visited = (visited + ch).sorted()
        }
    }

    BookmarkHost(container) { hooks ->
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.tab_browse), style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenBookmarks) {
                    Icon(Icons.Filled.Bookmarks, contentDescription = stringResource(R.string.bm_title))
                }
                IconButton(onClick = onOpenSearch) {
                    Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.browse_search_text))
                }
                FilledTonalButton(onClick = { showPicker = true }) {
                    Text(stringResource(R.string.browse_pick_chapter))
                }
            }
        }
        if (visited.size > 1) {
            ChipStrip(
                labels = visited.map { HebrewNumerals.toHebrew(it) },
                selectedIndex = visited.indexOf(pager.currentPage + 1),
                onSelect = { scope.launch { pager.animateScrollToPage(visited[it] - 1) } },
            )
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth(), key = { it }, beyondViewportPageCount = 1) { page ->
            val ref = PsalmRef(page + 1)
            ChapterPage(
                container.psalms, ref, settings,
                starred = favState.starFor(ref) != null,
                onToggleStar = { scope.launch { container.favorites.toggleStar(ref, ref.title(), favState) } },
                highlightVerse = if (ref.chapter == initialChapter) initialVerse else 0,
                bookmarks = hooks,
            )
        }
    }
    }

    if (showPicker) {
        ModalBottomSheet(onDismissRequest = { showPicker = false }) {
            ChapterPicker(
                current = pager.currentPage + 1,
                onPick = { ch ->
                    showPicker = false
                    scope.launch { pager.scrollToPage(ch - 1) }
                },
            )
        }
    }
}

@Composable
private fun ChapterPicker(current: Int, onPick: (Int) -> Unit) {
    var query by remember { mutableStateOf("") }
    val target = parseChapterQuery(query)
    Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.browse_search_hint)) },
            singleLine = true,
            isError = query.isNotBlank() && target == null,
            modifier = Modifier.fillMaxWidth(),
        )
        if (target != null) {
            FilledTonalButton(onClick = { onPick(target) }, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.browse_go_to, HebrewNumerals.toHebrew(target)))
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(64.dp),
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items((1..PSALMS_COUNT).toList()) { ch ->
                FilterChip(
                    selected = ch == current,
                    onClick = { onPick(ch) },
                    label = { Text(HebrewNumerals.toHebrew(ch), modifier = Modifier.fillMaxWidth()) },
                )
            }
        }
    }
}
