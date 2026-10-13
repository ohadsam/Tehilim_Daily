package com.ohadsam.tehilim.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

internal fun ResolvedDisplay.fontFamily(): FontFamily = when (family) {
    "serif" -> FontFamily.Serif
    "mono" -> FontFamily.Monospace
    else -> FontFamily.SansSerif
}

internal fun ResolvedDisplay.fontWeight(): FontWeight = if (bold) FontWeight.Bold else FontWeight.Normal

internal fun ResolvedDisplay.textAlign(): TextAlign = if (centered) TextAlign.Center else TextAlign.Start

@androidx.compose.runtime.Composable
internal fun ResolvedDisplay.backgroundColor(): ColorProvider =
    background?.let { ColorProvider(Color(it).copy(alpha = bgAlpha)) } ?: GlanceTheme.colors.surface

@androidx.compose.runtime.Composable
internal fun ResolvedDisplay.textColorProvider(): ColorProvider = textColor?.let { ColorProvider(Color(it)) } ?: GlanceTheme.colors.onSurface

/** Intent that opens [activity] to configure appwidget [appWidgetId] (used by the ⚙ button of every configurable widget). */
internal fun configureAction(context: Context, appWidgetId: Int, activity: Class<*>): Action = actionStartActivity(
    Intent(context, activity)
        .setAction(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
        .setData(Uri.parse("tehilim://widget-config/$appWidgetId"))
        .putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
)

/** A small tappable pill (counts, ⚙, ➕ …). */
@androidx.compose.runtime.Composable
internal fun Pill(text: String, action: Action, modifier: GlanceModifier = GlanceModifier, highlighted: Boolean = false) {
    Text(
        text,
        maxLines = 1,
        style = TextStyle(
            color = if (highlighted) GlanceTheme.colors.onPrimary else GlanceTheme.colors.onSecondaryContainer,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        ),
        modifier = modifier
            .background(if (highlighted) GlanceTheme.colors.primary else GlanceTheme.colors.secondaryContainer)
            .cornerRadius(14.dp)
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .clickable(action),
    )
}

/** Small indicators for the reminders that are switched on and the saved bookmarks; tapping opens that screen of the app. */
@androidx.compose.runtime.Composable
internal fun HeaderBadges(context: Context, reminders: Int, bookmarks: Int, alwaysShowBookmarks: Boolean = false) {
    if (reminders > 0) {
        Pill("⏰ $reminders", openApp(context, "reminders"), GlanceModifier.padding(start = 4.dp))
    }
    if (bookmarks > 0 || alwaysShowBookmarks) {
        Pill("🔖 $bookmarks", openApp(context, "bookmarks"), GlanceModifier.padding(start = 4.dp))
    }
}

/** The "said" toggle: a compact rounded pill that fills in once marked (replaces the full-width system button). */
@androidx.compose.runtime.Composable
internal fun SaidPill(said: Boolean, action: Action) {
    Row(GlanceModifier.fillMaxWidth().padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Pill(if (said) "✓ נאמר" else "○ סימון: נאמר", action, highlighted = said)
    }
}
