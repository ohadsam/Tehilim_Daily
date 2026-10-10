package com.ohadsam.tehilim.ui.logs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ohadsam.tehilim.AppContainer
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.data.prefs.LogPrefs
import com.ohadsam.tehilim.log.LogLevel
import com.ohadsam.tehilim.log.filtered
import com.ohadsam.tehilim.log.format

private const val SHARE_MAX_CHARS = 150_000

/** View, filter, copy, download and clear the app log; choose how long logs are kept. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogsScreen(container: AppContainer, onBack: () -> Unit) {
    val context = LocalContext.current
    val logger = container.logger
    var version by remember { mutableIntStateOf(0) }
    var level by remember { mutableStateOf(LogLevel.DEBUG) }
    var query by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }
    var retention by remember { mutableIntStateOf(container.logPrefs.retentionDays) }
    var toast by remember { mutableStateOf<Int?>(null) }

    val entries = remember(version, level, query) { logger.snapshot().filtered(level, null, query).take(500) }

    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            logger.flush()
            val ok = runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(logger.exportText().toByteArray()) } }.isSuccess
            toast = if (ok) R.string.logs_saved else R.string.logs_save_failed
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) }
            Text(stringResource(R.string.more_logs), style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(query, { query = it }, singleLine = true, label = { Text(stringResource(R.string.logs_search)) }, modifier = Modifier.fillMaxWidth())
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LogLevel.entries.forEach { l -> FilterChip(level == l, { level = l }, { Text("${l.symbol} ${stringResource(levelName(l))}") }) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    copyToClipboard(context, logger.exportText())
                    toast = R.string.logs_copied
                }) { Text(stringResource(R.string.logs_copy)) }
                OutlinedButton(onClick = { save.launch("tehilim-daily-log.txt") }) { Text(stringResource(R.string.logs_download)) }
                OutlinedButton(onClick = { shareText(context, logger.exportText()) }) { Text(stringResource(R.string.logs_share)) }
                OutlinedButton(onClick = { confirmClear = true }) { Text(stringResource(R.string.logs_clear)) }
                OutlinedButton(onClick = { version++ }) { Text(stringResource(R.string.logs_refresh)) }
            }
            Text(stringResource(R.string.logs_retention), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LogPrefs.OPTIONS.forEach { d ->
                    FilterChip(retention == d, {
                        retention = d
                        container.logPrefs.retentionDays = d
                        if (d > 0) logger.purgeOlderThanDays(d)
                        version++
                    }, { Text(if (d == 0) stringResource(R.string.logs_keep_forever) else stringResource(R.string.logs_days, d)) })
                }
            }
            toast?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.secondary) }
            Text(stringResource(R.string.logs_count, entries.size, logger.snapshot().size), style = MaterialTheme.typography.labelSmall)
        }
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(entries) { e ->
                Text(
                    e.format(), fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                    color = when (e.level) {
                        LogLevel.ERROR -> MaterialTheme.colorScheme.error
                        LogLevel.WARN -> Color(0xFFB8860B)
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.logs_clear)) },
            text = { Text(stringResource(R.string.logs_clear_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    logger.clear()
                    confirmClear = false
                    version++
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

private fun levelName(l: LogLevel) = when (l) {
    LogLevel.DEBUG -> R.string.logs_level_debug
    LogLevel.INFO -> R.string.logs_level_info
    LogLevel.WARN -> R.string.logs_level_warn
    LogLevel.ERROR -> R.string.logs_level_error
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Tehilim Daily log", text))
}

/** Sharing goes through an Intent (size-limited), so only the most recent part of a very long log is sent. */
private fun shareText(context: Context, text: String) {
    val body = if (text.length > SHARE_MAX_CHARS) text.takeLast(SHARE_MAX_CHARS) else text
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, body)
    context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
