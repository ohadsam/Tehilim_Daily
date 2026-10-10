package com.ohadsam.tehilim.ui.location

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ohadsam.tehilim.AppContainer
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.calendar.Holidays
import com.ohadsam.tehilim.calendar.TzeitMethod
import com.ohadsam.tehilim.calendar.ZmanimHolyTimes
import com.ohadsam.tehilim.location.DeviceLocation
import com.ohadsam.tehilim.location.LocationChoice
import com.ohadsam.tehilim.location.LocationSource
import com.ohadsam.tehilim.location.Places
import com.ohadsam.tehilim.ui.common.formatMoment
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Settings → מיקום: automatic (device) or manual (city / coordinates) location used for candle lighting and havdalah. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocationScreen(container: AppContainer, onBack: () -> Unit) {
    val context = LocalContext.current
    val store = container.locations
    val scope = rememberCoroutineScope()
    var version by mutableIntStateOf(0)
    var choice by remember { mutableStateOf(store.choice) }
    var query by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Int?>(null) }
    var lat by remember { mutableStateOf("") }
    var lon by remember { mutableStateOf("") }
    var elev by remember { mutableStateOf("0") }
    var customName by remember { mutableStateOf("") }
    var candle by remember { mutableStateOf(store.candleMinutesOverride) }
    var tzeit by remember { mutableStateOf(store.tzeit) }

    fun refreshFix() {
        scope.launch {
            busy = true
            val fix = DeviceLocation.fix(context)
            if (fix != null) {
                store.lastAutoFix = fix
                message = R.string.loc_updated
            } else {
                message = R.string.loc_no_fix
            }
            version++
            busy = false
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) refreshFix() else message = R.string.loc_permission_denied
    }

    val resolved = remember(choice, version, candle) { store.resolved() }
    LaunchedEffect(choice) { if (choice is LocationChoice.Auto && DeviceLocation.hasPermission(context) && store.lastAutoFix == null) refreshFix() }

    val preview = remember(resolved, tzeit) {
        val holy = ZmanimHolyTimes(resolved.place, tzeit)
        // find the first run start from today
        var start = LocalDate.now()
        var guard = 0
        while (!(Holidays.isHolyDay(start, resolved.place.inIsrael) && !Holidays.isHolyDay(start.minusDays(1), resolved.place.inIsrael)) && guard++ < 400) start = start.plusDays(1)
        var last = start
        while (Holidays.isHolyDay(last.plusDays(1), resolved.place.inIsrael)) last = last.plusDays(1)
        val c = holy.candleLighting(start.minusDays(1))
        val h = holy.havdalah(last)
        if (c != null && h != null) formatMoment(c, resolved.place.zone) to formatMoment(h, resolved.place.zone) else null
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) }
            Text(stringResource(R.string.loc_title), style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.loc_current, resolved.place.nameHe, stringResource(sourceLabel(resolved.source))),
                style = MaterialTheme.typography.titleMedium,
            )
            preview?.let { (c, h) ->
                Text(stringResource(R.string.loc_preview_candle, c), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.loc_preview_havdalah, h), style = MaterialTheme.typography.bodyMedium)
            }
            message?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.secondary) }

            ChoiceRow(choice is LocationChoice.Auto, R.string.loc_auto) {
                choice = LocationChoice.Auto
                store.choice = choice
                version++
            }
            if (choice is LocationChoice.Auto) {
                Button(
                    enabled = !busy,
                    onClick = { if (DeviceLocation.hasPermission(context)) refreshFix() else permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                ) { Text(stringResource(if (busy) R.string.loc_updating else R.string.loc_update_now)) }
            }

            ChoiceRow(choice is LocationChoice.Manual, R.string.loc_manual) {
                if (choice !is LocationChoice.Manual) {
                    choice = LocationChoice.Manual((store.resolved().place.id.takeIf { Places.byId(it) != null }) ?: Places.JERUSALEM.id)
                    store.choice = choice
                    version++
                }
            }
            if (choice is LocationChoice.Manual) {
                OutlinedTextField(query, { query = it }, singleLine = true, label = { Text(stringResource(R.string.loc_search)) }, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Places.search(query).forEach { p ->
                        FilterChip(
                            selected = (choice as? LocationChoice.Manual)?.placeId == p.id,
                            onClick = { choice = LocationChoice.Manual(p.id); store.choice = choice; version++ },
                            label = { Text(p.nameHe) },
                        )
                    }
                }
            }

            ChoiceRow(choice is LocationChoice.Custom, R.string.loc_custom) {
                if (choice !is LocationChoice.Custom) choice = LocationChoice.Custom("", 31.7683, 35.2137, 0.0)
            }
            if (choice is LocationChoice.Custom) {
                OutlinedTextField(customName, { customName = it }, singleLine = true, label = { Text(stringResource(R.string.loc_custom_name)) }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(lat, { lat = it }, singleLine = true, label = { Text(stringResource(R.string.loc_lat)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                    OutlinedTextField(lon, { lon = it }, singleLine = true, label = { Text(stringResource(R.string.loc_lon)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                    OutlinedTextField(elev, { elev = it }, singleLine = true, label = { Text(stringResource(R.string.loc_elev)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                }
                val la = lat.toDoubleOrNull()
                val lo = lon.toDoubleOrNull()
                OutlinedButton(
                    enabled = la != null && lo != null && la in -90.0..90.0 && lo in -180.0..180.0,
                    onClick = {
                        choice = LocationChoice.Custom(customName, la!!, lo!!, elev.toDoubleOrNull() ?: 0.0)
                        store.choice = choice
                        version++
                    },
                ) { Text(stringResource(R.string.action_save)) }
            }

            Text(stringResource(R.string.loc_candle_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(candle == null, { candle = null; store.candleMinutesOverride = null }, { Text(stringResource(R.string.loc_candle_default, resolved.place.candleLightingMinutes)) })
                listOf(18, 20, 22, 30, 40).forEach { m ->
                    FilterChip(candle == m, { candle = m; store.candleMinutesOverride = m }, { Text(stringResource(R.string.rem_minutes, m)) })
                }
            }
            Text(stringResource(R.string.loc_tzeit_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
            TzeitMethod.entries.forEach { m ->
                Row(Modifier.fillMaxWidth().clickable { tzeit = m; store.tzeit = m }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = tzeit == m, onClick = { tzeit = m; store.tzeit = m })
                    Text(stringResource(tzeitLabel(m)))
                }
            }
        }
    }
}

@Composable
private fun ChoiceRow(selected: Boolean, label: Int, onSelect: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onSelect), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(stringResource(label), style = MaterialTheme.typography.titleSmall)
    }
}

private fun sourceLabel(s: LocationSource) = when (s) {
    LocationSource.MANUAL -> R.string.loc_src_manual
    LocationSource.CUSTOM -> R.string.loc_src_custom
    LocationSource.AUTO -> R.string.loc_src_auto
    LocationSource.LAST_AUTO -> R.string.loc_src_last
    LocationSource.DEFAULT -> R.string.loc_src_default
}

private fun tzeitLabel(m: TzeitMethod) = when (m) {
    TzeitMethod.DEGREES_8_5 -> R.string.tzeit_8_5
    TzeitMethod.DEGREES_7_083 -> R.string.tzeit_7_083
    TzeitMethod.MINUTES_42 -> R.string.tzeit_42
    TzeitMethod.MINUTES_72 -> R.string.tzeit_72
}
