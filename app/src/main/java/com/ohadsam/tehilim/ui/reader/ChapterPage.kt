package com.ohadsam.tehilim.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf

import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.data.PsalmsRepository
import com.ohadsam.tehilim.data.prefs.ReaderSettings
import com.ohadsam.tehilim.domain.HebrewNumerals
import com.ohadsam.tehilim.domain.TextDisplay
import com.ohadsam.tehilim.ui.theme.ReaderFont
import com.ohadsam.tehilim.domain.PsalmErrorKind
import com.ohadsam.tehilim.domain.PsalmRef
import com.ohadsam.tehilim.domain.PsalmText
import com.ohadsam.tehilim.domain.title
import com.ohadsam.tehilim.ui.bookmarks.BookmarkHooks
import com.ohadsam.tehilim.ui.favorites.FavoriteColors
import com.ohadsam.tehilim.ui.theme.ReaderTextStyle

/** One chapter (or chapter slice): heading, verses, and loading / error-with-retry states. */
@Composable
fun ChapterPage(
    repo: PsalmsRepository,
    ref: PsalmRef,
    settings: ReaderSettings,
    modifier: Modifier = Modifier,
    starred: Boolean = false,
    onToggleStar: (() -> Unit)? = null,
    highlightVerse: Int = 0,
    bookmarks: BookmarkHooks? = null,
    headingLines: List<androidx.compose.ui.text.AnnotatedString> = emptyList(),
    autoScroll: AutoScrollController? = null,
    /** False for the neighbouring pages a pager keeps composed: only the visible one drives auto-scroll and bookmarks. */
    active: Boolean = true,
    commentary: com.ohadsam.tehilim.data.CommentaryRepository? = null,
) {
    var retry by remember(ref) { mutableIntStateOf(0) }
    val state by rememberPsalm(repo, ref, retry)
    Box(modifier.fillMaxSize()) {
        when (val s = state) {
            LoadState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            is LoadState.Error -> ErrorBox(s.kind, onRetry = { retry++ }, Modifier.align(Alignment.Center))
            is LoadState.Success -> ChapterText(ref, s.text, settings, starred, onToggleStar, highlightVerse, bookmarks, headingLines, autoScroll, active, commentary)
        }
    }
}

@Composable
private fun ChapterText(
    ref: PsalmRef,
    text: PsalmText,
    settings: ReaderSettings,
    starred: Boolean,
    onToggleStar: (() -> Unit)?,
    highlightVerse: Int,
    bookmarks: BookmarkHooks?,
    headingLines: List<androidx.compose.ui.text.AnnotatedString>,
    autoScroll: AutoScrollController?,
    active: Boolean,
    commentaryRepo: com.ohadsam.tehilim.data.CommentaryRepository?,
) {
    val shown: (String) -> String = { TextDisplay.apply(it, settings.showNikud, settings.showTaamim) }
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(settings.keepScreenOn) {
        view.keepScreenOn = settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }
    val marks = remember(bookmarks, ref.chapter) { bookmarks?.forChapter?.invoke(ref.chapter).orEmpty() }
    val listState = rememberLazyListState()
    val commentarySource = com.ohadsam.tehilim.domain.CommentarySource.fromKey(settings.commentaryKey)
    var commentaryRetry by remember(ref) { mutableIntStateOf(0) }
    val commentary by rememberCommentary(commentaryRepo, ref.chapter, commentarySource, settings.showCommentary, commentaryRetry)
    LaunchedEffect(highlightVerse, text) {
        val i = text.verses.indexOfFirst { it.number == highlightVerse }
        if (highlightVerse > 0 && i >= 0) listState.animateScrollToItem(i + 1) // +1: the title is item 0
    }
    if (active && autoScroll != null) {
        androidx.compose.runtime.DisposableEffect(listState, autoScroll) {
            autoScroll.attach(listState)
            onDispose { autoScroll.detach(listState) }
        }
    }
    if (active && bookmarks != null) {
        androidx.compose.runtime.DisposableEffect(listState, bookmarks, text) {
            // The verse nearest the top of the screen: "add a bookmark where I am" needs no precise long-press.
            bookmarks.visibleVerse.provider = {
                val key = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key is Int && it.offset > -it.size / 3 }?.key
                text.verses.firstOrNull { it.number == key }?.let { ref.chapter to it }
            }
            onDispose { bookmarks.visibleVerse.provider = null }
        }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().pauseAutoScrollOnDrag(autoScroll),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (headingLines.isNotEmpty()) {
            item(key = "name-heading") {
                // The name (or "נשמה") shown above its stanza with the stanza's letter(s) highlighted.
                Column(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    headingLines.forEach { line ->
                        Text(line, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
        item(key = "title") {
            Box(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text(
                    ref.title(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
                    textAlign = TextAlign.Center,
                )
                if (onToggleStar != null) {
                    IconButton(onClick = onToggleStar, modifier = Modifier.align(Alignment.CenterEnd)) {
                        Icon(
                            if (starred) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = stringResource(if (starred) R.string.action_unstar else R.string.action_star),
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }
        if (settings.showCommentary) {
            item(key = "commentary-status") {
                CommentaryStatus(commentary, commentarySource, onRetry = { commentaryRetry++ })
            }
        }
        items(text.verses, key = { it.number }) { v ->
            val marked = v.number == highlightVerse
            val mark = marks[v.number]
            Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth()
                    .background(if (marked) MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent)
                    .then(
                        if (bookmarks != null) {
                            Modifier.combinedClickable(onClick = {}, onLongClick = { bookmarks.onVerseLongPress(ref.chapter, v) })
                        } else {
                            Modifier
                        },
                    ),
                verticalAlignment = Alignment.Top,
            ) {
                if (mark != null) {
                    Icon(
                        Icons.Filled.Bookmark,
                        contentDescription = stringResource(R.string.bm_marker),
                        tint = FavoriteColors[mark.colorIndex.mod(FavoriteColors.size)],
                        modifier = Modifier.padding(top = 4.dp, end = 4.dp).size(18.dp).clickable { bookmarks?.onMarkerClick?.invoke(mark) },
                    )
                }
                if (settings.showVerseNumbers) {
                    Text(
                        HebrewNumerals.toHebrew(v.number, punctuation = false),
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = (settings.fontSizeSp * 0.6f).sp,
                        modifier = Modifier.width((settings.fontSizeSp * 1.7f).dp).padding(top = 4.dp),
                    )
                }
                Text(
                    shown(v.text),
                    style = ReaderTextStyle.copy(
                        fontFamily = ReaderFont.fromKey(settings.fontKey).family,
                        fontSize = settings.fontSizeSp.sp,
                        lineHeight = (settings.fontSizeSp * settings.lineHeight).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
            }
            val comments = (commentary as? CommentaryState.Loaded)?.byVerse?.get(v.number)
            if (settings.showCommentary && !comments.isNullOrEmpty()) {
                val size = (settings.fontSizeSp * COMMENTARY_SCALE).coerceAtLeast(MIN_COMMENTARY_SP)
                // Same start inset as the verse text so the commentary sits right under the verse it explains.
                val inset = if (settings.showVerseNumbers) (settings.fontSizeSp * 1.7f).dp else 0.dp
                comments.forEach { c ->
                    Text(
                        commentaryAnnotated(c, shown),
                        style = ReaderTextStyle.copy(
                            fontFamily = ReaderFont.fromKey(settings.fontKey).family,
                            fontSize = size.sp,
                            lineHeight = (size * COMMENTARY_LINE).sp,
                        ),
                        color = if (settings.commentaryColor != 0) androidx.compose.ui.graphics.Color(settings.commentaryColor) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().padding(start = inset, top = 2.dp, bottom = 4.dp),
                    )
                }
            }
            }
        }
        item(key = "source") {
            Text(
                text.versionTitle?.let { stringResource(R.string.reader_source, it) } ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ErrorBox(kind: PsalmErrorKind, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(
                when (kind) {
                    PsalmErrorKind.OFFLINE -> R.string.error_offline
                    PsalmErrorKind.SERVER -> R.string.error_server
                    PsalmErrorKind.NOT_FOUND -> R.string.error_not_found
                    PsalmErrorKind.INVALID_DATA -> R.string.error_invalid
                },
            ),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
    }
}

private const val COMMENTARY_SCALE = 0.72f
private const val MIN_COMMENTARY_SP = 12f
private const val COMMENTARY_LINE = 1.5f
