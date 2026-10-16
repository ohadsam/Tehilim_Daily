package com.ohadsam.tehilim.ui.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.share.ShareSender
import com.ohadsam.tehilim.share.SharePackage

/** "Send via": WhatsApp, e-mail, any app, or copy the message (with the paste-able code) to the clipboard. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareTargetSheet(pkg: SharePackage, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val failed = stringResource(R.string.set_share_failed)
    val copied = stringResource(R.string.sh_copied)

    fun send(target: ShareSender.Target) {
        runCatching { context.startActivity(ShareSender.intent(context, pkg, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { Toast.makeText(context, failed, Toast.LENGTH_SHORT).show() }
        onDismiss()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(
            Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(pkg.title, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.sh_share_via), style = MaterialTheme.typography.labelLarge)
            Button(onClick = { send(ShareSender.Target.WHATSAPP) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.set_share_whatsapp))
            }
            OutlinedButton(onClick = { send(ShareSender.Target.EMAIL) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.set_share_email))
            }
            OutlinedButton(onClick = { send(ShareSender.Target.ANY) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.set_share_other))
            }
            OutlinedButton(
                onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText(pkg.title, ShareSender.message(pkg)))
                    Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.sh_copy_message)) }
        }
    }
}
