package com.ohadsam.tehilim.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ohadsam.tehilim.AppContainer
import com.ohadsam.tehilim.BuildConfig
import com.ohadsam.tehilim.R

@Composable
fun MoreScreen(container: AppContainer) {
    val context = LocalContext.current
    val store = remember { WhatsNewStore(context) }
    val whatsNew = remember { store.load() }
    var showWhatsNew by remember { mutableStateOf(store.shouldAutoShow(BuildConfig.VERSION_NAME)) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 8.dp)) {
        Text(
            stringResource(R.string.tab_more),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.more_whats_new)) },
            supportingContent = { Text(stringResource(R.string.more_version, BuildConfig.VERSION_NAME)) },
            modifier = Modifier.fillMaxWidth().clickable { showWhatsNew = true },
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text(stringResource(R.string.more_logs)) },
            supportingContent = { Text(stringResource(R.string.more_logs_count, container.logger.snapshot().size)) },
        )
    }

    if (showWhatsNew) {
        AlertDialog(
            onDismissRequest = {
                store.markSeen(BuildConfig.VERSION_NAME)
                showWhatsNew = false
            },
            title = { Text(stringResource(R.string.whats_new_title, whatsNew.version.ifEmpty { BuildConfig.VERSION_NAME })) },
            text = {
                Column {
                    if (whatsNew.items.isEmpty()) Text(stringResource(R.string.whats_new_empty))
                    whatsNew.items.forEach { Text("• $it", modifier = Modifier.padding(vertical = 2.dp)) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    store.markSeen(BuildConfig.VERSION_NAME)
                    showWhatsNew = false
                }) { Text(stringResource(R.string.whats_new_close)) }
            },
        )
    }
}
