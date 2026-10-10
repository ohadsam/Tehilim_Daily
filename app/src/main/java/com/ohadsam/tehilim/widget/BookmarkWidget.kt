package com.ohadsam.tehilim.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.ohadsam.tehilim.domain.HebrewNumerals
import kotlinx.coroutines.flow.first

private val BOOKMARK_INDEX = intPreferencesKey("bookmark_index")

/** Shows one bookmark at a time (◀ ▶ to cycle) and a "continue where I stopped" row; tap opens the exact verse. */
class BookmarkWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val bookmarks = context.container.bookmarks.bookmarks.first()
        val last = context.container.readerPrefs.lastChapter
        provideContent {
            val prefs = currentState<Preferences>()
            val index = if (bookmarks.isEmpty()) 0 else (prefs[BOOKMARK_INDEX] ?: 0).mod(bookmarks.size)
            val b = bookmarks.getOrNull(index)
            GlanceTheme(colors = widgetColors(context)) {
                Column(GlanceModifier.fillMaxSize().cornerRadius(20.dp).background(GlanceTheme.colors.surface).padding(12.dp)) {
                    if (b == null) {
                        Text("אין עדיין סימניות. החזיקו לחיצה על פסוק באפליקציה.", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp))
                    } else {
                        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("‹", style = TextStyle(color = GlanceTheme.colors.secondary, fontSize = 26.sp), modifier = GlanceModifier.padding(horizontal = 8.dp).clickable(actionRunCallback<BookmarkStep>(stepParams(-1))))
                            Column(GlanceModifier.defaultWeight().clickable(openApp(context, "browse?chapter=${b.chapter}&verse=${b.verse}"))) {
                                Text(b.label, style = TextStyle(color = GlanceTheme.colors.secondary, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center), modifier = GlanceModifier.fillMaxWidth())
                                Text(b.snippet, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 15.sp, textAlign = TextAlign.Center), modifier = GlanceModifier.fillMaxWidth(), maxLines = 4)
                            }
                            Text("›", style = TextStyle(color = GlanceTheme.colors.secondary, fontSize = 26.sp), modifier = GlanceModifier.padding(horizontal = 8.dp).clickable(actionRunCallback<BookmarkStep>(stepParams(1))))
                        }
                    }
                    if (last > 0) {
                        Text(
                            "המשך קריאה · פרק ${HebrewNumerals.toHebrew(last)}",
                            style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center),
                            modifier = GlanceModifier.fillMaxWidth().padding(top = 8.dp).clickable(openApp(context, "browse?chapter=$last&verse=0")),
                        )
                    }
                }
            }
        }
    }

    companion object {
        val StepKey = ActionParameters.Key<Int>("step")

        fun stepParams(step: Int) = androidx.glance.action.actionParametersOf(StepKey to step)
    }
}

class BookmarkStep : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val step = parameters[BookmarkWidget.StepKey] ?: 0
        updateAppWidgetState(context, glanceId) { it[BOOKMARK_INDEX] = (it[BOOKMARK_INDEX] ?: 0) + step }
        BookmarkWidget().update(context, glanceId)
    }
}

class BookmarkWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BookmarkWidget()
}
