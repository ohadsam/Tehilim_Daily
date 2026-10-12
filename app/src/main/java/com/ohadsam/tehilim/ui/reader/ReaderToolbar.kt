package com.ohadsam.tehilim.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.bookmarks.Bookmark
import com.ohadsam.tehilim.ui.bookmarks.BookmarkHooks

/** One entry of the toolbar's overflow menu. */
class ToolbarAction(val label: String, val onClick: () -> Unit)

/**
 * The reader's single slim top bar (replaces the old stack of headers): title block that expands/collapses the
 * navigation panel, a bookmarks drop-down (add here / jump to a saved one / all), and an overflow menu of quick jumps.
 */
@Composable
fun ReaderToolbar(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    expanded: Boolean? = null,
    onToggleExpanded: (() -> Unit)? = null,
    bookmarks: BookmarkHooks? = null,
    onOpenBookmark: (Bookmark) -> Unit = {},
    onOpenAllBookmarks: () -> Unit = {},
    actions: List<ToolbarAction> = emptyList(),
    extra: @Composable () -> Unit = {},
) {
    Row(modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        val titleModifier = Modifier.weight(1f).let { if (onToggleExpanded != null) it.clickable(onClick = onToggleExpanded) else it }
        Row(titleModifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f, fill = false)) {
                Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (expanded != null) {
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = stringResource(if (expanded) R.string.action_collapse else R.string.action_expand),
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
        extra()
        if (bookmarks != null) BookmarksMenu(bookmarks, onOpenBookmark, onOpenAllBookmarks)
        if (actions.isNotEmpty()) OverflowMenu(actions)
    }
}

@Composable
private fun BookmarksMenu(hooks: BookmarkHooks, onOpen: (Bookmark) -> Unit, onOpenAll: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) { Icon(Icons.Filled.Bookmark, stringResource(R.string.bm_menu)) }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.bm_add_here)) },
            leadingIcon = { Icon(Icons.Filled.BookmarkAdd, null) },
            onClick = { open = false; hooks.onAddHere() },
        )
        val recent = remember(hooks.all) { com.ohadsam.tehilim.bookmarks.BookmarkText.sorted(hooks.all).take(MENU_BOOKMARKS) }
        if (recent.isNotEmpty()) {
            HorizontalDivider()
            recent.forEach { b ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(b.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(b.snippet, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    },
                    onClick = { open = false; onOpen(b) },
                )
            }
        }
        HorizontalDivider()
        DropdownMenuItem(text = { Text(stringResource(R.string.bm_all)) }, onClick = { open = false; onOpenAll() })
    }
}

@Composable
private fun OverflowMenu(actions: List<ToolbarAction>) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.action_more)) }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        actions.forEach { a -> DropdownMenuItem(text = { Text(a.label) }, onClick = { open = false; a.onClick() }) }
    }
}

private const val MENU_BOOKMARKS = 5
