package com.ohadsam.tehilim.ui.share

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ohadsam.tehilim.AppContainer
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.favorites.summarizeChapters
import com.ohadsam.tehilim.share.ImportResult
import com.ohadsam.tehilim.share.ShareCodec
import com.ohadsam.tehilim.share.SharePackage
import com.ohadsam.tehilim.share.refs

private sealed interface HubSheet {
    data class Edit(val pkg: SharePackage?, val division: Boolean) : HubSheet

    data class Details(val pkg: SharePackage) : HubSheet

    data class Send(val pkg: SharePackage) : HubSheet

    data class Distribute(val pkg: SharePackage) : HubSheet

    data class Receive(val text: String) : HubSheet
}

/** Home of sharing: create a division or a chapter list, send it, or receive what someone sent (also opened by a file / shared message). */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ShareHubScreen(container: AppContainer, onBack: () -> Unit, onOpenFavorite: (Long) -> Unit, onOpenFavorites: () -> Unit) {
    val items by container.sharedLists.items.collectAsState()
    val inbox by container.shareInbox.collectAsState()
    var sheet by remember { mutableStateOf<HubSheet?>(null) }
    var deleting by remember { mutableStateOf<SharePackage?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(inbox) {
        inbox?.let {
            sheet = HubSheet.Receive(it)
            container.shareInbox.value = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) }
                Text(stringResource(R.string.sh_title), style = MaterialTheme.typography.titleLarge)
            }
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(stringResource(R.string.sh_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { sheet = HubSheet.Edit(null, division = true) }) { Text(stringResource(R.string.sh_new_division)) }
                        Button(onClick = { sheet = HubSheet.Edit(null, division = false) }) { Text(stringResource(R.string.sh_new_list)) }
                        OutlinedButton(onClick = { sheet = HubSheet.Receive("") }) { Text(stringResource(R.string.sh_receive)) }
                    }
                }
                item { Text(stringResource(R.string.sh_saved), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
                if (items.isEmpty()) {
                    item { Text(stringResource(R.string.sh_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(items, key = { it.id }) { pkg ->
                    SharedCard(
                        pkg,
                        onOpen = { sheet = HubSheet.Details(pkg) },
                        onShare = { sheet = HubSheet.Send(pkg) },
                        onEdit = { sheet = HubSheet.Edit(pkg, pkg.isDivision) },
                        onDelete = { deleting = pkg },
                    )
                }
            }
        }
    }

    when (val s = sheet) {
        null -> Unit
        is HubSheet.Edit -> ShareEditorSheet(s.pkg, s.division, onDismiss = { sheet = null }, onSave = {
            val stored = container.sharedLists.save(it)
            if (s.pkg != null && s.pkg.parts != stored.parts) container.sharedLists.resetSent(stored.id) // parts changed: old marks are meaningless
            sheet = HubSheet.Details(stored)
        })
        is HubSheet.Details -> DetailsSheet(
            s.pkg,
            onDismiss = { sheet = null },
            onSend = { sheet = HubSheet.Send(it) },
            onDistribute = { sheet = HubSheet.Distribute(s.pkg) },
            onLoadMine = { sheet = HubSheet.Receive(ShareCodec.toCode(s.pkg)) },
        )
        is HubSheet.Send -> ShareTargetSheet(s.pkg, onDismiss = { sheet = null })
        is HubSheet.Distribute -> ShareDistributeSheet(container, s.pkg, onDismiss = { sheet = null })
        is HubSheet.Receive -> ShareImportSheet(container, s.text, onDismiss = { sheet = null }, onImported = { r: ImportResult ->
            sheet = null
            val id = r.firstFavoriteId
            if (r.created + r.skipped == 1 && id != null) {
                onOpenFavorite(id)
            } else {
                android.widget.Toast.makeText(context, container.appString(R.string.sh_import_done, r.created, r.skipped), android.widget.Toast.LENGTH_LONG).show()
                onOpenFavorites()
            }
        })
    }

    deleting?.let { pkg ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.sh_delete_title)) },
            text = { Text(stringResource(R.string.sh_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    container.sharedLists.delete(pkg.id)
                    deleting = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun SharedCard(pkg: SharePackage, onOpen: () -> Unit, onShare: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val all = pkg.parts.flatMap { it.refs() }
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(pkg.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (pkg.isDivision) {
                        stringResource(R.string.sh_division_summary, pkg.parts.size, summarizeChapters(all))
                    } else {
                        stringResource(R.string.sh_list_summary, summarizeChapters(all))
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onShare) { Text(stringResource(R.string.sh_share_all)) }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.action_more)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_edit)) }, onClick = {
                        menu = false
                        onEdit()
                    })
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_delete)) }, onClick = {
                        menu = false
                        onDelete()
                    })
                }
            }
        }
    }
}

/** The share's parts; for a division each part has its own send button ("send only this part to Dan"). */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun DetailsSheet(
    pkg: SharePackage,
    onDismiss: () -> Unit,
    onSend: (SharePackage) -> Unit,
    onDistribute: () -> Unit,
    onLoadMine: () -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Text(pkg.title, style = MaterialTheme.typography.titleLarge)
                FlowRow(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (pkg.isDivision) {
                        Button(onClick = onDistribute) { Text(stringResource(R.string.sh_distribute)) }
                        OutlinedButton(onClick = { onSend(pkg) }) { Text(stringResource(R.string.sh_share_all)) }
                    } else {
                        Button(onClick = { onSend(pkg) }) { Text(stringResource(R.string.sh_share_all)) }
                    }
                    OutlinedButton(onClick = onLoadMine) { Text(stringResource(R.string.sh_load_mine)) }
                }
            }
            items(pkg.parts.size) { i ->
                val part = pkg.parts[i]
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        if (part.label.isNotBlank()) Text(part.label, style = MaterialTheme.typography.titleSmall)
                        Text(partSummary(part.refs()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (pkg.isDivision) {
                        TextButton(onClick = { onSend(pkg.onlyPart(i)) }) { Text(stringResource(R.string.sh_share_part)) }
                    }
                }
            }
        }
    }
}
