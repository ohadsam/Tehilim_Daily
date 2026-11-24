package com.ohadsam.tehilim.ui.update

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.ohadsam.tehilim.AppContainer
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.update.UpdateState

/** Runs the daily automatic check on start and, when a new version was found, offers it once (until the user says "later"). */
@Composable
fun UpdatePrompt(container: AppContainer, onOpen: () -> Unit) {
    val updates = container.updates
    val state by updates.state.collectAsState()
    var hidden by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        updates.cleanupIfInstalled()
        updates.autoCheckIfDue()
    }
    val available = (state as? UpdateState.Available)?.info
    if (available != null && !hidden && updates.shouldOffer(available)) {
        AlertDialog(
            onDismissRequest = { hidden = true },
            title = { Text(stringResource(R.string.up_prompt_title)) },
            text = { Text(stringResource(R.string.up_prompt_body, available.versionName)) },
            confirmButton = {
                TextButton(onClick = {
                    hidden = true
                    onOpen()
                }) { Text(stringResource(R.string.up_prompt_open)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    updates.dismissOffer(available)
                    hidden = true
                }) { Text(stringResource(R.string.up_prompt_later)) }
            },
        )
    }
}
