package com.ohadsam.tehilim.ui.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.annotation.StringRes
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.widget.BookmarkWidgetReceiver
import com.ohadsam.tehilim.widget.FavoritesListWidgetReceiver
import com.ohadsam.tehilim.widget.HebrewDateWidgetReceiver
import com.ohadsam.tehilim.widget.IluiListWidgetReceiver
import com.ohadsam.tehilim.widget.ReaderFavoriteWidgetReceiver
import com.ohadsam.tehilim.widget.ReaderIluiWidgetReceiver
import com.ohadsam.tehilim.widget.ReaderRefuahWidgetReceiver
import com.ohadsam.tehilim.widget.ReaderWidgetReceiver
import com.ohadsam.tehilim.widget.RefuahListWidgetReceiver
import com.ohadsam.tehilim.widget.WeekdayWidgetReceiver
import kotlinx.coroutines.launch

private class WidgetEntry(@StringRes val name: Int, @StringRes val desc: Int, val receiver: Class<*>)

private val ENTRIES = listOf(
    WidgetEntry(R.string.widget_reader_name, R.string.widget_reader_desc, ReaderWidgetReceiver::class.java),
    WidgetEntry(R.string.widget_reader_favorite_name, R.string.widget_reader_favorite_desc, ReaderFavoriteWidgetReceiver::class.java),
    WidgetEntry(R.string.widget_reader_refuah_name, R.string.widget_reader_refuah_desc, ReaderRefuahWidgetReceiver::class.java),
    WidgetEntry(R.string.widget_reader_ilui_name, R.string.widget_reader_ilui_desc, ReaderIluiWidgetReceiver::class.java),
    WidgetEntry(R.string.widget_bookmark_name, R.string.widget_bookmark_desc, BookmarkWidgetReceiver::class.java),
    WidgetEntry(R.string.widget_favorites_name, R.string.widget_favorites_desc, FavoritesListWidgetReceiver::class.java),
    WidgetEntry(R.string.widget_refuah_name, R.string.widget_refuah_desc, RefuahListWidgetReceiver::class.java),
    WidgetEntry(R.string.widget_ilui_name, R.string.widget_ilui_desc, IluiListWidgetReceiver::class.java),
    WidgetEntry(R.string.widget_hebrew_date_name, R.string.widget_hebrew_date_desc, HebrewDateWidgetReceiver::class.java),
    WidgetEntry(R.string.widget_weekday_name, R.string.widget_weekday_desc, WeekdayWidgetReceiver::class.java),
)

/** Asks the launcher to pin [receiver] (its own confirmation dialog appears); returns false when the launcher cannot do that. */
private fun requestPin(context: Context, receiver: Class<*>): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
    val manager = AppWidgetManager.getInstance(context)
    if (!manager.isRequestPinAppWidgetSupported) return false
    return manager.requestPinAppWidget(ComponentName(context, receiver), null, null)
}

/** Every widget of the app with an "add to home screen" button (the launcher then shows its own confirmation / configuration). */
@Composable
fun AddWidgetScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val unsupported = stringResource(R.string.aw_unsupported)
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) }
            Text(stringResource(R.string.aw_title), style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.aw_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ENTRIES.forEach { e ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(e.name), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(e.desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { if (!requestPin(context, e.receiver)) scope.launch { snackbar.showSnackbar(unsupported) } }) {
                            Text(stringResource(R.string.aw_add))
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar)
    }
}
