package com.ohadsam.tehilim.ui.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.favorites.ChapterSetParser
import com.ohadsam.tehilim.favorites.summarizeChapters
import com.ohadsam.tehilim.share.DivisionMode
import com.ohadsam.tehilim.share.DivisionPlanner
import com.ohadsam.tehilim.share.ShareKind
import com.ohadsam.tehilim.share.SharePackage
import com.ohadsam.tehilim.share.SharePart
import com.ohadsam.tehilim.share.csvOf
import com.ohadsam.tehilim.share.defaultShareTitle
import com.ohadsam.tehilim.share.partLabel
import com.ohadsam.tehilim.share.refs

/** Create / edit a share: a plain chapter list ([division] = false) or a division of a chapter list between several people. */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ShareEditorSheet(existing: SharePackage?, division: Boolean, onDismiss: () -> Unit, onSave: (SharePackage) -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var kind by remember { mutableStateOf(existing?.kind ?: ShareKind.FAVORITE) }
    var person by remember { mutableStateOf(existing?.personName.orEmpty()) }
    var mother by remember { mutableStateOf(existing?.motherName.orEmpty()) }
    var chaptersText by remember {
        mutableStateOf(
            when {
                existing != null -> summarizeCsv(existing)
                division -> "1-150"
                else -> ""
            },
        )
    }
    var peopleText by remember { mutableStateOf((existing?.parts?.size ?: DEFAULT_PEOPLE).toString()) }
    var mode by remember { mutableStateOf(DivisionMode.BY_LENGTH) }

    val parsed = ChapterSetParser.parse(chaptersText)
    val chaptersValid = parsed.refs.isNotEmpty() && parsed.invalidTokens.isEmpty()
    val maxPeople = if (chaptersValid) DivisionPlanner.maxPeople(parsed.refs, mode) else 1
    val people = peopleText.toIntOrNull()
    val peopleValid = !division || (people != null && people in 1..maxPeople)
    val needsPerson = kind != ShareKind.FAVORITE
    val personValid = !needsPerson || person.isNotBlank()
    val plan = if (division && chaptersValid && peopleValid) DivisionPlanner.divide(parsed.refs, people!!, mode) else null

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(
                    when {
                        existing != null && division -> R.string.sh_edit_division
                        existing != null -> R.string.sh_edit_list
                        division -> R.string.sh_new_division_title
                        else -> R.string.sh_new_list_title
                    },
                ),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(stringResource(R.string.sh_for_whom), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    ShareKind.FAVORITE to R.string.sh_kind_favorite,
                    ShareKind.REFUAH to R.string.sh_kind_refuah,
                    ShareKind.ILUI_NISHMAT to R.string.sh_kind_ilui,
                ).forEach { (k, label) ->
                    FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(stringResource(label)) })
                }
            }
            if (needsPerson) {
                OutlinedTextField(
                    value = person, onValueChange = { person = it }, singleLine = true,
                    label = { Text(stringResource(R.string.person_name)) }, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = mother, onValueChange = { mother = it }, singleLine = true,
                    label = { Text(stringResource(R.string.person_mother)) }, modifier = Modifier.fillMaxWidth(),
                )
            }
            OutlinedTextField(
                value = title, onValueChange = { title = it }, singleLine = true,
                label = { Text(stringResource(R.string.sh_field_title)) },
                placeholder = { Text(defaultShareTitle(kind, person, division)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = chaptersText, onValueChange = { chaptersText = it },
                label = { Text(stringResource(if (division) R.string.sh_chapters_field else R.string.sh_chapters_field_list)) },
                supportingText = {
                    Text(
                        when {
                            parsed.invalidTokens.isNotEmpty() -> stringResource(R.string.fav_chapters_invalid, parsed.invalidTokens.joinToString(" "))
                            parsed.refs.isEmpty() -> stringResource(R.string.sh_chapters_all_hint)
                            else -> summarizeChapters(parsed.refs)
                        },
                    )
                },
                isError = parsed.invalidTokens.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            )
            if (division) {
                OutlinedTextField(
                    value = peopleText, onValueChange = { peopleText = it.filter(Char::isDigit).take(3) }, singleLine = true,
                    label = { Text(stringResource(R.string.sh_people)) },
                    supportingText = { Text(stringResource(R.string.sh_people_max, maxPeople)) },
                    isError = !peopleValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(mode == DivisionMode.BY_LENGTH, { mode = DivisionMode.BY_LENGTH }, { Text(stringResource(R.string.sh_mode_length)) })
                    FilterChip(mode == DivisionMode.BY_COUNT, { mode = DivisionMode.BY_COUNT }, { Text(stringResource(R.string.sh_mode_count)) })
                }
                if (plan != null) {
                    Text(stringResource(R.string.sh_preview), style = MaterialTheme.typography.labelLarge)
                    plan.forEachIndexed { i, refs ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.sh_part_label, i + 1, plan.size), style = MaterialTheme.typography.bodyMedium)
                            Text(partSummary(refs), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                Button(
                    enabled = chaptersValid && peopleValid && personValid,
                    onClick = {
                        val parts = plan?.mapIndexed { i, refs ->
                            SharePart(label = partLabel(i + 1, plan.size), chapters = csvOf(refs))
                        } ?: listOf(SharePart(chapters = csvOf(parsed.refs)))
                        onSave(
                            (existing ?: SharePackage(title = "", parts = parts)).copy(
                                title = title.trim().ifBlank { defaultShareTitle(kind, person, division) },
                                kind = kind,
                                personName = if (needsPerson) person.trim() else null,
                                motherName = if (needsPerson) mother.trim().ifBlank { null } else null,
                                parts = parts,
                            ),
                        )
                    },
                ) { Text(stringResource(R.string.action_save)) }
            }
        }
    }
}

private const val DEFAULT_PEOPLE = 10

/** All chapters of an existing share, in order, as the editor's text field. */
private fun summarizeCsv(pkg: SharePackage): String = pkg.parts.flatMap { it.refs() }.joinToString(", ") { it.compact }
