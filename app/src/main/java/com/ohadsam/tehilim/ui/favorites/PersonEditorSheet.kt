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
import com.ohadsam.tehilim.favorites.Person
import com.ohadsam.tehilim.favorites.PersonKind
import kotlinx.coroutines.launch

/** Month choices for a yahrzeit; "אדר" (12) also covers Adar I in leap years, 13 is Adar II. */
private val yahrzeitMonths: List<Pair<Int, String>> = listOf(
    7 to "תשרי", 8 to "חשוון", 9 to "כסלו", 10 to "טבת", 11 to "שבט", 12 to "אדר", 13 to "אדר ב׳",
    1 to "ניסן", 2 to "אייר", 3 to "סיוון", 4 to "תמוז", 5 to "אב", 6 to "אלול",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonEditorSheet(container: AppContainer, existing: Person?, kind: PersonKind, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var mother by remember { mutableStateOf(existing?.motherName.orEmpty()) }
    var day by remember { mutableStateOf(existing?.yahrzeitDay?.toString().orEmpty()) }
    var month by remember { mutableStateOf(existing?.yahrzeitMonth) }
    var monthMenu by remember { mutableStateOf(false) }

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
            if (isIlui) {
                Text(stringResource(R.string.person_yahrzeit), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = day, onValueChange = { day = it.take(2) }, singleLine = true, isError = !dayValid,
                        label = { Text(stringResource(R.string.person_day)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    androidx.compose.foundation.layout.Box(Modifier.weight(2f)) {
                        OutlinedButton(onClick = { monthMenu = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(yahrzeitMonths.firstOrNull { it.first == month }?.second ?: stringResource(R.string.person_month))
                        }
                        DropdownMenu(expanded = monthMenu, onDismissRequest = { monthMenu = false }) {
                            yahrzeitMonths.forEach { (m, label) ->
                                DropdownMenuItem(text = { Text(label) }, onClick = {
                                    month = m
                                    monthMenu = false
                                })
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                Button(
                    enabled = name.isNotBlank() && dayValid,
                    onClick = {
                        scope.launch {
                            val hasYahrzeit = isIlui && dayNumber != null && month != null && dayNumber <= com.ohadsam.tehilim.ui.common.maxDayOfHebrewMonth(month!!)
                            container.favorites.savePerson(
                                (existing ?: Person(name = name.trim(), kind = kind)).copy(
                                    name = name.trim(),
                                    motherName = mother.trim().ifBlank { null },
                                    yahrzeitDay = if (hasYahrzeit) dayNumber else null,
                                    yahrzeitMonth = if (hasYahrzeit) month else null,
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

/** Longest possible length of a Hebrew month in any year (Adar I and the variable Cheshvan/Kislev can have 30 days). */
private fun maxDayOf(month: Int): Int = if (month in intArrayOf(2, 4, 6, 10, 13)) 29 else 30
