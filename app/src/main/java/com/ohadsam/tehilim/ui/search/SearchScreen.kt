package com.ohadsam.tehilim.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ohadsam.tehilim.AppContainer
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.domain.HebrewNumerals
import com.ohadsam.tehilim.domain.PSALMS_COUNT
import com.ohadsam.tehilim.domain.PsalmChapter
import com.ohadsam.tehilim.search.PsalmSearch
import com.ohadsam.tehilim.search.SearchHit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Full-text search over the locally stored chapters; offers to download the rest when the library is incomplete. */
@Composable
fun SearchScreen(container: AppContainer, onBack: () -> Unit, onOpen: (chapter: Int, verse: Int) -> Unit) {
    var query by remember { mutableStateOf("") }
    var library by remember { mutableStateOf<List<PsalmChapter>>(emptyList()) }
    var hits by remember { mutableStateOf<List<SearchHit>>(emptyList()) }
    var downloaded by remember { mutableIntStateOf(0) }
    var downloading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        library = container.psalms.allCached()
        downloaded = library.size
    }
    LaunchedEffect(Unit) { reload() }
    LaunchedEffect(query, library) {
        delay(250) // debounce typing
        hits = withContext(Dispatchers.Default) { PsalmSearch.search(library, query) }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) }
            Text(stringResource(R.string.search_title), style = MaterialTheme.typography.titleLarge)
        }
        OutlinedTextField(
            value = query, onValueChange = { query = it }, singleLine = true,
            label = { Text(stringResource(R.string.search_hint)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        if (downloaded < PSALMS_COUNT) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.search_incomplete, downloaded, PSALMS_COUNT), color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (downloading) {
                    LinearProgressIndicator(progress = { downloaded / PSALMS_COUNT.toFloat() }, modifier = Modifier.fillMaxWidth())
                } else {
                    Button(onClick = {
                        downloading = true
                        scope.launch {
                            container.psalms.prefetchAll { downloaded = it.done }
                            reload()
                            downloading = false
                        }
                    }) { Text(stringResource(R.string.search_download_all)) }
                }
            }
        }
        if (query.isNotBlank() && hits.isEmpty() && library.isNotEmpty()) {
            Text(stringResource(R.string.search_no_results), modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (hits.isNotEmpty()) {
                item { Text(stringResource(R.string.search_count, hits.size), style = MaterialTheme.typography.labelMedium) }
            }
            items(hits, key = { it.chapter * 1000 + it.verse }) { h ->
                Card(Modifier.fillMaxWidth().clickable { onOpen(h.chapter, h.verse) }) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "${HebrewNumerals.toHebrew(h.chapter)} : ${HebrewNumerals.toHebrew(h.verse)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Text(highlight(h, MaterialTheme.colorScheme.secondary), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

private fun highlight(h: SearchHit, color: androidx.compose.ui.graphics.Color): AnnotatedString = buildAnnotatedString {
    var pos = 0
    for (r in h.matches) {
        if (r.first > pos) append(h.text.substring(pos, r.first))
        withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold)) { append(h.text.substring(r.first, r.last + 1)) }
        pos = r.last + 1
    }
    if (pos < h.text.length) append(h.text.substring(pos))
}
