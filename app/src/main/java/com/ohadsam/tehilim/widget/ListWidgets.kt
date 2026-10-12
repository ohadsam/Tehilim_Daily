package com.ohadsam.tehilim.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
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
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.ohadsam.tehilim.favorites.Favorite
import com.ohadsam.tehilim.favorites.FavoritesState
import com.ohadsam.tehilim.favorites.Person
import com.ohadsam.tehilim.favorites.PersonKind
import com.ohadsam.tehilim.favorites.summarizeChapters
import com.ohadsam.tehilim.said.SaidContext
import androidx.glance.layout.Row as GlanceRow

private data class ListRow(val title: String, val subtitle: String, val route: String, val said: SaidContext?)

private val ListKindKey = ActionParameters.Key<String>("list_kind")
private val ListIdKey = ActionParameters.Key<String>("list_id")

/**
 * Lists favorites, or the people for refuah / ilui nishmat. Tap a row to read it; the ○/✓ on the right marks today's reading as
 * said (same marks as the app); ➕ opens the app on the matching "add" editor; ⚙ sets the look. The rows are rebuilt inside the
 * composition from live data, so changes made anywhere appear at once.
 */
abstract class ListWidget(private val kind: Kind, private val heading: String) : GlanceAppWidget() {
    enum class Kind(val addRoute: String) { FAVORITES("favorites?add=favorite"), REFUAH("favorites?add=refuah"), ILUI("favorites?add=ilui") }

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val c = context.container
        val appWidgetId = runCatching { GlanceAppWidgetManager(context).getAppWidgetId(id) }.getOrDefault(AppWidgetManager.INVALID_APPWIDGET_ID)
        val config = if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) null else configureAction(context, appWidgetId, WidgetStyleConfigActivity::class.java)
        provideContent {
            val prefs = currentState<Preferences>()
            val state by c.favorites.state.collectAsState(initial = FavoritesState(emptyList(), emptyList()))
            val saidAll by c.said.entries.collectAsState(initial = emptyList())
            val app by c.readerPrefs.settings.collectAsState()
            val reminders by c.reminders.reminders.collectAsState(initial = emptyList())
            val d = WidgetDisplay.resolve(ReaderKeys.display(prefs), app)
            val today = c.locations.logicalToday()
            val rows = rowsFor(state)
            GlanceTheme(colors = widgetColors(context)) {
                Column(GlanceModifier.fillMaxSize().cornerRadius(20.dp).background(d.backgroundColor()).padding(10.dp)) {
                    GlanceRow(GlanceModifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (d.showTitle) heading else "",
                            style = TextStyle(color = GlanceTheme.colors.secondary, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                            modifier = GlanceModifier.defaultWeight(),
                        )
                        HeaderBadges(context, reminders.count { it.enabled }, 0)
                        Pill("➕", openApp(context, kind.addRoute), GlanceModifier.padding(start = 4.dp))
                        config?.let { Pill("⚙", it, GlanceModifier.padding(start = 4.dp)) }
                    }
                    if (rows.isEmpty()) {
                        Text("אין עדיין פריטים. ➕ מוסיף חדש.", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp))
                    }
                    LazyColumn {
                        items(rows) { r ->
                            val said = r.said != null && saidAll.any { it.date == today && it.context == r.said }
                            GlanceRow(GlanceModifier.fillMaxWidth().padding(vertical = d.spacingDp.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(GlanceModifier.defaultWeight().padding(horizontal = 4.dp).clickable(openApp(context, r.route))) {
                                    Text(r.title, style = TextStyle(color = d.textColorProvider(), fontSize = d.fontSp.sp, fontWeight = d.fontWeight(), fontFamily = d.fontFamily(), textAlign = d.textAlign()), maxLines = 1)
                                    Text(r.subtitle, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = (d.fontSp * 0.75f).sp, textAlign = d.textAlign()), maxLines = 1)
                                }
                                if (r.said != null) {
                                    Pill(
                                        if (said) "✓" else "○",
                                        actionRunCallback<ToggleListSaid>(actionParametersOf(ListKindKey to r.said.type.name, ListIdKey to r.said.id)),
                                        highlighted = said,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun rowsFor(state: FavoritesState): List<ListRow> = when (kind) {
        Kind.FAVORITES -> state.favorites.map { f: Favorite -> ListRow(f.name, summarizeChapters(f.chapters), "favorite/${f.id}", SaidContext.favorite(f.id)) }
        Kind.REFUAH -> personRows(state, PersonKind.REFUAH)
        Kind.ILUI -> personRows(state, PersonKind.ILUI_NISHMAT)
    }

    private fun personRows(state: FavoritesState, k: PersonKind): List<ListRow> = state.persons.filter { it.kind == k }.map { p: Person ->
        val chapters = state.favoritesOf(p).flatMap { it.chapters }.distinct()
        ListRow(p.displayName(), if (chapters.isEmpty()) "ללא מועדף מקושר" else summarizeChapters(chapters), "person/${p.id}", SaidContext.person(p.id))
    }
}

/** Toggles today's "said" mark for one favorite / person row. */
class ToggleListSaid : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val type = parameters[ListKindKey] ?: return
        val id = parameters[ListIdKey] ?: return
        val c = context.container
        val ctx = SaidContext(com.ohadsam.tehilim.said.SaidType.valueOf(type), id)
        c.said.toggle(c.locations.logicalToday(), ctx)
        WidgetUpdater.updateAll(context)
    }
}

class FavoritesListWidget : ListWidget(ListWidget.Kind.FAVORITES, "מועדפים")

class RefuahListWidget : ListWidget(ListWidget.Kind.REFUAH, "לרפואה")

class IluiListWidget : ListWidget(ListWidget.Kind.ILUI, "לעילוי נשמת")

class FavoritesListWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FavoritesListWidget()
}

class RefuahListWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RefuahListWidget()
}

class IluiListWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = IluiListWidget()
}
