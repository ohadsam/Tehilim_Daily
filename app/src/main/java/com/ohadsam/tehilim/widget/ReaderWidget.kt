package com.ohadsam.tehilim.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.Button
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
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
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
import com.ohadsam.tehilim.calendar.HebrewCalendar
import com.ohadsam.tehilim.domain.HebrewNumerals
import com.ohadsam.tehilim.domain.PsalmRef
import com.ohadsam.tehilim.domain.Verse
import com.ohadsam.tehilim.domain.schedule.PsalmSchedules
import com.ohadsam.tehilim.domain.schedule.hebrewName
import com.ohadsam.tehilim.domain.shortLabel
import com.ohadsam.tehilim.favorites.PersonKind
import com.ohadsam.tehilim.said.SaidContext
import kotlinx.coroutines.flow.first
import java.time.LocalDate

object ReaderKeys {
    val SOURCE = stringPreferencesKey("source")
    val SOURCE_ID = longPreferencesKey("source_id")
    val INDEX = intPreferencesKey("chapter_index")
    val INDEX_DAY = longPreferencesKey("index_day")
    val StepKey = ActionParameters.Key<Int>("step")

    const val MONTH = "month"
    const val WEEKDAY = "weekday"
    const val FAVORITE = "favorite"
    const val PERSON = "person"
}

private class ReaderModel(
    val title: String,
    val personTag: String?,
    val chapters: List<PsalmRef>,
    val index: Int,
    val verses: List<Verse>?,
    val error: String?,
    val saidContext: SaidContext?,
    val said: Boolean,
    val route: String,
)

/**
 * The reading widget: shows the text of a configured source (a favorite, a person, today's month-day or weekday chapters)
 * with ◀ ▶ between the chapters and a one-tap "נאמר" that clears itself the next day (the mark is per calendar day).
 */
class ReaderWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = getAppWidgetState<Preferences>(context, PreferencesGlanceStateDefinition, id)
        val model = load(context, prefs)
        provideContent {
            GlanceTheme(colors = widgetColors(context)) { Content(context, model) }
        }
    }

    private suspend fun load(context: Context, prefs: Preferences): ReaderModel {
        val c = context.container
        val today = LocalDate.now()
        val source = prefs[ReaderKeys.SOURCE] ?: ReaderKeys.MONTH
        val sourceId = prefs[ReaderKeys.SOURCE_ID] ?: 0L
        val favs = c.favorites.state.first()
        val hebrew = HebrewCalendar.fromLocalDate(today)

        var title: String
        var tag: String? = null
        var chapters: List<PsalmRef>
        var said: SaidContext?
        var route: String
        when (source) {
            ReaderKeys.FAVORITE -> {
                val f = favs.favorites.firstOrNull { it.id == sourceId }
                title = f?.name ?: "מועדף שנמחק"
                chapters = f?.chapters.orEmpty()
                tag = f?.let { favs.personsOf(it).firstOrNull() }?.let { (if (it.kind == PersonKind.REFUAH) "לרפואת " else "לעילוי נשמת ") + it.displayName() }
                said = f?.let { SaidContext.favorite(it.id) }
                route = "favorite/$sourceId"
            }
            ReaderKeys.PERSON -> {
                val p = favs.person(sourceId)
                title = p?.name ?: "שם שנמחק"
                chapters = p?.let { favs.favoritesOf(it).flatMap { f -> f.chapters }.distinct() }.orEmpty()
                tag = p?.let { (if (it.kind == PersonKind.REFUAH) "לרפואת " else "לעילוי נשמת ") + it.displayName() }
                said = p?.let { SaidContext.person(it.id) }
                route = "person/$sourceId"
            }
            ReaderKeys.WEEKDAY -> {
                title = "תהילים ל${today.dayOfWeek.hebrewName}"
                chapters = PsalmSchedules.weekly(today.dayOfWeek)
                said = SaidContext.weekday(today)
                route = "today?mode=week"
            }
            else -> {
                title = "תהילים ליום ${HebrewNumerals.toHebrew(hebrew.day)} בחודש"
                chapters = PsalmSchedules.monthly(hebrew.day.coerceAtMost(hebrew.daysInMonth), hebrew.daysInMonth)
                said = SaidContext.monthDay(hebrew.day)
                route = "today?mode=month"
            }
        }
        // The position resets each day so the widget always starts at the first chapter of the new day.
        val index = if (prefs[ReaderKeys.INDEX_DAY] == today.toEpochDay() && chapters.isNotEmpty()) (prefs[ReaderKeys.INDEX] ?: 0).coerceIn(0, chapters.lastIndex) else 0
        var verses: List<Verse>? = null
        var error: String? = null
        chapters.getOrNull(index)?.let { ref ->
            c.psalms.get(ref).fold({ verses = it.verses }, { error = "אין חיבור והפרק עדיין לא נשמר במכשיר" })
        }
        val isSaid = said?.let { c.said.isSaid(today, it) } == true
        return ReaderModel(title, tag, chapters, index, verses, error, said, isSaid, route)
    }

    @androidx.compose.runtime.Composable
    private fun Content(context: Context, m: ReaderModel) {
        Column(GlanceModifier.fillMaxSize().cornerRadius(20.dp).background(GlanceTheme.colors.surface).padding(10.dp)) {
            Text(
                m.title, maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.secondary, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                modifier = GlanceModifier.fillMaxWidth().clickable(openApp(context, m.route)),
            )
            m.personTag?.let {
                Text(it, maxLines = 1, style = TextStyle(color = GlanceTheme.colors.onSecondaryContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                    modifier = GlanceModifier.fillMaxWidth().background(GlanceTheme.colors.secondaryContainer).padding(2.dp))
            }
            if (m.chapters.isEmpty()) {
                Text("אין פרקים להצגה", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp), modifier = GlanceModifier.padding(8.dp))
                return
            }
            Row(GlanceModifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("‹", style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 28.sp), modifier = GlanceModifier.padding(horizontal = 12.dp).clickable(actionRunCallback<ReaderStep>(androidx.glance.action.actionParametersOf(ReaderKeys.StepKey to -1))))
                Text(
                    "${m.chapters[m.index].shortLabel()}  (${HebrewNumerals.toHebrew(m.index + 1)}/${HebrewNumerals.toHebrew(m.chapters.size)})",
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center),
                    modifier = GlanceModifier.defaultWeight(),
                )
                Text("›", style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 28.sp), modifier = GlanceModifier.padding(horizontal = 12.dp).clickable(actionRunCallback<ReaderStep>(androidx.glance.action.actionParametersOf(ReaderKeys.StepKey to 1))))
            }
            Column(GlanceModifier.defaultWeight().fillMaxWidth()) {
                val verses = m.verses
                when {
                    verses != null -> LazyColumn {
                        items(verses) { v ->
                            Text(
                                "${HebrewNumerals.toHebrew(v.number, punctuation = false)}  ${v.text}",
                                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp),
                                modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp),
                            )
                        }
                    }
                    m.error != null -> Text(m.error, style = TextStyle(color = GlanceTheme.colors.error, fontSize = 12.sp), modifier = GlanceModifier.padding(8.dp))
                }
            }
            if (m.saidContext != null) {
                Button(
                    text = if (m.said) "נאמר ✓" else "סימון: נאמר",
                    onClick = actionRunCallback<ToggleSaid>(),
                    modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        }
    }
}

class ReaderStep : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val step = parameters[ReaderKeys.StepKey] ?: 0
        val today = LocalDate.now().toEpochDay()
        updateAppWidgetState(context, glanceId) { p ->
            val base = if (p[ReaderKeys.INDEX_DAY] == today) (p[ReaderKeys.INDEX] ?: 0) else 0
            p[ReaderKeys.INDEX] = (base + step).coerceAtLeast(0) // upper bound is clamped when the content is loaded
            p[ReaderKeys.INDEX_DAY] = today
        }
        ReaderWidget().update(context, glanceId)
    }
}

class ToggleSaid : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val prefs = getAppWidgetState<Preferences>(context, PreferencesGlanceStateDefinition, glanceId)
        val today = LocalDate.now()
        val hebrew = HebrewCalendar.fromLocalDate(today)
        val ctx = when (prefs[ReaderKeys.SOURCE] ?: ReaderKeys.MONTH) {
            ReaderKeys.FAVORITE -> SaidContext.favorite(prefs[ReaderKeys.SOURCE_ID] ?: 0L)
            ReaderKeys.PERSON -> SaidContext.person(prefs[ReaderKeys.SOURCE_ID] ?: 0L)
            ReaderKeys.WEEKDAY -> SaidContext.weekday(today)
            else -> SaidContext.monthDay(hebrew.day)
        }
        context.container.said.toggle(today, ctx)
        WidgetUpdater.updateAll(context)
    }
}

class ReaderWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ReaderWidget()
}
