package com.ohadsam.tehilim.ui.share

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ohadsam.tehilim.BuildConfig
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.share.AppSharer

/** Sends the installed APK with install instructions: WhatsApp, e-mail or any app (optionally zipped for mail apps that block .apk). */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ShareAppSection(onFailed: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var asZip by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.set_share_intro), style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                R.string.set_share_whatsapp to AppSharer.Target.WHATSAPP,
                R.string.set_share_email to AppSharer.Target.EMAIL,
                R.string.set_share_other to AppSharer.Target.ANY,
            ).forEach { (label, target) ->
                OutlinedButton(onClick = {
                    val ok = runCatching {
                        val v = BuildConfig.VERSION_NAME
                        val file = AppSharer.prepare(context, v, asZip)
                        context.startActivity(AppSharer.intent(context, file, v, asZip, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }.isSuccess
                    if (!ok) onFailed()
                }) { Text(stringResource(label)) }
            }
        }
        FilterChip(asZip, { asZip = !asZip }, { Text(stringResource(R.string.set_share_zip)) })
    }
}
