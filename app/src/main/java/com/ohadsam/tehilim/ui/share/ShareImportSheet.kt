package com.ohadsam.tehilim.ui.share

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ohadsam.tehilim.AppContainer
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.favorites.summarizeChapters
import com.ohadsam.tehilim.share.DivisionPlanner
import com.ohadsam.tehilim.share.ImportResult
import com.ohadsam.tehilim.share.IncomingShare
import com.ohadsam.tehilim.share.ShareCodec
import com.ohadsam.tehilim.share.ShareKind
import com.ohadsam.tehilim.share.SharePackage
import com.ohadsam.tehilim.share.ShareParse
import com.ohadsam.tehilim.share.refs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Receives a share: [initialText] comes from an opened file / forwarded message; otherwise the user pastes the code or opens a
 * file. Shows what will be loaded (and, for a division, lets the person pick their part) before anything is saved.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ShareImportSheet(container: AppContainer, initialText: String, onDismiss: () -> Unit, onImported: (ImportResult) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember { mutableStateOf(initialText) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var failed by remember { mutableStateOf(false) }
    val parse = remember(text) { if (text.isBlank()) null else ShareCodec.parse(text) }
    val pkg = (parse as? ShareParse.Ok)?.pkg

    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val read = withContext(Dispatchers.IO) { IncomingShare.readUri(context, uri) }
                if (read != null) text = read
            }
        }
    }

    fun load(indexes: List<Int>) {
        val p = pkg ?: return
        scope.launch {
            runCatching { container.shareImporter.import(p, indexes) }
                .onSuccess { sheet.hide(); onImported(it) }
                .onFailure { failed = true }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.sh_import_title), style = MaterialTheme.typography.titleLarge)
            if (pkg == null) {
                OutlinedTextField(
                    value = text, onValueChange = { text = it }, minLines = 3, maxLines = 6,
                    label = { Text(stringResource(R.string.sh_import_paste_field)) },
                    isError = parse != null,
                    supportingText = {
                        when (parse) {
                            is ShareParse.NotShare -> Text(stringResource(R.string.sh_import_none))
                            is ShareParse.Invalid -> Text(stringResource(R.string.sh_import_bad))
                            else -> Unit
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { clipboardText(context)?.let { text = it } }) { Text(stringResource(R.string.sh_import_paste)) }
                    OutlinedButton(onClick = { openFile.launch(arrayOf("*/*")) }) { Text(stringResource(R.string.sh_import_file)) }
                }
            } else {
                SharePreview(pkg, selected) { selected = it }
                if (failed) Text(stringResource(R.string.sh_import_failed), color = MaterialTheme.colorScheme.error)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                    if (pkg.isDivision) {
                        OutlinedButton(onClick = { load(pkg.parts.indices.toList()) }) { Text(stringResource(R.string.sh_import_load_all)) }
                        Button(
                            enabled = selected != null,
                            onClick = { selected?.let { load(listOf(it)) } },
                            modifier = Modifier.padding(start = 8.dp),
                        ) { Text(stringResource(R.string.sh_import_load_mine)) }
                    } else {
                        Button(onClick = { load(listOf(0)) }) { Text(stringResource(R.string.sh_import_load)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SharePreview(pkg: SharePackage, selected: Int?, onSelect: (Int) -> Unit) {
    Text(pkg.title, style = MaterialTheme.typography.titleMedium)
    val who = pkg.personName.orEmpty()
    when (pkg.kind) {
        ShareKind.REFUAH -> Text(stringResource(R.string.sh_import_for_refuah, who), color = MaterialTheme.colorScheme.secondary)
        ShareKind.ILUI_NISHMAT -> Text(stringResource(R.string.sh_import_for_ilui, who), color = MaterialTheme.colorScheme.secondary)
        ShareKind.FAVORITE -> Unit
    }
    if (!pkg.isDivision) {
        Text(partSummary(pkg.parts[0].refs()), style = MaterialTheme.typography.bodyLarge)
        return
    }
    Text(stringResource(R.string.sh_import_pick), style = MaterialTheme.typography.labelLarge)
    pkg.parts.forEachIndexed { i, part ->
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.RadioButton) { onSelect(i) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected == i, onClick = null)
            Column(Modifier.padding(start = 8.dp, top = 6.dp, bottom = 6.dp)) {
                Text(part.label, style = MaterialTheme.typography.titleSmall)
                Text(partSummary(part.refs()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun partSummary(refs: List<com.ohadsam.tehilim.domain.PsalmRef>): String =
    stringResource(R.string.sh_part_summary, summarizeChapters(refs), DivisionPlanner.versesOf(refs))

private fun clipboardText(context: Context): String? {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    return cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
}
