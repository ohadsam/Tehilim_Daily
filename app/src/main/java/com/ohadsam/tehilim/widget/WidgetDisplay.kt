package com.ohadsam.tehilim.widget

import com.ohadsam.tehilim.data.prefs.ReaderSettings

/**
 * Per-widget display options. While [followApp] is true the widget mirrors the app's reading settings (size, nikud,
 * ta'amim, verse numbers); colours are independent (0 = theme default).
 */
data class WidgetDisplayPrefs(
    val followApp: Boolean = true,
    val fontSp: Float = 16f,
    val nikud: Boolean = true,
    val taamim: Boolean = false,
    val numbers: Boolean = true,
    val background: Int = 0,
    val textColor: Int = 0,
)

data class ResolvedDisplay(
    val fontSp: Float,
    val nikud: Boolean,
    val taamim: Boolean,
    val numbers: Boolean,
    val background: Int?,
    val textColor: Int?,
)

object WidgetDisplay {
    const val MIN_SP = 11f
    const val MAX_SP = 30f
    private const val APP_TO_WIDGET = 0.75f // widget text is a bit smaller than the in-app reading size

    fun resolve(w: WidgetDisplayPrefs, app: ReaderSettings): ResolvedDisplay =
        if (w.followApp) {
            ResolvedDisplay(
                (app.fontSizeSp * APP_TO_WIDGET).coerceIn(MIN_SP, MAX_SP), app.showNikud, app.showTaamim, app.showVerseNumbers,
                w.background.takeIf { it != 0 }, w.textColor.takeIf { it != 0 },
            )
        } else {
            ResolvedDisplay(
                w.fontSp.coerceIn(MIN_SP, MAX_SP), w.nikud, w.taamim, w.numbers, w.background.takeIf { it != 0 }, w.textColor.takeIf { it != 0 },
            )
        }
}
