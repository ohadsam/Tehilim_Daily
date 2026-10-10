package com.ohadsam.tehilim.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.ohadsam.tehilim.favorites.FavoritesState
import com.ohadsam.tehilim.favorites.PersonKind
import com.ohadsam.tehilim.ui.theme.TehilimTheme
import kotlinx.coroutines.launch

/**
 * Opens when a reading widget is added and again from the widget's ⚙ button: choose what it shows and how it looks
 * (font size, nikud/ta'amim, verse numbers, background and text colour), or follow the app's reading settings.
 */
@OptIn(ExperimentalLayoutApi::class)
class ReaderWidgetConfigActivity : ComponentActivity() {
    private data class Choice(val title: String, val subtitle: String, val source: String, val id: Long)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val container = applicationContext.container
        setContent {
            androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                TehilimTheme(container.themePrefs.settings.collectAsState().value) {
                    val state by container.favorites.state.collectAsState(initial = FavoritesState(emptyList(), emptyList()))
                    val scope = rememberCoroutineScope()
                    var source by remember { mutableStateOf(ReaderKeys.MONTH) }
                    var sourceId by remember { mutableStateOf(0L) }
                    var display by remember { mutableStateOf(WidgetDisplayPrefs()) }
                    var loaded by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        val glanceId = GlanceAppWidgetManager(this@ReaderWidgetConfigActivity).getGlanceIdBy(widgetId)
                        val p = getAppWidgetState<Preferences>(this@ReaderWidgetConfigActivity, PreferencesGlanceStateDefinition, glanceId)
                        source = p[ReaderKeys.SOURCE] ?: ReaderKeys.MONTH
                        sourceId = p[ReaderKeys.SOURCE_ID] ?: 0L
                        display = ReaderKeys.display(p)
                        loaded = true
                    }

                    val choices = buildList {
                        add(Choice("תהילים של היום בחודש", "מתעדכן אוטומטית כל יום", ReaderKeys.MONTH, 0))
                        add(Choice("תהילים של היום בשבוע", "מתעדכן אוטומטית כל יום", ReaderKeys.WEEKDAY, 0))
                        state.favorites.forEach { add(Choice(it.name, "מועדף", ReaderKeys.FAVORITE, it.id)) }
                        state.persons.forEach { add(Choice(it.displayName(), if (it.kind == PersonKind.REFUAH) "לרפואה" else "לעילוי נשמת", ReaderKeys.PERSON, it.id)) }
                    }

                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("הגדרות הווידג'ט", style = MaterialTheme.typography.titleLarge)
                        Text("מה להציג?", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                        choices.forEach { c ->
                            Row(Modifier.fillMaxWidth().clickable { source = c.source; sourceId = c.id }, verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = source == c.source && sourceId == c.id, onClick = { source = c.source; sourceId = c.id })
                                Column {
                                    Text(c.title)
                                    Text(c.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Text("תצוגה", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                        ToggleRow("ללכת לפי הגדרות הקריאה של האפליקציה (גודל, ניקוד, טעמים, מספרי פסוקים)", display.followApp) { display = display.copy(followApp = it) }
                        if (!display.followApp) {
                            Text("גודל גופן: ${display.fontSp.toInt()}")
                            Slider(display.fontSp, { display = display.copy(fontSp = it) }, valueRange = WidgetDisplay.MIN_SP..WidgetDisplay.MAX_SP)
                            ToggleRow("ניקוד", display.nikud) { display = display.copy(nikud = it) }
                            ToggleRow("טעמי מקרא", display.taamim) { display = display.copy(taamim = it) }
                            ToggleRow("מספרי פסוקים", display.numbers) { display = display.copy(numbers = it) }
                        }
                        Text("צבע רקע")
                        Swatches(BACKGROUNDS, display.background) { display = display.copy(background = it) }
                        Text("צבע טקסט")
                        Swatches(TEXTS, display.textColor) { display = display.copy(textColor = it) }
                        Text("לחיצה על מספר פסוק בווידג'ט שומרת או מסירה סימניה — אותן סימניות כמו באפליקציה. מיקום הפרק והסימון ״נאמר״ משותפים לאפליקציה.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Button(enabled = loaded, onClick = {
                            scope.launch {
                                val glanceId = GlanceAppWidgetManager(this@ReaderWidgetConfigActivity).getGlanceIdBy(widgetId)
                                updateAppWidgetState(this@ReaderWidgetConfigActivity, glanceId) {
                                    it[ReaderKeys.SOURCE] = source
                                    it[ReaderKeys.SOURCE_ID] = sourceId
                                    it[ReaderKeys.FOLLOW_APP] = display.followApp
                                    it[ReaderKeys.FONT_SP] = display.fontSp
                                    it[ReaderKeys.NIKUD] = display.nikud
                                    it[ReaderKeys.TAAMIM] = display.taamim
                                    it[ReaderKeys.NUMBERS] = display.numbers
                                    it[ReaderKeys.BACKGROUND] = display.background
                                    it[ReaderKeys.TEXT_COLOR] = display.textColor
                                }
                                ReaderWidget().update(this@ReaderWidgetConfigActivity, glanceId)
                                setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                                finish()
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("שמירה") }
                    }
                }
            }
        }
    }

    @Composable
    private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, Modifier.weight(1f))
            Switch(value, onChange)
        }
    }

    @Composable
    private fun Swatches(colors: List<Color>, selected: Int, onPick: (Int) -> Unit) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { onPick(0) }) { Text("ברירת מחדל") }
            colors.forEach { c ->
                val argb = c.toArgb()
                val sel = selected == argb
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(c)
                        .border(if (sel) 3.dp else 1.dp, if (sel) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable { onPick(argb) },
                )
            }
        }
    }

    private companion object {
        val BACKGROUNDS = listOf(0xFFFBF6EA, 0xFFFFFFFF, 0xFFF4ECD8, 0xFFEAF4EA, 0xFF1C2335, 0xFF0F1420, 0xFF000000).map { Color(it) }
        val TEXTS = listOf(0xFF1B1B1F, 0xFF000000, 0xFF3E2C00, 0xFFFFFFFF, 0xFFE6E1D6, 0xFFF2C14E).map { Color(it) }
    }
}
