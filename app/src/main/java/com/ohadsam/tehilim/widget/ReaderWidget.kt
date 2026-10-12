package com.ohadsam.tehilim.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
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
import androidx.glance.unit.ColorProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.ohadsam.tehilim.favorites.FavoritesState
import com.ohadsam.tehilim.calendar.HebrewCalendar
import com.ohadsam.tehilim.data.prefs.PositionStore
import com.ohadsam.tehilim.domain.HebrewNumerals
import com.ohadsam.tehilim.domain.PsalmRef
import com.ohadsam.tehilim.domain.TextDisplay
import com.ohadsam.tehilim.domain.Verse
import com.ohadsam.tehilim.domain.schedule.PsalmSchedules
import com.ohadsam.tehilim.domain.schedule.hebrewName
import com.ohadsam.tehilim.domain.shortLabel
import com.ohadsam.tehilim.favorites.PersonKind
import com.ohadsam.tehilim.said.SaidContext
import kotlinx.coroutines.flow.first

object ReaderKeys {
    val SOURCE = stringPreferencesKey("source")
    val SOURCE_ID = longPreferencesKey("source_id")
    val FOLLOW_APP = booleanPreferencesKey("follow_app")
    val FONT_SP = floatPreferencesKey("font_sp")
    val NIKUD = booleanPreferencesKey("nikud")
    val TAAMIM = booleanPreferencesKey("taamim")
    val NUMBERS = booleanPreferencesKey("numbers")
    val BACKGROUND = intPreferencesKey("background")
    val TEXT_COLOR = intPreferencesKey("text_color")
    val BOLD = booleanPreferencesKey("bold")
    val FAMILY = stringPreferencesKey("family")
    val CENTERED = booleanPreferencesKey("centered")
    val SPACING = intPreferencesKey("spacing")
    val BG_ALPHA = floatPreferencesKey("bg_alpha")
    val SHOW_TITLE = booleanPreferencesKey("show_title")
    val StepKey = ActionParameters.Key<Int>("step")
    val ChapterKey = ActionParameters.Key<Int>("chapter")
    val VerseKey = ActionParameters.Key<Int>("verse")

    const val MONTH = "month"
    const val WEEKDAY = "weekday"
    const val FAVORITE = "favorite"
    const val PERSON = "person"

    fun display(p: Preferences) = WidgetDisplayPrefs(
        followApp = p[FOLLOW_APP] ?: true,
        fontSp = p[FONT_SP] ?: 16f,
        nikud = p[NIKUD] ?: true,
        taamim = p[TAAMIM] ?: false,
        numbers = p[NUMBERS] ?: true,
        background = p[BACKGROUND] ?: 0,
        textColor = p[TEXT_COLOR] ?: 0,
        bold = p[BOLD] ?: false,
        family = p[FAMILY] ?: "sans",
        centered = p[CENTERED] ?: false,
        spacingDp = p[SPACING] ?: 3,
        bgAlpha = p[BG_ALPHA] ?: 1f,
        showTitle = p[SHOW_TITLE] ?: true,
    )

    /** Writes every display option; shared by the reading and the style configuration screens. */
    fun write(p: androidx.datastore.preferences.core.MutablePreferences, d: WidgetDisplayPrefs) {
        p[FOLLOW_APP] = d.followApp
        p[FONT_SP] = d.fontSp
        p[NIKUD] = d.nikud
        p[TAAMIM] = d.taamim
        p[NUMBERS] = d.numbers
        p[BACKGROUND] = d.background
        p[TEXT_COLOR] = d.textColor
        p[BOLD] = d.bold
        p[FAMILY] = d.family
        p[CENTERED] = d.centered
        p[SPACING] = d.spacingDp
        p[BG_ALPHA] = d.bgAlpha
        p[SHOW_TITLE] = d.showTitle
    }
}

/** What a reading widget shows today: the chapters, what "נאמר" marks, where tapping the title goes and the shared position key. */
internal class ResolvedSource(
    val title: String,
    val personTag: String?,
    val chapters: List<PsalmRef>,
    val saidContext: SaidContext?,
    val route: String,
    val positionKey: String,
    val today: java.time.LocalDate,
)

internal suspend fun resolveSource(context: Context, prefs: Preferences): ResolvedSource {
    val c = context.container
    val today = c.locations.logicalToday()
    val source = prefs[ReaderKeys.SOURCE] ?: ReaderKeys.MONTH
    val id = prefs[ReaderKeys.SOURCE_ID] ?: 0L
    val favs = c.favorites.state.first()
    val hebrew = HebrewCalendar.fromLocalDate(today)
    return when (source) {
        ReaderKeys.FAVORITE -> {
            val f = favs.favorites.firstOrNull { it.id == id }
            ResolvedSource(
                f?.name ?: "מועדף שנמחק", f?.let { favs.personsOf(it).firstOrNull() }?.let { (if (it.kind == PersonKind.REFUAH) "לרפואת " else "לעילוי נשמת ") + it.displayName() },
                f?.chapters.orEmpty(), f?.let { SaidContext.favorite(it.id) }, "favorite/$id", PositionStore.key("f", id, today), today,
            )
        }
        ReaderKeys.PERSON -> {
            val p = favs.person(id)
            ResolvedSource(
                p?.name ?: "שם שנמחק", p?.let { (if (it.kind == PersonKind.REFUAH) "לרפואת " else "לעילוי נשמת ") + it.displayName() },
                p?.let { favs.favoritesOf(it).flatMap { f -> f.chapters }.distinct() }.orEmpty(), p?.let { SaidContext.person(it.id) },
                "person/$id", PositionStore.key("p", id, today), today,
            )
        }
        ReaderKeys.WEEKDAY -> ResolvedSource(
            "תהילים ל${today.dayOfWeek.hebrewName}", null, PsalmSchedules.weekly(today.dayOfWeek), SaidContext.weekday(today),
            "today?mode=week", PositionStore.key("w", 0, today), today,
        )
        else -> ResolvedSource(
            "תהילים ליום ${HebrewNumerals.toHebrew(hebrew.day)} בחודש",
            null, PsalmSchedules.monthly(hebrew.day.coerceAtMost(hebrew.daysInMonth), hebrew.daysInMonth), SaidContext.monthDay(hebrew.day),
            "today?mode=month", PositionStore.key("m", 0, today), today,
        )
    }
}

private class ReaderModel(
    val src: ResolvedSource,
    val index: Int,
    val verses: List<Verse>?,
    val error: String?,
    val said: Boolean,
    val display: ResolvedDisplay,
    val bookmarkedVerses: Set<Int>,
    val bookmarkCount: Int,
    val reminderCount: Int,
    val configAction: Action?,
)

/**
 * The reading widget. Navigation position, "נאמר" marks and bookmarks are the same data the app uses, so what is done
 * here appears in the app and vice versa; display options are per widget (or follow the app's reading settings).
 *
 * The model is rebuilt INSIDE the composition from live flows: Glance reuses a running session on update() and only
 * recomposes, so data captured once in provideGlance would never change (that is why paging used to do nothing).
 */
class ReaderWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val c = context.container
        val first = load(context, id, getAppWidgetState<Preferences>(context, PreferencesGlanceStateDefinition, id))
        provideContent {
            val prefs = currentState<Preferences>()
            val position by c.positions.version.collectAsState()
            val marks by c.bookmarks.bookmarks.collectAsState(initial = emptyList())
            val said by c.said.entries.collectAsState(initial = emptyList())
            val favs by c.favorites.state.collectAsState(initial = FavoritesState(emptyList(), emptyList()))
            val app by c.readerPrefs.settings.collectAsState()
            val reminders by c.reminders.reminders.collectAsState(initial = emptyList())
            val model by produceState(first, prefs, position, marks, said, favs, app, reminders) { value = load(context, id, prefs) }
            GlanceTheme(colors = widgetColors(context)) { Content(context, model) }
        }
    }

    private suspend fun load(context: Context, id: GlanceId, prefs: Preferences): ReaderModel {
        val c = context.container
        val src = resolveSource(context, prefs)
        val index = if (src.chapters.isEmpty()) 0 else c.positions.get(src.positionKey).coerceIn(0, src.chapters.lastIndex)
        var verses: List<Verse>? = null
        var error: String? = null
        val ref = src.chapters.getOrNull(index)
        ref?.let { r -> c.psalms.get(r).fold({ verses = it.verses }, { error = "אין חיבור והפרק עדיין לא נשמר במכשיר" }) }
        val allMarks = c.bookmarks.bookmarks.first()
        val marks = if (ref != null) allMarks.filter { it.chapter == ref.chapter }.map { it.verse }.toSet() else emptySet()
        val appWidgetId = runCatching { GlanceAppWidgetManager(context).getAppWidgetId(id) }.getOrDefault(AppWidgetManager.INVALID_APPWIDGET_ID)
        val config = if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) null else configureAction(context, appWidgetId, ReaderWidgetConfigActivity::class.java)
        return ReaderModel(
            src, index, verses, error, src.saidContext?.let { c.said.isSaid(src.today, it) } == true,
            WidgetDisplay.resolve(ReaderKeys.display(prefs), c.readerPrefs.settings.value), marks,
            allMarks.size, c.reminders.snapshot().count { it.enabled }, config,
        )
    }

    @androidx.compose.runtime.Composable
    private fun Content(context: Context, m: ReaderModel) {
        val d = m.display
        val textColor = d.textColorProvider()
        Column(GlanceModifier.fillMaxSize().cornerRadius(20.dp).background(d.backgroundColor()).padding(10.dp)) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (d.showTitle) {
                    Text(
                        m.src.title, maxLines = 1,
                        style = TextStyle(color = GlanceTheme.colors.secondary, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Start),
                        modifier = GlanceModifier.defaultWeight().clickable(openApp(context, m.src.route)),
                    )
                } else {
                    Row(GlanceModifier.defaultWeight().clickable(openApp(context, m.src.route))) {}
                }
                HeaderBadges(context, m.reminderCount, m.bookmarkCount)
                m.configAction?.let { Pill("⚙", it, GlanceModifier.padding(start = 4.dp)) }
            }
            m.src.personTag?.let {
                Text(it, maxLines = 1, style = TextStyle(color = GlanceTheme.colors.onSecondaryContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                    modifier = GlanceModifier.fillMaxWidth().background(GlanceTheme.colors.secondaryContainer).padding(2.dp))
            }
            if (m.src.chapters.isEmpty()) {
                Text("אין פרקים להצגה", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp), modifier = GlanceModifier.padding(8.dp))
            } else {
                if (m.src.chapters.size > 1) {
                    // The first child sits on the right in RTL, so "previous" is first and points right.
                    Row(GlanceModifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        NavArrow("›", m.index > 0, -1)
                        Text(
                            "${m.src.chapters[m.index].shortLabel()}  (${HebrewNumerals.toHebrew(m.index + 1)}/${HebrewNumerals.toHebrew(m.src.chapters.size)})",
                            style = TextStyle(color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center),
                            modifier = GlanceModifier.defaultWeight(),
                        )
                        NavArrow("‹", m.index < m.src.chapters.lastIndex, 1)
                    }
                } else {
                    Text(m.src.chapters[0].shortLabel(), style = TextStyle(color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center), modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp))
                }
                Column(GlanceModifier.defaultWeight().fillMaxWidth()) {
                    val verses = m.verses
                    val chapter = m.src.chapters[m.index].chapter
                    when {
                        verses != null -> LazyColumn {
                            items(verses) { v ->
                                Row(GlanceModifier.fillMaxWidth().padding(vertical = d.spacingDp.dp), verticalAlignment = Alignment.Top) {
                                    // Tapping the verse number toggles a bookmark on that verse (same bookmarks as in the app).
                                    val marked = v.number in m.bookmarkedVerses
                                    Text(
                                        (if (marked) "🔖" else "") + if (d.numbers || marked) HebrewNumerals.toHebrew(v.number, punctuation = false) else "·",
                                        style = TextStyle(color = GlanceTheme.colors.secondary, fontSize = (d.fontSp * 0.7f).sp),
                                        modifier = GlanceModifier.padding(end = 6.dp).clickable(
                                            actionRunCallback<ToggleBookmark>(actionParametersOf(ReaderKeys.ChapterKey to chapter, ReaderKeys.VerseKey to v.number)),
                                        ),
                                    )
                                    Text(
                                        TextDisplay.apply(v.text, d.nikud, d.taamim),
                                        style = TextStyle(color = textColor, fontSize = d.fontSp.sp, fontWeight = d.fontWeight(), fontFamily = d.fontFamily(), textAlign = d.textAlign()),
                                        modifier = GlanceModifier.defaultWeight(),
                                    )
                                }
                            }
                        }
                        m.error != null -> Text(m.error, style = TextStyle(color = GlanceTheme.colors.error, fontSize = 12.sp), modifier = GlanceModifier.padding(8.dp))
                    }
                }
            }
            if (m.src.saidContext != null) SaidPill(m.said, actionRunCallback<ToggleSaid>())
        }
    }

    /** Paging arrow: a rounded pill with a generous tap area; dimmed (and inert) at the ends. */
    @androidx.compose.runtime.Composable
    private fun NavArrow(glyph: String, enabled: Boolean, step: Int) {
        val mod = GlanceModifier.background(GlanceTheme.colors.secondaryContainer).cornerRadius(16.dp).padding(horizontal = 16.dp, vertical = 4.dp)
        Text(
            glyph,
            style = TextStyle(color = if (enabled) GlanceTheme.colors.onSecondaryContainer else GlanceTheme.colors.outline, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            modifier = if (enabled) mod.clickable(actionRunCallback<ReaderStep>(actionParametersOf(ReaderKeys.StepKey to step))) else mod,
        )
    }
}

class ReaderStep : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val step = parameters[ReaderKeys.StepKey] ?: 0
        val prefs = getAppWidgetState<Preferences>(context, PreferencesGlanceStateDefinition, glanceId)
        val src = resolveSource(context, prefs)
        if (src.chapters.isEmpty()) return
        val next = (context.container.positions.get(src.positionKey) + step).coerceIn(0, src.chapters.lastIndex)
        context.container.positions.set(src.positionKey, next) // shared with the app
        WidgetUpdater.updateAll(context)
    }
}

class ToggleSaid : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val prefs = getAppWidgetState<Preferences>(context, PreferencesGlanceStateDefinition, glanceId)
        val src = resolveSource(context, prefs)
        src.saidContext?.let { context.container.said.toggle(src.today, it) }
        WidgetUpdater.updateAll(context)
    }
}

class ToggleBookmark : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val chapter = parameters[ReaderKeys.ChapterKey] ?: return
        val verse = parameters[ReaderKeys.VerseKey] ?: return
        val c = context.container
        val existing = c.bookmarks.bookmarks.first().firstOrNull { it.chapter == chapter && it.verse == verse }
        if (existing != null) {
            c.bookmarks.delete(existing.id)
        } else {
            val text = c.psalms.get(PsalmRef(chapter)).getOrNull()?.verses?.firstOrNull { it.number == verse }?.text ?: return
            c.bookmarks.add(chapter, verse, text)
        }
        WidgetUpdater.updateAll(context)
    }
}

class ReaderWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ReaderWidget()
}
