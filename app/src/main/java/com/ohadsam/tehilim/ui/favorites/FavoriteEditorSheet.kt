package com.ohadsam.tehilim.ui.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ohadsam.tehilim.AppContainer
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.favorites.ChapterSetParser
import com.ohadsam.tehilim.favorites.Favorite
import com.ohadsam.tehilim.favorites.FavoritesState
import com.ohadsam.tehilim.favorites.NameLetters
import com.ohadsam.tehilim.favorites.PersonKind
import com.ohadsam.tehilim.favorites.summarizeChapters
import kotlinx.coroutines.launch

/** Create / edit a favorite: name, chapters (free text like "20-24, 91"), colour and linked people. */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun FavoriteEditorSheet(container: AppContainer, state: FavoritesState, existing: Favorite?, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var chaptersText by remember { mutableStateOf(existing?.chapters?.joinToString(", ") { it.compact }.orEmpty()) }
    var color by remember { mutableStateOf(existing?.colorIndex ?: 0) }
    var personIds by remember { mutableStateOf(existing?.personIds.orEmpty()) }

    val parsed = ChapterSetParser.parse(chaptersText)
    val canSave = parsed.refs.isNotEmpty() && parsed.invalidTokens.isEmpty()

    fun appendRefs(refs: List<com.ohadsam.tehilim.domain.PsalmRef>) {
        val merged = ChapterSetParser.parse(chaptersText).refs + refs
        chaptersText = merged.distinct().joinToString(", ") { it.compact }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(if (existing == null) R.string.fav_new_favorite else R.string.fav_edit_favorite),
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                value = name, onValueChange = { name = it }, singleLine = true,
                label = { Text(stringResource(R.string.fav_name)) }, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = chaptersText, onValueChange = { chaptersText = it },
                label = { Text(stringResource(R.string.fav_chapters)) },
                supportingText = {
                    Text(
                        when {
                            parsed.invalidTokens.isNotEmpty() -> stringResource(R.string.fav_chapters_invalid, parsed.invalidTokens.joinToString(" "))
                            parsed.refs.isEmpty() -> stringResource(R.string.fav_chapters_hint)
                            else -> summarizeChapters(parsed.refs)
                        },
                    )
                },
                isError = parsed.invalidTokens.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.fav_color), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FavoriteColors.indices.forEach { i ->
                    Row(Modifier.clickable { color = i }.padding(4.dp)) {
                        ColorDot(i, size = if (i == color) 32.dp else 24.dp)
                    }
                }
            }
            if (state.persons.isNotEmpty()) {
                Text(stringResource(R.string.fav_link_people), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.persons.forEach { p ->
                        FilterChip(
                            selected = p.id in personIds,
                            onClick = { personIds = if (p.id in personIds) personIds - p.id else personIds + p.id },
                            label = { Text((if (p.kind == PersonKind.REFUAH) "לרפואת " else "לעילוי נשמת ") + p.name) },
                        )
                    }
                }
                val chosen = state.persons.filter { it.id in personIds }
                if (chosen.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { appendRefs(chosen.flatMap { NameLetters.stanzasFor(it.name) }) }) {
                            Text(stringResource(R.string.fav_add_name_letters))
                        }
                        if (chosen.any { it.kind == PersonKind.ILUI_NISHMAT }) {
                            OutlinedButton(onClick = { appendRefs(NameLetters.stanzasFor(NameLetters.NESHAMA)) }) {
                                Text(stringResource(R.string.fav_add_neshama))
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                Button(
                    enabled = canSave,
                    onClick = {
                        scope.launch {
                            val finalName = name.ifBlank { summarizeChapters(parsed.refs) }
                            container.favorites.saveFavorite(
                                (existing ?: Favorite(name = finalName, chapters = parsed.refs)).copy(
                                    name = finalName, chapters = parsed.refs, colorIndex = color, personIds = personIds,
                                ),
                            )
                            sheet.hide()
                            onDismiss()
                        }
                    },
                ) { Text(stringResource(R.string.action_save)) }
            }
        }
    }
}
