package com.ohadsam.tehilim.ui.settings

import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ohadsam.tehilim.AppContainer
import com.ohadsam.tehilim.BuildConfig
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.data.prefs.ReaderSettings
import com.ohadsam.tehilim.data.prefs.ThemeMode
import com.ohadsam.tehilim.domain.PSALMS_COUNT
import com.ohadsam.tehilim.domain.TextDisplay
import com.ohadsam.tehilim.prayers.PrayerCatalog
import com.ohadsam.tehilim.ui.theme.Palettes
import com.ohadsam.tehilim.ui.theme.ReaderFont
import com.ohadsam.tehilim.ui.theme.ReaderTextStyle
import com.ohadsam.tehilim.widget.WidgetUpdater
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.sp

private const val PREVIEW_VERSE = "אַשְׁרֵי הָאִישׁ אֲשֶׁר לֹא הָלַךְ בַּעֲצַת רְשָׁעִים" // Psalms 1:1 (first half), for font previews

/** The single place for every preference: appearance, reading, location, prayers, data (cache/backup), logs, about. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onOpenLocation: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenHistory: () -> Unit,
    onShowWhatsNew: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val theme by container.themePrefs.settings.collectAsState()
    val reader by container.readerPrefs.settings.collectAsState()
    val prayer by container.prayerPrefs.settings.collectAsState()
    var cached by remember { mutableIntStateOf(0) }
    var progress by remember { mutableStateOf<Int?>(null) }
    var confirmClearCache by remember { mutableStateOf(false) }
    var confirmImport by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<Int?>(null) }
    var showLicenses by remember { mutableStateOf(false) }

    LaunchedEffect(progress) { cached = container.psalms.cachedChapters().size }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val ok = runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(container.backup.export().toByteArray()) } }.isSuccess
            message = if (ok) R.string.set_backup_saved else R.string.set_backup_failed
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            confirmImport = runCatching { context.contentResolver.openInputStream(uri)?.use { String(it.readBytes()) } }.getOrNull()
            if (confirmImport == null) message = R.string.set_import_failed
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) }
            Text(stringResource(R.string.set_title), style = MaterialTheme.typography.titleLarge)
        }

        Section(R.string.set_appearance) {
            Label(R.string.set_theme_mode)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { m ->
                    FilterChip(theme.mode == m, { container.themePrefs.update { it.copy(mode = m) } }, { Text(stringResource(modeLabel(m))) })
                }
            }
            Label(R.string.set_palette)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Palettes.ALL.forEach { p ->
                    FilterChip(
                        theme.paletteKey == p.key, { container.themePrefs.update { it.copy(paletteKey = p.key) } }, { Text(p.nameHe) },
                        leadingIcon = { Box(Modifier.size(14.dp).clip(CircleShape).background(p.swatch)) },
                    )
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SwitchRow(R.string.set_dynamic, theme.dynamicColor) { v -> container.themePrefs.update { it.copy(dynamicColor = v) } }
            }
            Label(R.string.set_bg_light)
            Swatches(Palettes.LIGHT_BACKGROUNDS, theme.lightBackground) { c -> container.themePrefs.update { it.copy(lightBackground = c) } }
            Label(R.string.set_bg_dark)
            Swatches(Palettes.DARK_BACKGROUNDS, theme.darkBackground) { c -> container.themePrefs.update { it.copy(darkBackground = c) } }
        }

        Section(R.string.set_reading) {
            Label(R.string.set_font)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReaderFont.entries.forEach { f ->
                    FilterChip(reader.fontKey == f.key, { container.readerPrefs.update { it.copy(fontKey = f.key) } }, { Text(f.nameHe) })
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Text(
                    TextDisplay.apply(PREVIEW_VERSE, reader.showNikud, reader.showTaamim),
                    style = ReaderTextStyle.copy(
                        fontFamily = ReaderFont.fromKey(reader.fontKey).family, fontSize = reader.fontSizeSp.sp, lineHeight = (reader.fontSizeSp * reader.lineHeight).sp,
                    ),
                    modifier = Modifier.padding(16.dp),
                )
            }
            Label(R.string.set_font_size)
            Slider(reader.fontSizeSp, { v -> container.readerPrefs.update { it.copy(fontSizeSp = v) } }, valueRange = ReaderSettings.MIN_SIZE..ReaderSettings.MAX_SIZE)
            Label(R.string.set_line_height)
            Slider(reader.lineHeight, { v -> container.readerPrefs.update { it.copy(lineHeight = v) } }, valueRange = 1.2f..2.4f)
            SwitchRow(R.string.set_nikud, reader.showNikud) { v -> container.readerPrefs.update { it.copy(showNikud = v) } }
            SwitchRow(R.string.set_taamim, reader.showTaamim) { v -> container.readerPrefs.update { it.copy(showTaamim = v) } }
            SwitchRow(R.string.set_verse_numbers, reader.showVerseNumbers) { v -> container.readerPrefs.update { it.copy(showVerseNumbers = v) } }
            SwitchRow(R.string.set_keep_screen, reader.keepScreenOn) { v -> container.readerPrefs.update { it.copy(keepScreenOn = v) } }
        }

        Section(R.string.set_prayers) {
            val nusachim = PrayerCatalog.nusachim(container.prayers)
            SwitchRow(R.string.set_prayer_before, prayer.showBefore) { v -> container.prayerPrefs.update { it.copy(showBefore = v) } }
            SwitchRow(R.string.set_prayer_after, prayer.showAfter) { v -> container.prayerPrefs.update { it.copy(showAfter = v) } }
            if (nusachim.isEmpty()) {
                Text(stringResource(R.string.set_prayer_none), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Label(R.string.set_nusach)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    nusachim.forEach { n -> FilterChip(prayer.nusach == n, { container.prayerPrefs.update { it.copy(nusach = n) } }, { Text(nusachName(n)) }) }
                }
            }
        }

        Section(R.string.set_places) {
            NavRow(R.string.set_location, onOpenLocation)
            NavRow(R.string.tab_reminders, onOpenReminders)
            NavRow(R.string.bm_title, onOpenBookmarks)
            NavRow(R.string.more_history, onOpenHistory)
        }

        Section(R.string.set_data) {
            Text(stringResource(R.string.set_cache_status, cached, PSALMS_COUNT))
            progress?.let { LinearProgressIndicator(progress = { it / PSALMS_COUNT.toFloat() }, modifier = Modifier.fillMaxWidth()) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = progress == null, onClick = {
                    progress = cached
                    scope.launch {
                        container.psalms.prefetchAll { progress = it.done }
                        progress = null
                    }
                }) { Text(stringResource(R.string.search_download_all)) }
                OutlinedButton(onClick = { confirmClearCache = true }) { Text(stringResource(R.string.set_clear_cache)) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { exportLauncher.launch("tehilim-daily-backup.json") }) { Text(stringResource(R.string.set_backup)) }
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }) { Text(stringResource(R.string.set_restore)) }
            }
            message?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.secondary) }
        }

        Section(R.string.set_support) {
            NavRow(R.string.more_logs, onOpenLogs)
        }

        Section(R.string.more_about) {
            Text(stringResource(R.string.more_version, BuildConfig.VERSION_NAME))
            NavRow(R.string.more_whats_new, onShowWhatsNew)
            NavRow(R.string.set_licenses) { showLicenses = true }
            Text(stringResource(R.string.set_sefaria), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (confirmClearCache) {
        AlertDialog(
            onDismissRequest = { confirmClearCache = false },
            title = { Text(stringResource(R.string.set_clear_cache)) },
            text = { Text(stringResource(R.string.set_clear_cache_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClearCache = false
                    scope.launch {
                        container.psalms.clearCache()
                        cached = 0
                    }
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmClearCache = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    confirmImport?.let { text ->
        AlertDialog(
            onDismissRequest = { confirmImport = null },
            title = { Text(stringResource(R.string.set_restore)) },
            text = { Text(stringResource(R.string.set_restore_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmImport = null
                    scope.launch {
                        val ok = runCatching { container.backup.import(text) }.isSuccess
                        message = if (ok) R.string.set_import_done else R.string.set_import_failed
                        if (ok) {
                            runCatching { container.reminderScheduler.rescheduleAll() }
                            runCatching { WidgetUpdater.updateAll(context) }
                        }
                    }
                }) { Text(stringResource(R.string.set_restore)) }
            },
            dismissButton = { TextButton(onClick = { confirmImport = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    if (showLicenses) LicensesDialog(context) { showLicenses = false }
}

@Composable
private fun Section(title: Int, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
            content()
        }
    }
}

@Composable
private fun Label(res: Int) = Text(stringResource(res), style = MaterialTheme.typography.labelLarge)

@Composable
private fun SwitchRow(label: Int, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(label), Modifier.weight(1f))
        Switch(value, onChange)
    }
}

@Composable
private fun NavRow(label: Int, onClick: () -> Unit) {
    ListItem(headlineContent = { Text(stringResource(label)) }, modifier = Modifier.clickable(onClick = onClick))
    HorizontalDivider()
}

@Composable
private fun Swatches(colors: List<Color>, selected: Int?, onPick: (Int?) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = { onPick(null) }) { Text(stringResource(R.string.set_bg_default)) }
        colors.forEach { c ->
            val argb = c.toArgb()
            val isSel = selected == argb
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(c)
                    .border(if (isSel) 3.dp else 1.dp, if (isSel) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { onPick(argb) },
            )
        }
    }
}

@Composable
private fun LicensesDialog(context: Context, onDismiss: () -> Unit) {
    val text = remember {
        runCatching {
            (context.assets.list("licenses") ?: emptyArray()).sorted().joinToString("\n\n————————\n\n") { name ->
                "$name\n" + context.assets.open("licenses/$name").bufferedReader().use { it.readText() }.take(1200) + "…"
            }
        }.getOrDefault("")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_licenses)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text(stringResource(R.string.set_licenses_intro)); Text(text, style = MaterialTheme.typography.bodySmall) } },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) } },
    )
}

private fun modeLabel(m: ThemeMode) = when (m) {
    ThemeMode.SYSTEM -> R.string.set_mode_system
    ThemeMode.LIGHT -> R.string.set_mode_light
    ThemeMode.DARK -> R.string.set_mode_dark
}

private fun nusachName(key: String) = when (key) {
    "ashkenaz" -> "אשכנז"
    "sfard" -> "ספרד"
    "edot" -> "עדות המזרח"
    "chabad" -> "חב״ד"
    else -> key
}
