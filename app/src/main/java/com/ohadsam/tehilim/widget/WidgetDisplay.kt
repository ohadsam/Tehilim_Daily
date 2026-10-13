package com.ohadsam.tehilim.widget

import com.ohadsam.tehilim.data.prefs.ReaderSettings

/**
 * Per-widget display options. While [followApp] is true the text options (size, nikud, ta'amim, verse numbers) mirror the
 * app's reading settings; everything else (weight, family, alignment, spacing, colours, opacity, title) is independent.
 * Colours: 0 = theme default.
 */
data class WidgetDisplayPrefs(
    val followApp: Boolean = true,
    val fontSp: Float = 16f,
    val nikud: Boolean = true,
    val taamim: Boolean = false,
    val numbers: Boolean = true,
    val background: Int = 0,
    val textColor: Int = 0,
    val bold: Boolean = false,
    /** "sans" | "serif" | "mono": Glance can only use system font families, not the app's bundled reading fonts. */
    val family: String = "sans",
    val centered: Boolean = false,
    val spacingDp: Int = 3,
    /** Only applies together with a custom [background]. */
    val bgAlpha: Float = 1f,
    val showTitle: Boolean = true,
    /** Reading widgets: first show a landing screen (icon, dates, source, chapter range); paging forward starts the reading. */
    val landing: Boolean = true,
)

data class ResolvedDisplay(
    val fontSp: Float,
    val nikud: Boolean,
    val taamim: Boolean,
    val numbers: Boolean,
    val background: Int?,
    val textColor: Int?,
    val bold: Boolean = false,
    val family: String = "sans",
    val centered: Boolean = false,
    val spacingDp: Int = 3,
    val bgAlpha: Float = 1f,
    val showTitle: Boolean = true,
)

object WidgetDisplay {
    const val MIN_SP = 11f
    const val MAX_SP = 30f
    const val MIN_ALPHA = 0.2f
    const val MAX_SPACING = 16
    private const val APP_TO_WIDGET = 0.75f // widget text is a bit smaller than the in-app reading size

    fun resolve(w: WidgetDisplayPrefs, app: ReaderSettings): ResolvedDisplay {
        val bg = w.background.takeIf { it != 0 }
        val common = Triple(w.bold, w.family, w.centered)
        return if (w.followApp) {
            ResolvedDisplay(
                (app.fontSizeSp * APP_TO_WIDGET).coerceIn(MIN_SP, MAX_SP), app.showNikud, app.showTaamim, app.showVerseNumbers,
                bg, w.textColor.takeIf { it != 0 }, common.first, common.second, common.third,
                w.spacingDp.coerceIn(0, MAX_SPACING), w.bgAlpha.coerceIn(MIN_ALPHA, 1f), w.showTitle,
            )
        } else {
            ResolvedDisplay(
                w.fontSp.coerceIn(MIN_SP, MAX_SP), w.nikud, w.taamim, w.numbers, bg, w.textColor.takeIf { it != 0 },
                common.first, common.second, common.third, w.spacingDp.coerceIn(0, MAX_SPACING), w.bgAlpha.coerceIn(MIN_ALPHA, 1f), w.showTitle,
            )
        }
    }
}
