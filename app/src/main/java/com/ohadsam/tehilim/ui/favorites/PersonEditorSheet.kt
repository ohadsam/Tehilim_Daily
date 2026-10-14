package com.ohadsam.tehilim.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ohadsam.tehilim.AppContainer
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.favorites.ChapterSetParser
import com.ohadsam.tehilim.favorites.Favorite
import com.ohadsam.tehilim.favorites.FavoritesState
import com.ohadsam.tehilim.favorites.Person
import com.ohadsam.tehilim.favorites.PersonKind
import com.ohadsam.tehilim.favorites.summarizeChapters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonEditorSheet(container: AppContainer, existing: Person?, kind: PersonKind, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var mother by remember { mutableStateOf(existing?.motherName.orEmpty()) }
    var day by remember { mutableStateOf(existing?.yahrzeitDay?.toString().orEmpty()) }
    var month by remember { mutableStateOf(existing?.yahrzeitMonth) }

    // The person's own chapter list lives in a favorite linked only to them, so reading, widgets and reminders keep working unchanged.
    val loaded by androidx.compose.runtime.produceState<FavoritesState?>(null) { value = container.favorites.state.first() }
    val personal = existing?.let { loaded?.personalFavorite(it.id) }
    var chaptersText by remember { mutableStateOf("") }
    androidx.compose.runtime.LaunchedEffect(personal?.id, loaded != null) {
        if (personal != null) chaptersText = personal.chapters.joinToString(", ") { it.compact }
    }
    val parsed = ChapterSetParser.parse(chaptersText)
    val chaptersValid = parsed.invalidTokens.isEmpty()

    val dayNumber = day.toIntOrNull()
    val dayValid = day.isBlank() || (dayNumber != null && dayNumber in 1..30)
    val isIlui = kind == PersonKind.ILUI_NISHMAT

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(if (isIlui) R.string.fav_new_ilui else R.string.fav_new_refuah),
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                value = name, onValueChange = { name = it }, singleLine = true,
                label = { Text(stringResource(R.string.person_name)) }, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = mother, onValueChange = { mother = it }, singleLine = true,
                label = { Text(stringResource(R.string.person_mother)) }, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = chaptersText, onValueChange = { chaptersText = it },
                label = { Text(stringResource(R.string.person_chapters)) },
                supportingText = {
                    Text(
                        when {
                            parsed.invalidTokens.isNotEmpty() -> stringResource(R.string.fav_chapters_invalid, parsed.invalidTokens.joinToString(" "))
                            parsed.refs.isEmpty() -> stringResource(R.string.fav_chapters_hint)
                            else -> summarizeChapters(parsed.refs)
                        },
                    )
                },
                isError = !chaptersValid,
                modifier = Modifier.fillMaxWidth(),
            )
            if (isIlui) {
                Text(stringResource(R.string.person_yahrzeit), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = day, onValueChange = { day = it.take(2) }, singleLine = true, isError = !dayValid,
                        label = { Text(stringResource(R.string.person_day)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    com.ohadsam.tehilim.ui.common.HebrewMonthPicker(month, stringResource(R.string.person_month), { month = it }, Modifier.weight(2f))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                Button(
                    enabled = name.isNotBlank() && dayValid && chaptersValid && loaded != null,
                    onClick = {
                        scope.launch {
                            val hasYahrzeit = isIlui && dayNumber != null && month != null && dayNumber <= com.ohadsam.tehilim.ui.common.maxDayOfHebrewMonth(month!!)
                            val personId = container.favorites.savePerson(
                                (existing ?: Person(name = name.trim(), kind = kind)).copy(
                                    name = name.trim(),
                                    motherName = mother.trim().ifBlank { null },
                                    yahrzeitDay = if (hasYahrzeit) dayNumber else null,
                                    yahrzeitMonth = if (hasYahrzeit) month else null,
                                ),
                            )
                            when {
                                parsed.refs.isNotEmpty() -> container.favorites.saveFavorite(
                                    (personal ?: Favorite(name = name.trim(), chapters = parsed.refs)).copy(chapters = parsed.refs, personIds = listOf(personId)),
                                )
                                personal != null -> container.favorites.deleteFavorite(personal.id) // field cleared = list removed
                            }
                            sheet.hide()
                            onDismiss()
                        }
                    },
                ) { Text(stringResource(R.string.action_save)) }
            }
        }
    }
}

/** Longest possible length of a Hebrew month in any year (Adar I and the variable Cheshvan/Kislev can have 30 days). */
private fun maxDayOf(month: Int): Int = if (month in intArrayOf(2, 4, 6, 10, 13)) 29 else 30
