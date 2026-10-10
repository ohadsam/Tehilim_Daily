package com.ohadsam.tehilim.ui.bookmarks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.ohadsam.tehilim.AppContainer
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.bookmarks.Bookmark
import com.ohadsam.tehilim.bookmarks.BookmarkText
import kotlinx.coroutines.launch

/** All bookmarks: pinned first, searchable; tap to jump to the verse, long-press to edit, "clear all" with Undo. */
@Composable
fun BookmarksScreen(container: AppContainer, onBack: () -> Unit, onOpen: (Bookmark) -> Unit) {
    val all by container.bookmarks.bookmarks.collectAsState(initial = emptyList())
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Bookmark?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val shown = BookmarkText.filter(all, query)
    val clearedMsg = stringResource(R.string.bm_cleared)
    val undo = stringResource(R.string.action_undo)

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) }
            Text(stringResource(R.string.bm_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            if (all.isNotEmpty()) {
                IconButton(onClick = { confirmClear = true }) { Icon(Icons.Filled.DeleteSweep, stringResource(R.string.bm_clear_all)) }
            }
        }
        if (all.isEmpty()) {
            Text(stringResource(R.string.bm_empty), modifier = Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            OutlinedTextField(
                query, { query = it }, singleLine = true, label = { Text(stringResource(R.string.bm_search)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
        }
        LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(shown, key = { it.id }) { b ->
                Card(Modifier.fillMaxWidth().clickable { onOpen(b) }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (b.pinned) Icons.Filled.PushPin else Icons.Filled.Bookmark, contentDescription = null,
                            tint = com.ohadsam.tehilim.ui.favorites.FavoriteColors[b.colorIndex.mod(com.ohadsam.tehilim.ui.favorites.FavoriteColors.size)],
                        )
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(b.label, style = MaterialTheme.typography.titleMedium)
                            Text(b.snippet, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            b.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) }
                        }
                        TextButton(onClick = { editing = b }) { Text(stringResource(R.string.action_edit)) }
                    }
                }
            }
        }
    }
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) { SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter)) }

    editing?.let { b ->
        BookmarkEditorSheet(
            b,
            onSave = { scope.launch { container.bookmarks.update(it) } },
            onDelete = {
                scope.launch {
                    container.bookmarks.delete(b.id)
                    if (snackbar.showSnackbar(clearedMsg, undo, duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed) {
                        container.bookmarks.restore(b)
                    }
                }
            },
            onDismiss = { editing = null },
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.bm_clear_all)) },
            text = { Text(stringResource(R.string.bm_clear_confirm, all.size)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    val backup = all
                    scope.launch {
                        container.bookmarks.clear()
                        if (snackbar.showSnackbar(clearedMsg, undo, duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed) {
                            backup.forEach { container.bookmarks.restore(it) }
                        }
                    }
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
