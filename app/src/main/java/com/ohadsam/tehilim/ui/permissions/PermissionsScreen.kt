package com.ohadsam.tehilim.ui.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.permissions.PermissionChecker
import com.ohadsam.tehilim.permissions.PermissionId
import com.ohadsam.tehilim.permissions.PermissionItem

/** Automatic check of everything the app needs; re-checks every time the user comes back from a system settings page. */
@Composable
fun PermissionsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val summary = remember(tick) { PermissionChecker.check(context) }

    val notif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }
    val location = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }

    fun fix(id: PermissionId) {
        when (id) {
            PermissionId.NOTIFICATIONS ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notif.launch(Manifest.permission.POST_NOTIFICATIONS) else openAppNotificationSettings(context)
            PermissionId.CHANNELS -> openAppNotificationSettings(context)
            PermissionId.EXACT_ALARMS ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                }
            PermissionId.BATTERY -> context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            PermissionId.LOCATION -> location.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) }
            Text(stringResource(R.string.perm_title), style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                when {
                    summary.allGood -> stringResource(R.string.perm_all_good)
                    summary.missingRequired.isNotEmpty() -> stringResource(R.string.perm_missing_required, summary.missingRequired.size)
                    else -> stringResource(R.string.perm_missing_optional, summary.missingOptional.size)
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (summary.missingRequired.isEmpty()) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
            )
            summary.items.forEach { item -> PermissionCard(item, onFix = { fix(item.id) }) }
            OutlinedButton(onClick = { tick++ }) { Text(stringResource(R.string.perm_recheck)) }
        }
    }
}

@Composable
private fun PermissionCard(item: PermissionItem, onFix: () -> Unit) {
    val (title, body) = texts(item.id)
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.granted) MaterialTheme.colorScheme.surfaceVariant
            else if (item.required) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (item.granted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = stringResource(if (item.granted) R.string.perm_ok else R.string.perm_needs_attention),
                tint = if (item.granted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(stringResource(title) + if (!item.required) " " + stringResource(R.string.perm_optional_tag) else "", style = MaterialTheme.typography.titleSmall)
                Text(stringResource(body), style = MaterialTheme.typography.bodySmall)
            }
            if (!item.granted) Button(onClick = onFix) { Text(stringResource(R.string.rem_fix)) }
        }
    }
}

private fun texts(id: PermissionId): Pair<Int, Int> = when (id) {
    PermissionId.NOTIFICATIONS -> R.string.perm_notifications to R.string.perm_notifications_body
    PermissionId.CHANNELS -> R.string.perm_channels to R.string.perm_channels_body
    PermissionId.EXACT_ALARMS -> R.string.perm_exact to R.string.perm_exact_body
    PermissionId.BATTERY -> R.string.perm_battery to R.string.perm_battery_body
    PermissionId.LOCATION -> R.string.perm_location to R.string.perm_location_body
}

private fun openAppNotificationSettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
}
