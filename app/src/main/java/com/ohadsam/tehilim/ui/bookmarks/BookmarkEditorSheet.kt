package com.ohadsam.tehilim.ui.bookmarks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.bookmarks.Bookmark
import com.ohadsam.tehilim.ui.favorites.ColorDot
import com.ohadsam.tehilim.ui.favorites.FavoriteColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkEditorSheet(bookmark: Bookmark, onSave: (Bookmark) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var label by remember { mutableStateOf(bookmark.label) }
    var note by remember { mutableStateOf(bookmark.note.orEmpty()) }
    var color by remember { mutableStateOf(bookmark.colorIndex) }
    var pinned by remember { mutableStateOf(bookmark.pinned) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.bm_edit), style = MaterialTheme.typography.titleLarge)
            Text(bookmark.snippet, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(label, { label = it }, singleLine = true, label = { Text(stringResource(R.string.bm_label)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(note, { note = it }, label = { Text(stringResource(R.string.bm_note)) }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FavoriteColors.indices.forEach { i ->
                    Row(Modifier.clickable { color = i }.padding(4.dp)) { ColorDot(i, size = if (i == color) 32.dp else 24.dp) }
                }
            }
            FilterChip(selected = pinned, onClick = { pinned = !pinned }, label = { Text(stringResource(R.string.bm_pin)) })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = {
                    onDelete()
                    onDismiss()
                }) { Text(stringResource(R.string.action_delete)) }
                Row {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                    Button(onClick = {
                        onSave(bookmark.copy(label = label.ifBlank { bookmark.label }, note = note.ifBlank { null }, colorIndex = color, pinned = pinned))
                        onDismiss()
                    }) { Text(stringResource(R.string.action_save)) }
                }
            }
        }
    }
}
