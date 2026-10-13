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
import com.ohadsam.tehilim.favorites.summarizeChapters
import androidx.glance.layout.size
import com.ohadsam.tehilim.said.SaidContext
import kotlinx.coroutines.flow.first

private val GREGORIAN = java.time.format.DateTimeFormatter.ofPattern("d 'ב'MMMM yyyy", java.util.Locale("he"))

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
    val LANDING = booleanPreferencesKey("landing")
    /** "Add bookmark" mode: only then the per-verse ＋ buttons are shown (keeps the text clean the rest of the time). */
    val BM_MODE = booleanPreferencesKey("bookmark_mode")
    /** positionKey (source + date) for which the user already left the landing screen; a new day or a new source shows it again. */
    val LANDING_SEEN = stringPreferencesKey("landing_seen")
    /** chapter * 1000 + verse of the bookmark whose delete button was tapped once (second tap deletes). */
    val PENDING_BM = intPreferencesKey("pending_bookmark")
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
        landing = p[LANDING] ?: true,
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
        p[LANDING] = d.landing
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
    val pendingDelete: Int,
    val configAction: Action?,
    val landingEnabled: Boolean,
    val landing: Boolean,
    val hebrewDate: String,
    val gregorianDate: String,
    val bmMode: Boolean,
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
        val landingEnabled = ReaderKeys.display(prefs).landing && src.chapters.isNotEmpty()
        val landing = landingEnabled && prefs[ReaderKeys.LANDING_SEEN] != src.positionKey
        val hebrewDate = HebrewCalendar.fromLocalDate(src.today).format()
        val gregorianDate = "${src.today.dayOfWeek.hebrewName} · ${GREGORIAN.format(src.today)}"
        return ReaderModel(
            src, index, verses, error, src.saidContext?.let { c.said.isSaid(src.today, it) } == true,
            WidgetDisplay.resolve(ReaderKeys.display(prefs), c.readerPrefs.settings.value), marks,
            allMarks.size, c.reminders.snapshot().count { it.enabled }, prefs[ReaderKeys.PENDING_BM] ?: 0, config,
            landingEnabled, landing, hebrewDate, gregorianDate, prefs[ReaderKeys.BM_MODE] == true,
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
                HeaderBadges(context, m.reminderCount, 0)
                if (!m.landing) {
                    // One bookmark button: tap to show the ＋ buttons next to the verses, tap again (or add one) to hide them.
                    Pill(if (m.bmMode) "🔖 ✓" else "🔖＋", actionRunCallback<ToggleBookmarkMode>(), GlanceModifier.padding(start = 4.dp), highlighted = m.bmMode)
                    if (m.bmMode && m.bookmarkCount > 0) Pill("הכל (${m.bookmarkCount})", openApp(context, "bookmarks"), GlanceModifier.padding(start = 4.dp))
                }
                m.configAction?.let { Pill("⚙", it, GlanceModifier.padding(start = 4.dp)) }
            }
            m.src.personTag?.let {
                Text(it, maxLines = 1, style = TextStyle(color = GlanceTheme.colors.onSecondaryContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                    modifier = GlanceModifier.fillMaxWidth().background(GlanceTheme.colors.secondaryContainer).padding(2.dp))
            }
            if (m.landing) {
                LandingBody(context, m)
            } else             if (m.src.chapters.isEmpty()) {
                Text("אין פרקים להצגה", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp), modifier = GlanceModifier.padding(8.dp))
            } else {
                if (m.src.chapters.size > 1 || m.landingEnabled) {
                    // The first child sits on the right in RTL, so "previous" is first and points right.
                    Row(GlanceModifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        NavArrow(rtlArrow(context, previous = true), m.index > 0 || m.landingEnabled, -1)
                        Text(
                            progressLabel(m.src.chapters, m.index),
                            style = TextStyle(color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center),
                            modifier = GlanceModifier.defaultWeight(),
                        )
                        NavArrow(rtlArrow(context, previous = false), m.index < m.src.chapters.lastIndex, 1)
                    }
                } else {
                    Text(m.src.chapters[0].shortLabel(), style = TextStyle(color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center), modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp))
                }
                Column(GlanceModifier.defaultWeight().fillMaxWidth()) {
                    val verses = m.verses
                    val chapter = m.src.chapters[m.index].chapter
                    when {
                        verses != null -> ParityBox(m.index % 2 == 1) { LazyColumn {
                            items(verses, itemId = { chapter * 1000L + it.number }) { v ->
                                val marked = v.number in m.bookmarkedVerses
                                val confirming = m.pendingDelete == chapter * 1000 + v.number
                                Row(GlanceModifier.fillMaxWidth().padding(vertical = d.spacingDp.dp), verticalAlignment = Alignment.Top) {
                                    Text(
                                        if (d.numbers) HebrewNumerals.toHebrew(v.number, punctuation = false) else "·",
                                        style = TextStyle(color = GlanceTheme.colors.secondary, fontSize = (d.fontSp * 0.7f).sp),
                                        modifier = GlanceModifier.padding(end = 6.dp),
                                    )
                                    Text(
                                        TextDisplay.apply(v.text, d.nikud, d.taamim),
                                        style = TextStyle(color = textColor, fontSize = d.fontSp.sp, fontWeight = d.fontWeight(), fontFamily = d.fontFamily(), textAlign = d.textAlign()),
                                        modifier = GlanceModifier.defaultWeight(),
                                    )
                                    // Bookmark controls: 🔖 adds; on a bookmarked verse 🔖 opens it in the app (edit) and ✕ deletes (tap twice).
                                    if (marked) {
                                        Text(
                                            "🔖",
                                            style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 15.sp),
                                            modifier = GlanceModifier.padding(horizontal = 6.dp, vertical = 2.dp).clickable(openApp(context, "browse?chapter=$chapter&verse=${v.number}")),
                                        )
                                        Text(
                                            if (confirming) "למחוק? ✕" else "✕",
                                            style = TextStyle(color = if (confirming) GlanceTheme.colors.error else GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                            modifier = GlanceModifier.padding(horizontal = 4.dp, vertical = 2.dp).clickable(
                                                actionRunCallback<DeleteVerseBookmark>(actionParametersOf(ReaderKeys.ChapterKey to chapter, ReaderKeys.VerseKey to v.number)),
                                            ),
                                        )
                                    } else if (m.bmMode) {
                                        Pill(
                                            "＋",
                                            actionRunCallback<AddBookmark>(actionParametersOf(ReaderKeys.ChapterKey to chapter, ReaderKeys.VerseKey to v.number)),
                                            GlanceModifier.padding(start = 6.dp),
                                        )
                                    }
                                }
                            }
                        } }
                        m.error != null -> Text(m.error, style = TextStyle(color = GlanceTheme.colors.error, fontSize = 12.sp), modifier = GlanceModifier.padding(8.dp))
                    }
                }
            }
            if (m.src.saidContext != null && !m.landing) SaidPill(m.said, actionRunCallback<ToggleSaid>())
        }
    }

    /** The first screen of the day: app icon, Hebrew + Gregorian date, what is being read and the chapters of today; "next" starts the reading. */
    @androidx.compose.runtime.Composable
    private fun androidx.glance.layout.ColumnScope.LandingBody(context: Context, m: ReaderModel) {
        val d = m.display
        Column(GlanceModifier.defaultWeight().fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.glance.Image(
                provider = androidx.glance.ImageProvider(com.ohadsam.tehilim.R.mipmap.ic_launcher),
                contentDescription = "תהילים יומי",
                modifier = GlanceModifier.size(52.dp),
            )
            Text(m.hebrewDate, style = TextStyle(color = GlanceTheme.colors.secondary, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center), modifier = GlanceModifier.fillMaxWidth().padding(top = 6.dp))
            Text(m.gregorianDate, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp, textAlign = TextAlign.Center), modifier = GlanceModifier.fillMaxWidth())
            Text(m.src.title, style = TextStyle(color = d.textColorProvider(), fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center), modifier = GlanceModifier.fillMaxWidth().padding(top = 8.dp), maxLines = 2)
            m.src.personTag?.let {
                Text(it, maxLines = 1, style = TextStyle(color = GlanceTheme.colors.onSecondaryContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center), modifier = GlanceModifier.padding(top = 2.dp))
            }
            Text(
                "פרקי היום: " + summarizeChapters(m.src.chapters),
                style = TextStyle(color = d.textColorProvider(), fontSize = 15.sp, textAlign = TextAlign.Center),
                modifier = GlanceModifier.fillMaxWidth().padding(top = 8.dp),
                maxLines = 4,
            )
        }
        // Same paging row as the reading view: "previous" is inert here, "next" starts the reading.
        Row(GlanceModifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            NavArrow(rtlArrow(context, previous = true), false, -1)
            Text("לדפדוף לקריאה", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp, textAlign = TextAlign.Center), modifier = GlanceModifier.defaultWeight())
            NavArrow(rtlArrow(context, previous = false), true, 1)
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

/**
 * Glance keeps a list's scroll position across updates. Alternating between two different tree shapes (and giving every verse a
 * chapter-specific item id) makes each new chapter start at its top.
 */
@androidx.compose.runtime.Composable
private fun ParityBox(odd: Boolean, content: @androidx.compose.runtime.Composable () -> Unit) {
    if (odd) androidx.glance.layout.Box(GlanceModifier.fillMaxSize()) { content() } else content()
}

class ReaderStep : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val step = parameters[ReaderKeys.StepKey] ?: 0
        val prefs = getAppWidgetState<Preferences>(context, PreferencesGlanceStateDefinition, glanceId)
        val src = resolveSource(context, prefs)
        if (src.chapters.isEmpty()) return
        val landingEnabled = ReaderKeys.display(prefs).landing
        val seen = prefs[ReaderKeys.LANDING_SEEN] == src.positionKey
        val current = context.container.positions.get(src.positionKey)
        val next = stepReader(landingEnabled, seen, current, src.chapters.size, step)
        context.container.positions.set(src.positionKey, next.index) // shared with the app
        updateAppWidgetState(context, glanceId) {
            if (next.landingSeen) it[ReaderKeys.LANDING_SEEN] = src.positionKey else it.remove(ReaderKeys.LANDING_SEEN)
        }
        updateAppWidgetState(context, glanceId) {
            it.remove(ReaderKeys.PENDING_BM)
            it[ReaderKeys.BM_MODE] = false
        }
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

class ToggleBookmarkMode : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, glanceId) { it[ReaderKeys.BM_MODE] = it[ReaderKeys.BM_MODE] != true }
        ReaderWidget().update(context, glanceId)
    }
}

class AddBookmark : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val chapter = parameters[ReaderKeys.ChapterKey] ?: return
        val verse = parameters[ReaderKeys.VerseKey] ?: return
        val c = context.container
        if (c.bookmarks.bookmarks.first().none { it.chapter == chapter && it.verse == verse }) {
            val text = c.psalms.get(PsalmRef(chapter)).getOrNull()?.verses?.firstOrNull { it.number == verse }?.text ?: return
            c.bookmarks.add(chapter, verse, text)
        }
        updateAppWidgetState(context, glanceId) { it[ReaderKeys.BM_MODE] = false }
        WidgetUpdater.updateAll(context)
    }
}

/** First tap arms the delete (the ✕ turns into "למחוק?"), second tap on the same verse deletes the bookmark. */
class DeleteVerseBookmark : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val chapter = parameters[ReaderKeys.ChapterKey] ?: return
        val verse = parameters[ReaderKeys.VerseKey] ?: return
        val key = chapter * 1000 + verse
        var delete = false
        updateAppWidgetState(context, glanceId) { prefs ->
            if (prefs[ReaderKeys.PENDING_BM] == key) {
                delete = true
                prefs.remove(ReaderKeys.PENDING_BM)
            } else {
                prefs[ReaderKeys.PENDING_BM] = key
            }
        }
        if (delete) {
            val c = context.container
            c.bookmarks.bookmarks.first().firstOrNull { it.chapter == chapter && it.verse == verse }?.let { c.bookmarks.delete(it.id) }
            WidgetUpdater.updateAll(context)
        } else {
            ReaderWidget().update(context, glanceId)
        }
    }
}

/** Arrow glyphs are not bidi-mirrored triangles, so they point where they say; "previous" is the first child (right edge in RTL). */
private fun rtlArrow(context: Context, previous: Boolean): String {
    val rtl = context.resources.configuration.layoutDirection == android.view.View.LAYOUT_DIRECTION_RTL
    val pointsRight = previous == rtl
    return if (pointsRight) "▶\uFE0E" else "◀\uFE0E"
}

class ReaderWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ReaderWidget()
}

/** Pickable widget entries that pre-select the kind of source: one specific favorite, a refuah name or an ilui nishmat name. */
class ReaderFavoriteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ReaderWidget()
}

class ReaderRefuahWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ReaderWidget()
}

class ReaderIluiWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ReaderWidget()
}
