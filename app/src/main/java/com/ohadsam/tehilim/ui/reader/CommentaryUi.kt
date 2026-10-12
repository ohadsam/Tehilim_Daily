package com.ohadsam.tehilim.ui.reader

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.data.CommentaryRepository
import com.ohadsam.tehilim.data.sefaria.CommentaryParser
import com.ohadsam.tehilim.domain.CommentarySource
import com.ohadsam.tehilim.domain.PsalmErrorKind
import com.ohadsam.tehilim.domain.PsalmException

sealed interface CommentaryState {
    data object Off : CommentaryState

    data object Loading : CommentaryState

    /** verse number → comments; empty when this commentator has nothing on the chapter. */
    data class Loaded(val byVerse: Map<Int, List<String>>) : CommentaryState

    data class Error(val kind: PsalmErrorKind) : CommentaryState
}

/** Loads the commentary of [chapter] through the local-first repository, only while [enabled]. */
@Composable
fun rememberCommentary(repo: CommentaryRepository?, chapter: Int, source: CommentarySource, enabled: Boolean, retryKey: Int): State<CommentaryState> =
    produceState<CommentaryState>(CommentaryState.Off, repo, chapter, source, enabled, retryKey) {
        if (!enabled || repo == null) {
            value = CommentaryState.Off
            return@produceState
        }
        value = CommentaryState.Loading
        value = repo.get(chapter, source).fold(
            onSuccess = { CommentaryState.Loaded(it) },
            onFailure = { CommentaryState.Error((it as? PsalmException)?.kind ?: PsalmErrorKind.INVALID_DATA) },
        )
    }

/** Builds the displayed comment: the opening words Sefaria marked bold stay bold; [shown] applies the nikud/ta'amim settings. */
fun commentaryAnnotated(raw: String, shown: (String) -> String = { it }): AnnotatedString = buildAnnotatedString {
    val segment = StringBuilder()
    var bold = false

    fun flush() {
        if (segment.isEmpty()) return
        val text = shown(segment.toString())
        if (bold) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text) } else append(text)
        segment.clear()
    }
    for (ch in raw) {
        when (ch) {
            CommentaryParser.BOLD_START -> { flush(); bold = true }
            CommentaryParser.BOLD_END -> { flush(); bold = false }
            else -> segment.append(ch)
        }
    }
    flush()
}

/** The reader's show/hide commentary button. */
@Composable
fun CommentaryToggle(on: Boolean, onToggle: () -> Unit) {
    FilterChip(selected = on, onClick = onToggle, label = { Text(stringResource(R.string.tb_commentary)) })
}

/** One line under the chapter title telling what the commentary is doing: loading, which commentator, none, or unavailable (retry). */
@Composable
fun CommentaryStatus(state: CommentaryState, source: CommentarySource, onRetry: () -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    when (state) {
        CommentaryState.Off -> Unit
        CommentaryState.Loading -> Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
            Text(stringResource(R.string.cm_loading), style = MaterialTheme.typography.labelMedium, color = muted, modifier = Modifier.padding(start = 8.dp))
        }
        is CommentaryState.Error -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.cm_unavailable), style = MaterialTheme.typography.labelMedium, color = muted, modifier = Modifier.weight(1f))
            TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
        }
        is CommentaryState.Loaded -> Text(
            if (state.byVerse.isEmpty()) stringResource(R.string.cm_none, source.hebrewName) else stringResource(R.string.cm_source, source.hebrewName),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        )
    }
}
