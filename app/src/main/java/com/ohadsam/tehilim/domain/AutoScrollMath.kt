package com.ohadsam.tehilim.domain

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Pure maths of the reader's auto-scroll (same scale as the kodesh-app: level 1 = 8 px/s … level 15 = 120 px/s). */
object AutoScrollMath {
    const val MIN_LEVEL = 1
    const val MAX_LEVEL = 15
    const val DEFAULT_LEVEL = 4
    private const val PX_PER_LEVEL = 8f

    fun clampLevel(level: Int): Int = min(MAX_LEVEL, max(MIN_LEVEL, level))

    fun pxPerSec(level: Int): Float = clampLevel(level) * PX_PER_LEVEL

    /** 7 → "7 שנ׳", 125 → "2 דק׳ 05 שנ׳", 3725 → "1 ש׳ 02 דק׳" (seconds dropped once an hour is involved). */
    fun formatTime(seconds: Float): String {
        val sec = max(0, ceil(seconds).toInt())
        val h = sec / 3600
        val m = sec % 3600 / 60
        val s = sec % 60
        return when {
            h > 0 -> "$h ש׳ ${m.toString().padStart(2, '0')} דק׳"
            m > 0 -> "$m דק׳ ${s.toString().padStart(2, '0')} שנ׳"
            else -> "$s שנ׳"
        }
    }

    data class Progress(val percent: Int, val remainingSeconds: Float, val atEnd: Boolean)

    /** Always computed from the real position, never accumulated, so manual scrolls and speed changes are just new inputs. */
    fun progress(scrolledPx: Float, totalScrollablePx: Float, level: Int, holdMs: Long = 0): Progress {
        if (totalScrollablePx <= 0f) return Progress(100, 0f, true)
        val left = max(0f, totalScrollablePx - scrolledPx)
        val atEnd = left < 1f
        val pct = if (atEnd) 100 else min(99, (scrolledPx / totalScrollablePx * 100f).roundToInt().coerceAtLeast(0))
        return Progress(pct, if (atEnd) 0f else left / pxPerSec(level) + max(0L, holdMs) / 1000f, atEnd)
    }
}
