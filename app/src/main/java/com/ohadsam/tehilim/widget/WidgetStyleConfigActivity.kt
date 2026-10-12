package com.ohadsam.tehilim.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.ohadsam.tehilim.ui.theme.TehilimTheme
import kotlinx.coroutines.launch

/**
 * Look settings (font, size, weight, alignment, spacing, colours, opacity, title) for the bookmark and the favorites /
 * refuah / ilui list widgets. Opens when such a widget is added and from its ⚙ button. The reading widget has its own
 * screen because it also chooses what to show.
 */
class WidgetStyleConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val container = applicationContext.container
        // The bookmark widget shows verse text; the list widgets only show names.
        val showsText = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)?.provider?.className?.endsWith("BookmarkWidgetReceiver") == true
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                TehilimTheme(container.themePrefs.settings.collectAsState().value) {
                    val scope = rememberCoroutineScope()
                    var display by remember { mutableStateOf(WidgetDisplayPrefs()) }
                    var loaded by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        val glanceId = GlanceAppWidgetManager(this@WidgetStyleConfigActivity).getGlanceIdBy(widgetId)
                        display = ReaderKeys.display(getAppWidgetState<Preferences>(this@WidgetStyleConfigActivity, PreferencesGlanceStateDefinition, glanceId))
                        loaded = true
                    }
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("הגדרות תצוגה לווידג'ט", style = MaterialTheme.typography.titleLarge)
                        StylePreview(display, container.readerPrefs.settings.collectAsState().value)
                        StyleEditor(display, { display = it }, textOptions = showsText)
                        Button(enabled = loaded, onClick = {
                            scope.launch {
                                val glanceId = GlanceAppWidgetManager(this@WidgetStyleConfigActivity).getGlanceIdBy(widgetId)
                                updateAppWidgetState(this@WidgetStyleConfigActivity, glanceId) { ReaderKeys.write(it, display) }
                                WidgetUpdater.updateAll(applicationContext)
                                setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                                finish()
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("שמירה") }
                    }
                }
            }
        }
    }
}
