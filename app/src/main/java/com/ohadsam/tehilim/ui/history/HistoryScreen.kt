package com.ohadsam.tehilim.ui.history

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.ohadsam.tehilim.AppContainer
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.favorites.FavoritesState
import com.ohadsam.tehilim.said.HistoryBuilder
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Last 30 days, newest first, showing for each day what was marked as said; tap a row to correct it. */
@Composable
fun HistoryScreen(container: AppContainer, onBack: () -> Unit) {
    val entries by container.said.entries.collectAsState(initial = emptyList())
    val favState by container.favorites.state.collectAsState(initial = FavoritesState(emptyList(), emptyList()))
    var query by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()
    val today = remember { container.locations.logicalToday() }

    val days = remember(entries, favState, query, status) {
        HistoryBuilder.filter(HistoryBuilder.build(today, entries, favState), query, status)
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) }
            Text(stringResource(R.string.history_title), style = MaterialTheme.typography.titleLarge)
        }
        OutlinedTextField(
            value = query, onValueChange = { query = it }, singleLine = true,
            label = { Text(stringResource(R.string.history_search)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(status == null, { status = null }, { Text(stringResource(R.string.history_all)) })
            FilterChip(status == true, { status = true }, { Text(stringResource(R.string.history_said)) })
            FilterChip(status == false, { status = false }, { Text(stringResource(R.string.history_not_said)) })
        }
        if (days.isEmpty()) {
            Text(stringResource(R.string.history_empty), modifier = Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(days, key = { it.date.toEpochDay() }) { day ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(day.hebrewLabel, style = MaterialTheme.typography.titleMedium)
                        Text(day.date.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        day.rows.forEach { row ->
                            Row(
                                Modifier.fillMaxWidth().clickable { scope.launch { container.said.toggle(day.date, row.context) } }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    if (row.said) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                    contentDescription = stringResource(if (row.said) R.string.history_said else R.string.history_not_said),
                                    tint = if (row.said) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                                )
                                Text(row.label, modifier = Modifier.padding(start = 12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
