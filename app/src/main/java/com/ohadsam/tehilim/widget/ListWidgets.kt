package com.ohadsam.tehilim.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.ohadsam.tehilim.favorites.Favorite
import com.ohadsam.tehilim.favorites.FavoritesState
import com.ohadsam.tehilim.favorites.Person
import com.ohadsam.tehilim.favorites.PersonKind
import com.ohadsam.tehilim.favorites.summarizeChapters
import kotlinx.coroutines.flow.first

private data class Row(val title: String, val subtitle: String, val route: String)

/** Lists favorites, or the people for refuah / ilui nishmat; tapping a row opens it in the app. */
abstract class ListWidget(private val kind: Kind, private val heading: String) : GlanceAppWidget() {
    enum class Kind { FAVORITES, REFUAH, ILUI }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = context.container.favorites.state.first()
        val rows = rowsFor(state)
        provideContent {
            GlanceTheme(colors = widgetColors(context)) {
                Column(GlanceModifier.fillMaxSize().cornerRadius(20.dp).background(GlanceTheme.colors.surface).padding(10.dp)) {
                    Text(heading, style = TextStyle(color = GlanceTheme.colors.secondary, fontSize = 15.sp, fontWeight = FontWeight.Bold), modifier = GlanceModifier.padding(bottom = 6.dp))
                    if (rows.isEmpty()) {
                        Text("אין עדיין פריטים. הוסיפו באפליקציה.", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp))
                    }
                    LazyColumn {
                        items(rows) { r ->
                            Column(GlanceModifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 4.dp).clickable(openApp(context, r.route))) {
                                Text(r.title, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                                Text(r.subtitle, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp), maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun rowsFor(state: FavoritesState): List<Row> = when (kind) {
        Kind.FAVORITES -> state.favorites.map { f: Favorite -> Row(f.name, summarizeChapters(f.chapters), "favorite/${f.id}") }
        Kind.REFUAH -> personRows(state, PersonKind.REFUAH)
        Kind.ILUI -> personRows(state, PersonKind.ILUI_NISHMAT)
    }

    private fun personRows(state: FavoritesState, k: PersonKind): List<Row> = state.persons.filter { it.kind == k }.map { p: Person ->
        val chapters = state.favoritesOf(p).flatMap { it.chapters }.distinct()
        Row(p.displayName(), if (chapters.isEmpty()) "ללא מועדף מקושר" else summarizeChapters(chapters), "person/${p.id}")
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
