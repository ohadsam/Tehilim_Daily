package com.ohadsam.tehilim.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
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
import com.ohadsam.tehilim.bookmarks.BookmarkText
import com.ohadsam.tehilim.domain.HebrewNumerals
import com.ohadsam.tehilim.domain.TextDisplay
import kotlinx.coroutines.flow.first

private val PENDING_DELETE = longPreferencesKey("pending_delete")
private val BookmarkIdKey = ActionParameters.Key<Long>("bookmark_id")

/**
 * All bookmarks as a scrolling list: tap a row to open the verse, ✕ (twice, to confirm) to delete, ➕ to open the app at the
 * last chapter read so a verse can be saved, ⚙ for the look. Same bookmarks as the app; the list is live (rebuilt inside the
 * composition), so adding or deleting anywhere shows up here immediately.
 */
class BookmarkWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val c = context.container
        val appWidgetId = runCatching { GlanceAppWidgetManager(context).getAppWidgetId(id) }.getOrDefault(AppWidgetManager.INVALID_APPWIDGET_ID)
        val config = if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) null else configureAction(context, appWidgetId, WidgetStyleConfigActivity::class.java)
        provideContent {
            val prefs = currentState<Preferences>()
            val bookmarks by c.bookmarks.bookmarks.collectAsState(initial = emptyList())
            val app by c.readerPrefs.settings.collectAsState()
            val reminders by c.reminders.reminders.collectAsState(initial = emptyList())
            val d = WidgetDisplay.resolve(ReaderKeys.display(prefs), app)
            val pending = prefs[PENDING_DELETE]
            val rows = BookmarkText.sorted(bookmarks)
            val last = c.readerPrefs.lastChapter
            val textColor = d.textColorProvider()
            GlanceTheme(colors = widgetColors(context)) {
                Column(GlanceModifier.fillMaxSize().cornerRadius(20.dp).background(d.backgroundColor()).padding(10.dp)) {
                    Row(GlanceModifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (d.showTitle) "סימניות (${rows.size})" else "",
                            style = TextStyle(color = GlanceTheme.colors.secondary, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                            modifier = GlanceModifier.defaultWeight().clickable(openApp(context, "bookmarks")),
                        )
                        HeaderBadges(context, reminders.count { it.enabled }, 0)
                        Pill("➕", openApp(context, "browse?chapter=${last.coerceAtLeast(1)}&verse=0"), GlanceModifier.padding(start = 4.dp))
                        config?.let { Pill("⚙", it, GlanceModifier.padding(start = 4.dp)) }
                    }
                    if (rows.isEmpty()) {
                        Text("אין עדיין סימניות. ➕ פותח את הקריאה — החזיקו לחיצה על פסוק, או לחצו על מספר פסוק בווידג'ט הקריאה.", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp))
                    }
                    LazyColumn(GlanceModifier.defaultWeight()) {
                        items(rows) { b ->
                            Row(GlanceModifier.fillMaxWidth().padding(vertical = d.spacingDp.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(GlanceModifier.defaultWeight().clickable(openApp(context, "browse?chapter=${b.chapter}&verse=${b.verse}"))) {
                                    Text(
                                        "🔖 ${b.label}",
                                        style = TextStyle(color = GlanceTheme.colors.secondary, fontSize = (d.fontSp * 0.8f).sp, fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                    )
                                    Text(
                                        TextDisplay.apply(b.snippet, d.nikud, d.taamim),
                                        style = TextStyle(color = textColor, fontSize = d.fontSp.sp, fontWeight = d.fontWeight(), fontFamily = d.fontFamily(), textAlign = d.textAlign()),
                                        maxLines = 3,
                                    )
                                }
                                val confirming = pending == b.id
                                Text(
                                    if (confirming) "למחוק? ✕" else "✕",
                                    style = TextStyle(color = if (confirming) GlanceTheme.colors.error else GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                    modifier = GlanceModifier.padding(horizontal = 10.dp, vertical = 8.dp).clickable(actionRunCallback<DeleteBookmark>(actionParametersOf(BookmarkIdKey to b.id))),
                                )
                            }
                        }
                    }
                    if (last > 0) {
                        Text(
                            "המשך קריאה · פרק ${HebrewNumerals.toHebrew(last)}",
                            style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center),
                            modifier = GlanceModifier.fillMaxWidth().padding(top = 6.dp).clickable(openApp(context, "browse?chapter=$last&verse=0")),
                        )
                    }
                }
            }
        }
    }
}

/** First tap arms the delete (the row shows "למחוק?"), second tap on the same row deletes it. */
class DeleteBookmark : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[BookmarkIdKey] ?: return
        var delete = false
        updateAppWidgetState(context, glanceId) { prefs ->
            if (prefs[PENDING_DELETE] == id) {
                delete = true
                prefs.remove(PENDING_DELETE)
            } else {
                prefs[PENDING_DELETE] = id
            }
        }
        if (delete) {
            context.container.bookmarks.delete(id)
            WidgetUpdater.updateAll(context) // the reading widget shows the same bookmarks
        } else {
            BookmarkWidget().update(context, glanceId)
        }
    }
}

class BookmarkWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BookmarkWidget()
}
