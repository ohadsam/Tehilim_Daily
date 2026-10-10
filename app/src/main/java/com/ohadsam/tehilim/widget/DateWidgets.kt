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
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.ohadsam.tehilim.calendar.HebrewCalendar
import com.ohadsam.tehilim.domain.HebrewNumerals
import com.ohadsam.tehilim.domain.schedule.PsalmSchedules
import com.ohadsam.tehilim.domain.schedule.hebrewName
import com.ohadsam.tehilim.favorites.summarizeChapters
import java.time.LocalDate

/** Small widget: the Hebrew date. Tap opens the month view. */
class HebrewDateWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = LocalDate.now()
        val h = HebrewCalendar.fromLocalDate(today)
        val chapters = summarizeChapters(PsalmSchedules.monthly(h.day.coerceAtMost(h.daysInMonth), h.daysInMonth))
        provideContent {
            GlanceTheme(colors = WidgetColors) {
                Column(
                    GlanceModifier.fillMaxSize().cornerRadius(20.dp).background(GlanceTheme.colors.surface).padding(10.dp)
                        .clickable(openApp(context, "today?mode=month")),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(HebrewNumerals.toHebrew(h.day), style = TextStyle(color = GlanceTheme.colors.secondary, fontSize = 30.sp, fontWeight = FontWeight.Bold))
                    Text("${h.monthName} ${HebrewCalendar.yearLabel(h.year)}", style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, textAlign = TextAlign.Center))
                    Text(chapters, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp, textAlign = TextAlign.Center), maxLines = 2)
                }
            }
        }
    }
}

class HebrewDateWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HebrewDateWidget()
}

/** Small widget: today's weekday and its chapters. Tap opens the weekday view. */
class WeekdayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = LocalDate.now()
        val chapters = summarizeChapters(PsalmSchedules.weekly(today.dayOfWeek))
        provideContent {
            GlanceTheme(colors = WidgetColors) {
                Column(
                    GlanceModifier.fillMaxSize().cornerRadius(20.dp).background(GlanceTheme.colors.surface).padding(10.dp)
                        .clickable(openApp(context, "today?mode=week")),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(today.dayOfWeek.hebrewName, style = TextStyle(color = GlanceTheme.colors.secondary, fontSize = 20.sp, fontWeight = FontWeight.Bold))
                    Text("תהילים $chapters", style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp, textAlign = TextAlign.Center), maxLines = 3)
                }
            }
        }
    }
}

class WeekdayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeekdayWidget()
}
