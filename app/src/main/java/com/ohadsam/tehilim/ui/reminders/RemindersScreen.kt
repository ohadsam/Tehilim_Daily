package com.ohadsam.tehilim.ui.reminders

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.ohadsam.tehilim.AppContainer
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.domain.schedule.hebrewName
import com.ohadsam.tehilim.favorites.FavoritesState
import com.ohadsam.tehilim.reminders.Reminder
import com.ohadsam.tehilim.reminders.ReminderContent
import com.ohadsam.tehilim.reminders.ReminderTarget
import com.ohadsam.tehilim.reminders.Repeat
import com.ohadsam.tehilim.ui.common.formatMoment
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

/** All reminders with their next firing time, plus a "health" card for the permissions that make reminders reliable. */
@Composable
fun RemindersScreen(container: AppContainer, onOpenRoute: (String) -> Unit, onOpenLocation: () -> Unit) {
    val context = LocalContext.current
    val reminders by container.reminders.reminders.collectAsState(initial = emptyList())
    val favState by container.favorites.state.collectAsState(initial = FavoritesState(emptyList(), emptyList()))
    val bookmarks by container.bookmarks.bookmarks.collectAsState(initial = emptyList())
    var editing by remember { mutableStateOf<Reminder?>(null) }
    var creating by remember { mutableStateOf(false) }
    var health by remember { mutableIntStateOf(0) } // bump to re-evaluate permission state after returning from settings
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedMsg = stringResource(R.string.rem_deleted)
    val undo = stringResource(R.string.action_undo)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { health++ }
    LaunchedEffect(reminders) { runCatching { container.reminderScheduler.rescheduleAll() } }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { health++ }
    val notificationsOk = remember(health) { container.notifications.areEnabled() }
    val exactOk = remember(health) { container.reminderScheduler.canScheduleExact() }
    val place = remember(health) { container.locations.resolved().place }
    val planner = remember(health, place) { container.reminderScheduler.planner() }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Text(stringResource(R.string.tab_reminders), style = MaterialTheme.typography.titleLarge) }
            item {
                Card(Modifier.fillMaxWidth().clickable(onClick = onOpenLocation), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(stringResource(R.string.rem_place, place.nameHe), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(R.string.rem_place_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (!notificationsOk) {
                item {
                    HealthCard(R.string.rem_health_notifications) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                        }
                    }
                }
            }
            if (!exactOk) {
                item {
                    HealthCard(R.string.rem_health_exact) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                        }
                    }
                }
            }
            if (reminders.isEmpty()) {
                item { Text(stringResource(R.string.rem_empty), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 24.dp)) }
            }
            items(reminders, key = { it.id }) { r ->
                ReminderCard(
                    r, favState, planner, place.zone,
                    onToggle = { on -> scope.launch { val updated = r.copy(enabled = on); container.reminders.save(updated); container.reminderScheduler.schedule(updated) } },
                    onEdit = { editing = r },
                    onOpen = {
                        val today = LocalDate.now()
                        ReminderContent.build(r, today, today, com.ohadsam.tehilim.reminders.Shift.NONE, favState, bookmarks)?.let { onOpenRoute(it.route) }
                    },
                    onDelete = {
                        scope.launch {
                            container.reminderScheduler.cancel(r.id)
                            container.reminders.delete(r.id)
                            if (snackbar.showSnackbar(deletedMsg, undo, duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed) {
                                val id = container.reminders.save(r.copy(id = 0))
                                container.reminderScheduler.schedule(r.copy(id = id))
                            }
                        }
                    },
                )
            }
        }
        ExtendedFloatingActionButton(
            onClick = { creating = true },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.rem_new)) },
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    if (creating) ReminderEditorSheet(container, favState, bookmarks, null, onDismiss = { creating = false })
    editing?.let { ReminderEditorSheet(container, favState, bookmarks, it, onDismiss = { editing = null }) }
}

@Composable
private fun HealthCard(message: Int, onFix: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Text(stringResource(message), Modifier.weight(1f).padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            TextButton(onClick = onFix) { Text(stringResource(R.string.rem_fix)) }
        }
    }
}

@Composable
private fun ReminderCard(
    r: Reminder,
    favs: FavoritesState,
    planner: com.ohadsam.tehilim.reminders.ReminderPlanner,
    zone: java.time.ZoneId,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val next = remember(r, planner) { runCatching { planner.next(r, Instant.now()).firstOrNull() }.getOrNull() }
    var menu by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(r.name?.takeIf { it.isNotBlank() } ?: targetTitle(r.target, favs), style = MaterialTheme.typography.titleMedium)
                Text(
                    r.times.sorted().joinToString(" · ") { "%02d:%02d".format(it.hour, it.minute) } + " · " + repeatText(r.repeat),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (!r.enabled) stringResource(R.string.rem_off) else next?.let { stringResource(R.string.rem_next, formatMoment(Instant.ofEpochMilli(it.fireAtMillis), zone)) } ?: stringResource(R.string.rem_no_next),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary,
                )
                if (next != null && next.shift != com.ohadsam.tehilim.reminders.Shift.NONE) {
                    AssistChip(onClick = {}, label = { Text(stringResource(R.string.rem_shifted)) })
                }
            }
            Switch(checked = r.enabled, onCheckedChange = onToggle)
            Box {
                TextButton(onClick = { menu = true }) { Text("⋮") }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_edit)) }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_delete)) }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}

private fun targetTitle(t: ReminderTarget, favs: FavoritesState): String = when (t) {
    is ReminderTarget.Favorite -> favs.favorites.firstOrNull { it.id == t.id }?.name ?: "מועדף שנמחק"
    is ReminderTarget.Person -> favs.person(t.id)?.name ?: "שם שנמחק"
    is ReminderTarget.Group -> (favs.favorites.firstOrNull { it.id == t.favoriteId }?.name ?: "?") + " + " + (favs.person(t.personId)?.name ?: "?")
    ReminderTarget.MonthDayToday -> "תהילים ליום בחודש"
    ReminderTarget.WeekdayToday -> "תהילים ליום בשבוע"
    is ReminderTarget.BookmarkTarget -> "סימניה"
}

private fun repeatText(r: Repeat): String = when (r) {
    Repeat.Daily -> "כל יום"
    is Repeat.Once -> "פעם אחת · ${r.date}"
    is Repeat.Weekly -> r.days.sortedBy { it.value % 7 }.joinToString(" ") { it.hebrewName.removePrefix("יום ") }
    is Repeat.MonthlyHebrew -> "כל ${com.ohadsam.tehilim.domain.HebrewNumerals.toHebrew(r.day)} בחודש עברי"
}
