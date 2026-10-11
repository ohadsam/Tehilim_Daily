package com.ohadsam.tehilim.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ohadsam.tehilim.AppContainer
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.domain.HebrewNumerals
import com.ohadsam.tehilim.favorites.Favorite
import com.ohadsam.tehilim.favorites.FavoritesState
import com.ohadsam.tehilim.favorites.NameLetterPlan
import com.ohadsam.tehilim.favorites.NameSection
import com.ohadsam.tehilim.favorites.NameSpec
import com.ohadsam.tehilim.favorites.Person
import com.ohadsam.tehilim.favorites.PersonKind
import com.ohadsam.tehilim.reminders.HolyDayRule
import com.ohadsam.tehilim.reminders.Reminder
import com.ohadsam.tehilim.reminders.ReminderTarget
import com.ohadsam.tehilim.reminders.Repeat
import com.ohadsam.tehilim.ui.common.HebrewMonthPicker
import com.ohadsam.tehilim.ui.common.maxDayOfHebrewMonth
import com.ohadsam.tehilim.ui.reminders.TimePickerDialog
import kotlinx.coroutines.launch
import java.time.LocalTime

/**
 * Builds a "name letters" reading: type the full name (one or more names), get the matching stanzas of Psalm 119 followed by
 * "נשמה", save it for a person (refuah / ilui nishmat) and, with a yahrzeit date, create a yearly reminder that opens it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NameLettersScreen(container: AppContainer, personId: Long?, onBack: () -> Unit, onSaved: (favoriteId: Long) -> Unit) {
    val state by container.favorites.state.collectAsState(initial = FavoritesState(emptyList(), emptyList()))
    val scope = rememberCoroutineScope()
    val existing = personId?.let { state.person(it) }

    var names by remember(existing?.id) { mutableStateOf(existing?.name?.split(" ")?.filter { it.isNotBlank() }.orEmpty()) }
    var input by remember { mutableStateOf("") }
    var kind by remember(existing?.id) { mutableStateOf(existing?.kind ?: PersonKind.ILUI_NISHMAT) }
    var neshama by remember(existing?.id) { mutableStateOf((existing?.kind ?: PersonKind.ILUI_NISHMAT) == PersonKind.ILUI_NISHMAT) }
    var mother by remember(existing?.id) { mutableStateOf(existing?.motherName.orEmpty()) }
    var day by remember(existing?.id) { mutableStateOf(existing?.yahrzeitDay?.toString().orEmpty()) }
    var month by remember(existing?.id) { mutableStateOf(existing?.yahrzeitMonth) }
    var remind by remember { mutableStateOf(true) }
    var time by remember { mutableStateOf(LocalTime.of(8, 0)) }
    var timeDialog by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val spec = NameSpec(names + listOfNotNull(input.trim().takeIf { it.isNotEmpty() }), neshama)
    val plan = remember(spec) { NameLetterPlan.build(spec) }
    val scheme = MaterialTheme.colorScheme

    val dayNumber = day.toIntOrNull()
    val yahrzeitOk = kind == PersonKind.ILUI_NISHMAT && dayNumber != null && month != null && dayNumber in 1..maxDayOfHebrewMonth(month!!)
    val canSave = plan.isNotEmpty() && !saving

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) }
            Text(stringResource(R.string.nl_title), style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.nl_intro), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)

            Text(stringResource(R.string.nl_names), style = MaterialTheme.typography.titleSmall, color = scheme.secondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                names.forEachIndexed { i, n ->
                    InputChip(selected = false, onClick = { names = names.toMutableList().also { it.removeAt(i) } }, label = { Text("$n  ✕") })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(input, { input = it }, singleLine = true, label = { Text(stringResource(R.string.nl_add_name_hint)) }, modifier = Modifier.weight(1f))
                OutlinedButton(enabled = input.isNotBlank(), onClick = {
                    names = names + input.trim().split(" ").filter { it.isNotBlank() }
                    input = ""
                }) { Text(stringResource(R.string.nl_add_name)) }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.nl_neshama), Modifier.weight(1f))
                Switch(neshama, { neshama = it })
            }

            Text(stringResource(R.string.nl_for), style = MaterialTheme.typography.titleSmall, color = scheme.secondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(kind == PersonKind.ILUI_NISHMAT, { kind = PersonKind.ILUI_NISHMAT; neshama = true }, { Text(stringResource(R.string.fav_tab_ilui)) })
                FilterChip(kind == PersonKind.REFUAH, { kind = PersonKind.REFUAH }, { Text(stringResource(R.string.fav_tab_refuah)) })
            }
            OutlinedTextField(mother, { mother = it }, singleLine = true, label = { Text(stringResource(R.string.person_mother)) }, modifier = Modifier.fillMaxWidth())

            if (kind == PersonKind.ILUI_NISHMAT) {
                Text(stringResource(R.string.person_yahrzeit), style = MaterialTheme.typography.titleSmall, color = scheme.secondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        day, { day = it.take(2) }, singleLine = true, label = { Text(stringResource(R.string.person_day)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
                    )
                    HebrewMonthPicker(month, stringResource(R.string.person_month), { month = it }, Modifier.weight(1.5f))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.nl_remind), Modifier.weight(1f))
                    Switch(remind && yahrzeitOk, { remind = it }, enabled = yahrzeitOk)
                }
                if (remind && yahrzeitOk) {
                    OutlinedButton(onClick = { timeDialog = true }) { Text(stringResource(R.string.nl_remind_time, "%02d:%02d".format(time.hour, time.minute))) }
                }
            }

            Text(stringResource(R.string.nl_preview), style = MaterialTheme.typography.titleSmall, color = scheme.secondary)
            if (plan.isEmpty()) Text(stringResource(R.string.nl_empty), color = scheme.onSurfaceVariant)
            plan.forEach { seg ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(nameHeading(seg, scheme.onSurfaceVariant, scheme.secondary, scheme.secondaryContainer), Modifier.weight(1f))
                        val r = seg.ref
                        Text(
                            stringResource(
                                if (seg.section == NameSection.NAME) R.string.nl_stanza else R.string.nl_stanza_neshama,
                                HebrewNumerals.toHebrew(r.fromVerse!!, false), HebrewNumerals.toHebrew(r.toVerse!!, false),
                            ),
                            style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Button(
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    saving = true
                    scope.launch {
                        val full = spec.fullName
                        val person = (existing ?: Person(name = full, kind = kind)).copy(
                            name = full.ifBlank { existing?.name.orEmpty() }, kind = kind, motherName = mother.trim().ifBlank { null },
                            yahrzeitDay = if (yahrzeitOk) dayNumber else existing?.yahrzeitDay,
                            yahrzeitMonth = if (yahrzeitOk) month else existing?.yahrzeitMonth,
                        )
                        val pid = container.favorites.savePerson(person)
                        val fid = container.favorites.saveFavorite(
                            Favorite(
                                name = container.appString(R.string.nl_favorite_name, full), chapters = NameLetterPlan.refs(spec),
                                colorIndex = 0, personIds = listOf(pid), nameSpec = spec,
                            ),
                        )
                        if (remind && yahrzeitOk) {
                            val rid = container.reminders.save(
                                Reminder(
                                    target = ReminderTarget.Group(fid, pid), times = listOf(time), repeat = Repeat.YearlyHebrew(month!!, dayNumber!!),
                                    holyDayRule = HolyDayRule.BOTH,
                                ),
                            )
                            container.reminders.get(rid)?.let { container.reminderScheduler.schedule(it) }
                        }
                        // Navigation touches lifecycle state, which must happen on the main thread (also under Compose test dispatchers).
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { onSaved(fid) }
                    }
                },
            ) { Text(stringResource(R.string.nl_save)) }
        }
    }
    if (timeDialog) TimePickerDialog(time, onDismiss = { timeDialog = false }, onConfirm = { time = it; timeDialog = false })
}
