package com.ohadsam.tehilim.ui.update

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.ohadsam.tehilim.BuildConfig
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.ui.share.ShareAppSection
import com.ohadsam.tehilim.update.UpdateController
import com.ohadsam.tehilim.update.UpdateError
import com.ohadsam.tehilim.update.UpdateInfo
import com.ohadsam.tehilim.update.UpdateState

/** Check for a new version, download it inside the app and install it — plus sending this app to someone else. */
@Composable
fun UpdateScreen(container: AppContainer, onBack: () -> Unit) {
    val updates = container.updates
    val state by updates.state.collectAsState()
    var auto by remember { mutableStateOf(updates.autoCheck) }
    var shareFailed by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) }
            Text(stringResource(R.string.up_title), style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.up_installed, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE))
            UpdateCard(updates, state)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.up_auto), Modifier.weight(1f))
                Switch(checked = auto, onCheckedChange = {
                    auto = it
                    updates.autoCheck = it
                })
            }
            HorizontalDivider()
            Text(stringResource(R.string.up_share_section), style = MaterialTheme.typography.titleMedium)
            ShareAppSection(onFailed = { shareFailed = true })
            if (shareFailed) Text(stringResource(R.string.set_share_failed), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun UpdateCard(updates: UpdateController, state: UpdateState) {
    val context = LocalContext.current
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (state) {
                UpdateState.Idle -> Text(stringResource(R.string.up_idle))
                UpdateState.Checking -> {
                    Text(stringResource(R.string.up_checking))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                is UpdateState.UpToDate -> Text(stringResource(R.string.up_uptodate))
                is UpdateState.Available -> AvailableBody(state.info) { updates.download(state.info) }
                is UpdateState.Downloading -> {
                    Text(stringResource(R.string.up_downloading, state.percent))
                    LinearProgressIndicator(progress = { state.percent / 100f }, modifier = Modifier.fillMaxWidth())
                    OutlinedButton(onClick = updates::cancel) { Text(stringResource(R.string.up_cancel)) }
                }
                is UpdateState.Ready -> {
                    Text(stringResource(R.string.up_ready), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.up_install_permission), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.up_install_note), style = MaterialTheme.typography.bodySmall)
                    // First press without the permission opens its settings page; after allowing, press again to install.
                    Button(onClick = { updates.installIntent()?.let { runCatching { context.startActivity(it) } } }) {
                        Text(stringResource(R.string.up_install))
                    }
                }
                is UpdateState.Failed -> Text(stringResource(errorText(state.error)), color = MaterialTheme.colorScheme.error)
            }
            val busy = state is UpdateState.Checking || state is UpdateState.Downloading
            OutlinedButton(enabled = !busy, onClick = { updates.check() }) { Text(stringResource(R.string.up_check)) }
        }
    }
}

@Composable
private fun AvailableBody(info: UpdateInfo, onDownload: () -> Unit) {
    val context = LocalContext.current
    Text(stringResource(R.string.up_available, info.versionName), style = MaterialTheme.typography.titleMedium)
    if (info.sizeBytes > 0) {
        Text(stringResource(R.string.up_available_size, Formatter.formatShortFileSize(context, info.sizeBytes)), style = MaterialTheme.typography.bodySmall)
    }
    if (info.notes.isNotBlank()) {
        Text(stringResource(R.string.up_notes), style = MaterialTheme.typography.labelLarge)
        Text(info.notes, style = MaterialTheme.typography.bodyMedium)
    }
    Button(onClick = onDownload) { Text(stringResource(R.string.up_download)) }
}

private fun errorText(e: UpdateError): Int = when (e) {
    UpdateError.OFFLINE -> R.string.up_err_offline
    UpdateError.NOT_PUBLIC -> R.string.up_err_not_public
    UpdateError.SERVER -> R.string.up_err_server
    UpdateError.BAD_FILE -> R.string.up_err_bad_file
    UpdateError.STORAGE -> R.string.up_err_storage
}
