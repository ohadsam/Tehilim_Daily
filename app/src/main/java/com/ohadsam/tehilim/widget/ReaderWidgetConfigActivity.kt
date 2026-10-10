package com.ohadsam.tehilim.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.ohadsam.tehilim.favorites.FavoritesState
import com.ohadsam.tehilim.favorites.PersonKind
import com.ohadsam.tehilim.ui.theme.TehilimTheme
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Shown when the user drops a reading widget on the home screen: choose what it should display. */
class ReaderWidgetConfigActivity : ComponentActivity() {
    private data class Choice(val title: String, val subtitle: String?, val source: String, val id: Long)

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
                TehilimTheme {
                    val state by container.favorites.state.collectAsState(initial = FavoritesState(emptyList(), emptyList()))
                    val scope = rememberCoroutineScope()
                    val choices = buildList {
                        add(Choice("תהילים של היום בחודש", "מתעדכן אוטומטית כל יום", ReaderKeys.MONTH, 0))
                        add(Choice("תהילים של היום בשבוע", "מתעדכן אוטומטית כל יום", ReaderKeys.WEEKDAY, 0))
                        state.favorites.forEach { add(Choice(it.name, "מועדף", ReaderKeys.FAVORITE, it.id)) }
                        state.persons.forEach {
                            add(Choice(it.displayName(), if (it.kind == PersonKind.REFUAH) "לרפואה" else "לעילוי נשמת", ReaderKeys.PERSON, it.id))
                        }
                    }
                    Column(Modifier.fillMaxWidth().padding(top = 24.dp)) {
                        Text("מה להציג בווידג'ט?", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
                        LazyColumn {
                            items(choices) { c ->
                                ListItem(
                                    headlineContent = { Text(c.title) },
                                    supportingContent = c.subtitle?.let { { Text(it) } },
                                    modifier = Modifier.clickable {
                                        scope.launch {
                                            val glanceId = GlanceAppWidgetManager(this@ReaderWidgetConfigActivity).getGlanceIdBy(widgetId)
                                            updateAppWidgetState(this@ReaderWidgetConfigActivity, glanceId) {
                                                it[ReaderKeys.SOURCE] = c.source
                                                it[ReaderKeys.SOURCE_ID] = c.id
                                                it[ReaderKeys.INDEX] = 0
                                                it[ReaderKeys.INDEX_DAY] = applicationContext.container.locations.logicalToday().toEpochDay()
                                            }
                                            ReaderWidget().update(this@ReaderWidgetConfigActivity, glanceId)
                                            setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                                            finish()
                                        }
                                    },
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}
