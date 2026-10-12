package com.ohadsam.tehilim.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.domain.AutoScrollMath

/**
 * Auto-scroll controls: a small ▶ button while idle (only when the text overflows the screen), and while a session is
 * active a strip with pause/resume, stop, slower/faster, progress and time left.
 */
@Composable
fun AutoScrollToolbar(c: AutoScrollController, globalKeepScreenOn: Boolean, modifier: Modifier = Modifier) {
    val view = LocalView.current
    val playing = c.phase == ScrollPhase.PLAYING
    val keepAwake = c.current.keepAwake
    DisposableEffect(playing, keepAwake) {
        if (playing && keepAwake) view.keepScreenOn = true
        onDispose { if (playing && keepAwake) view.keepScreenOn = globalKeepScreenOn }
    }
    val list = c.list
    if (!c.active && c.phase != ScrollPhase.ENDED) {
        val overflow = list != null && (list.canScrollForward || list.canScrollBackward)
        if (overflow) {
            androidx.compose.material3.SmallFloatingActionButton(
                onClick = { c.start() },
                modifier = modifier.padding(start = 12.dp, bottom = 8.dp),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ) { Icon(Icons.Filled.PlayArrow, stringResource(R.string.as_start)) }
        }
        return
    }

    val progress by remember(list, c.level, c.holdUntilMs) {
        derivedStateOf {
            val hold = (c.holdUntilMs - System.currentTimeMillis()).coerceAtLeast(0L)
            if (list == null) AutoScrollMath.Progress(0, 0f, false) else progressOf(list, c.level, hold).let { it.copy(remainingSeconds = kotlin.math.ceil(it.remainingSeconds / 2f) * 2f) }
        }
    }
    val ended = c.phase == ScrollPhase.ENDED
    Card(modifier.fillMaxWidth().padding(horizontal = 12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = { c.toggle() }) {
                    Icon(
                        when {
                            ended -> Icons.Filled.Replay
                            playing -> Icons.Filled.Pause
                            else -> Icons.Filled.PlayArrow
                        },
                        contentDescription = stringResource(if (playing) R.string.as_pause else R.string.as_play),
                    )
                }
                IconButton(onClick = { c.stop() }) { Icon(Icons.Filled.Stop, stringResource(R.string.as_stop)) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { c.slower() }, enabled = c.level > AutoScrollMath.MIN_LEVEL) {
                        Icon(Icons.Filled.Remove, stringResource(R.string.as_slower))
                    }
                    Text(stringResource(R.string.as_speed, c.level), style = MaterialTheme.typography.labelLarge)
                    IconButton(onClick = { c.faster() }, enabled = c.level < AutoScrollMath.MAX_LEVEL) {
                        Icon(Icons.Filled.Add, stringResource(R.string.as_faster))
                    }
                }
            }
            LinearProgressIndicator(progress = { progress.percent / 100f }, modifier = Modifier.fillMaxWidth())
            Text(
                when {
                    ended || progress.atEnd -> stringResource(R.string.as_end)
                    c.phase == ScrollPhase.PAUSED -> stringResource(R.string.as_paused_left, progress.percent, AutoScrollMath.formatTime(progress.remainingSeconds))
                    else -> stringResource(R.string.as_left, progress.percent, AutoScrollMath.formatTime(progress.remainingSeconds))
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
            )
        }
    }
}
