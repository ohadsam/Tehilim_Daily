package com.ohadsam.tehilim.ui.share

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ohadsam.tehilim.AppContainer
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.share.ShareSender
import com.ohadsam.tehilim.share.SharePackage
import com.ohadsam.tehilim.share.refs

/**
 * Sending a division one person at a time: pick the app once, then each row sends only that person's part (optionally with
 * their name in the greeting) and is ticked as sent; "send next" jumps to the first part not sent yet.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ShareDistributeSheet(container: AppContainer, pkg: SharePackage, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val store = container.sharedLists
    val version by store.progress.collectAsState()
    var target by remember { mutableStateOf(ShareSender.Target.WHATSAPP) }
    val failed = stringResource(R.string.set_share_failed)
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun send(index: Int) {
        val name = store.recipient(pkg.id, index).ifBlank { null }
        runCatching {
            context.startActivity(ShareSender.intent(context, pkg.onlyPart(index), target, name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onSuccess { store.setSent(pkg.id, index, true) }
            .onFailure { Toast.makeText(context, failed, Toast.LENGTH_SHORT).show() }
    }

    // `version` is read so the list is recomposed after every change in recipients / sent marks.
    val sentFlags = remember(version) { pkg.parts.indices.map { store.isSent(pkg.id, it) } }
    val sentCount = sentFlags.count { it }
    val next = sentFlags.indexOfFirst { !it }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                Text(pkg.title, style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.sh_dist_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    stringResource(R.string.sh_dist_progress, sentCount, pkg.parts.size),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )
                LinearProgressIndicator(progress = { sentCount.toFloat() / pkg.parts.size }, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        ShareSender.Target.WHATSAPP to R.string.set_share_whatsapp,
                        ShareSender.Target.EMAIL to R.string.set_share_email,
                        ShareSender.Target.ANY to R.string.set_share_other,
                    ).forEach { (t, label) ->
                        FilterChip(selected = target == t, onClick = { target = t }, label = { Text(stringResource(label)) })
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = next >= 0, onClick = { send(next) }) {
                        Text(if (next >= 0) stringResource(R.string.sh_dist_next, next + 1) else stringResource(R.string.sh_dist_all_done))
                    }
                    TextButton(enabled = sentCount > 0, onClick = { store.resetSent(pkg.id) }) { Text(stringResource(R.string.sh_dist_reset)) }
                }
                HorizontalDivider(Modifier.padding(top = 8.dp))
            }
            itemsIndexed(pkg.parts) { i, part ->
                var name by remember(i) { mutableStateOf(store.recipient(pkg.id, i)) }
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = sentFlags.getOrElse(i) { false }, onCheckedChange = { store.setSent(pkg.id, i, it) })
                        Column(Modifier.weight(1f)) {
                            Text(part.label, style = MaterialTheme.typography.titleSmall)
                            Text(partSummary(part.refs()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(onClick = { send(i) }) {
                            Text(stringResource(if (sentFlags.getOrElse(i) { false }) R.string.sh_dist_sent else R.string.sh_dist_send))
                        }
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            store.setRecipient(pkg.id, i, it)
                        },
                        singleLine = true,
                        label = { Text(stringResource(R.string.sh_dist_recipient)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
