package com.ohadsam.tehilim.ui.bookmarks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ohadsam.tehilim.AppContainer
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.bookmarks.Bookmark
import kotlinx.coroutines.launch

/**
 * Wraps a reader: long-press on a verse adds a bookmark (with Undo), long-press on an already bookmarked verse or
 * a tap on its marker opens the editor. Provides the snackbar for feedback.
 */
@Composable
fun BookmarkHost(container: AppContainer, modifier: Modifier = Modifier, content: @Composable (BookmarkHooks) -> Unit) {
    val all by container.bookmarks.bookmarks.collectAsState(initial = emptyList())
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<Bookmark?>(null) }
    val savedMsg = stringResource(R.string.bm_saved)
    val undo = stringResource(R.string.action_undo)
    val deletedMsg = stringResource(R.string.bm_cleared)

    val hooks = remember(all) {
        val byChapter = all.groupBy { it.chapter }
        BookmarkHooks(
            forChapter = { ch -> byChapter[ch].orEmpty().associateBy { it.verse } },
            onVerseLongPress = { ch, verse ->
                val existing = byChapter[ch].orEmpty().firstOrNull { it.verse == verse.number }
                if (existing != null) {
                    editing = existing
                } else {
                    scope.launch {
                        val created = container.bookmarks.add(ch, verse.number, verse.text)
                        if (snackbar.showSnackbar(savedMsg, undo, duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) {
                            // The id returned by add() is the row id, so Undo deletes exactly this bookmark.
                            container.bookmarks.delete(created.id)
                        }
                    }
                }
            },
            onMarkerClick = { editing = it },
        )
    }

    Box(modifier.fillMaxSize()) {
        content(hooks)
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
    editing?.let { b ->
        BookmarkEditorSheet(
            bookmark = b,
            onSave = { scope.launch { container.bookmarks.update(it) } },
            onDelete = {
                scope.launch {
                    container.bookmarks.delete(b.id)
                    if (snackbar.showSnackbar(deletedMsg, undo) == SnackbarResult.ActionPerformed) {
                        container.bookmarks.restore(b)
                    }
                }
            },
            onDismiss = { editing = null },
        )
    }
}
