package com.ohadsam.tehilim.ui.reader

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import com.ohadsam.tehilim.data.prefs.AutoScrollSettings
import com.ohadsam.tehilim.domain.AutoScrollMath
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs

enum class ScrollPhase { IDLE, PLAYING, PAUSED, ENDED }

/**
 * Steady auto-scroll of whichever chapter is on screen (behaviour ported from the kodesh-app): play / pause / stop,
 * speed 1..15, a short hold when a new section starts, optional hand-over to the next chapter or day, pause when the
 * user drags. Speed changes made from the toolbar are session-only; the settings screen holds the default.
 */
@Stable
class AutoScrollController(private val settings: () -> AutoScrollSettings) {
    var phase by mutableStateOf(ScrollPhase.IDLE)
        private set
    private var sessionLevel by mutableIntStateOf(0)
    var list: LazyListState? by mutableStateOf(null)
        private set
    var holdUntilMs by mutableLongStateOf(0L)
        private set

    /** Asked when the end of a section is reached; return true if the host moved on to the next one. */
    var advance: (() -> Boolean)? = null

    val level: Int get() = if (sessionLevel == 0) AutoScrollMath.clampLevel(settings().speed) else sessionLevel
    val current: AutoScrollSettings get() = settings()
    val active: Boolean get() = phase == ScrollPhase.PLAYING || phase == ScrollPhase.PAUSED

    fun attach(state: LazyListState) { list = state }

    fun detach(state: LazyListState) { if (list === state) list = null }

    fun start() { phase = ScrollPhase.PLAYING; holdUntilMs = 0 }

    fun pause() { if (phase == ScrollPhase.PLAYING) phase = ScrollPhase.PAUSED }

    fun resume() {
        if (phase == ScrollPhase.PAUSED || phase == ScrollPhase.ENDED) phase = ScrollPhase.PLAYING
    }

    fun toggle() { if (phase == ScrollPhase.PLAYING) pause() else resume() }

    fun stop() { phase = ScrollPhase.IDLE }

    fun faster() { sessionLevel = AutoScrollMath.clampLevel(level + 1) }

    fun slower() { sessionLevel = AutoScrollMath.clampLevel(level - 1) }

    fun onUserDrag() { if (phase == ScrollPhase.PLAYING && settings().pauseOnTouch) pause() }

    /** Called by the playback loop at the bottom of a section. */
    internal fun reachedEnd() {
        val delaySec = settings().sectionDelaySec
        if (settings().autoAdvance && advance?.invoke() == true) {
            // The next page attaches its own list; until then there is nothing to scroll.
            list = null
            holdUntilMs = System.currentTimeMillis() + delaySec * 1000L
        } else {
            phase = ScrollPhase.ENDED
        }
    }
}

@Composable
fun rememberAutoScrollController(prefs: com.ohadsam.tehilim.data.prefs.AutoScrollPrefs): AutoScrollController =
    remember(prefs) { AutoScrollController { prefs.settings.value } }

/** The playback loop: one frame at a time, fractional pixels accumulate because scrollBy needs whole ones. */
@Composable
fun AutoScrollPlayback(c: AutoScrollController) {
    val list = c.list
    LaunchedEffect(c.phase, list) {
        if (c.phase != ScrollPhase.PLAYING) return@LaunchedEffect
        if (list == null) {
            // Waiting for a freshly shown chapter to attach its list.
            while (isActive && c.list == null) delay(50)
            return@LaunchedEffect
        }
        var last = androidx.compose.runtime.withFrameNanos { it }
        var acc = 0f
        var dwellPx = 0f
        while (isActive) {
            val now = androidx.compose.runtime.withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.1f)
            last = now
            if (System.currentTimeMillis() < c.holdUntilMs) continue
            acc += AutoScrollMath.pxPerSec(c.level) * dt
            val whole = acc.toInt()
            if (whole < 1) continue
            acc -= whole
            if (list.canScrollForward) {
                try {
                    list.scrollBy(whole.toFloat())
                } catch (e: CancellationException) {
                    // A finger on the list pre-empts our scroll; only a real cancellation of this effect must propagate.
                    if (!currentCoroutineContext().isActive) throw e
                }
            } else {
                // A chapter that fits on the screen still needs reading time: count one screen's worth of scrolling.
                dwellPx += whole
                val screen = (list.layoutInfo.viewportEndOffset - list.layoutInfo.viewportStartOffset).coerceAtLeast(1)
                if (dwellPx >= screen) { c.reachedEnd(); return@LaunchedEffect }
            }
            if (!list.canScrollForward && dwellPx == 0f && list.layoutInfo.visibleItemsInfo.isNotEmpty()) {
                // Reached the bottom of a long chapter.
                c.reachedEnd()
                return@LaunchedEffect
            }
        }
    }
}

/** Pauses auto-scroll when the user starts a deliberate vertical drag (a tap that drifts a few pixels does not count). */
fun Modifier.pauseAutoScrollOnDrag(c: AutoScrollController?): Modifier = if (c == null) this else pointerInput(c) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var total = Offset.Zero
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            total += change.positionChange()
            if (abs(total.y) > viewConfiguration.touchSlop && abs(total.y) > abs(total.x)) {
                c.onUserDrag()
                break
            }
        }
    }
}

/** Progress through the chapter on screen, estimated from the lazy list (item heights vary, so this is an average). */
fun progressOf(list: LazyListState, level: Int, holdMs: Long): AutoScrollMath.Progress {
    val info = list.layoutInfo
    val visible = info.visibleItemsInfo
    if (visible.isEmpty() || info.totalItemsCount == 0) return AutoScrollMath.Progress(0, 0f, false)
    val avg = visible.sumOf { it.size } / visible.size.toFloat()
    val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
    val total = avg * info.totalItemsCount
    val scrolled = list.firstVisibleItemIndex * avg + list.firstVisibleItemScrollOffset
    val p = AutoScrollMath.progress(scrolled, (total - viewport).coerceAtLeast(0f), level, holdMs)
    return if (!list.canScrollForward) AutoScrollMath.Progress(100, 0f, true) else p
}
