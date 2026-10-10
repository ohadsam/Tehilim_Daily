package com.ohadsam.tehilim.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.data.PsalmsRepository
import com.ohadsam.tehilim.data.prefs.ReaderSettings
import com.ohadsam.tehilim.domain.HebrewNumerals
import com.ohadsam.tehilim.domain.PsalmErrorKind
import com.ohadsam.tehilim.domain.PsalmRef
import com.ohadsam.tehilim.domain.PsalmText
import com.ohadsam.tehilim.domain.title
import com.ohadsam.tehilim.ui.theme.ReaderTextStyle

/** One chapter (or chapter slice): heading, verses, and loading / error-with-retry states. */
@Composable
fun ChapterPage(
    repo: PsalmsRepository,
    ref: PsalmRef,
    settings: ReaderSettings,
    modifier: Modifier = Modifier,
) {
    var retry by remember(ref) { mutableIntStateOf(0) }
    val state by rememberPsalm(repo, ref, retry)
    Box(modifier.fillMaxSize()) {
        when (val s = state) {
            LoadState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            is LoadState.Error -> ErrorBox(s.kind, onRetry = { retry++ }, Modifier.align(Alignment.Center))
            is LoadState.Success -> ChapterText(ref, s.text, settings)
        }
    }
}

@Composable
private fun ChapterText(ref: PsalmRef, text: PsalmText, settings: ReaderSettings) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item(key = "title") {
            Text(
                ref.title(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                textAlign = TextAlign.Center,
            )
        }
        items(text.verses, key = { it.number }) { v ->
            Row(verticalAlignment = Alignment.Top) {
                if (settings.showVerseNumbers) {
                    Text(
                        HebrewNumerals.toHebrew(v.number, punctuation = false),
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = (settings.fontSizeSp * 0.6f).sp,
                        modifier = Modifier.width((settings.fontSizeSp * 1.7f).dp).padding(top = 4.dp),
                    )
                }
                Text(
                    v.text,
                    style = ReaderTextStyle.copy(fontSize = settings.fontSizeSp.sp, lineHeight = (settings.fontSizeSp * 1.65f).sp),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item(key = "source") {
            Text(
                text.versionTitle?.let { stringResource(R.string.reader_source, it) } ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ErrorBox(kind: PsalmErrorKind, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(
                when (kind) {
                    PsalmErrorKind.OFFLINE -> R.string.error_offline
                    PsalmErrorKind.SERVER -> R.string.error_server
                    PsalmErrorKind.NOT_FOUND -> R.string.error_not_found
                    PsalmErrorKind.INVALID_DATA -> R.string.error_invalid
                },
            ),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
    }
}
