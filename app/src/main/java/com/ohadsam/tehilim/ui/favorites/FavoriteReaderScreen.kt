package com.ohadsam.tehilim.ui.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ohadsam.tehilim.AppContainer
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.domain.PsalmRef
import com.ohadsam.tehilim.favorites.FavoritesState
import com.ohadsam.tehilim.favorites.Person
import com.ohadsam.tehilim.said.SaidContext
import com.ohadsam.tehilim.ui.bookmarks.BookmarkHost
import com.ohadsam.tehilim.ui.reader.DayReader
import com.ohadsam.tehilim.ui.reader.SaidBar
import kotlinx.coroutines.launch

/** Reads one favorite (its people are shown highlighted above the text) or all chapters linked to one person. */
@Composable
fun FavoriteReaderScreen(container: AppContainer, favoriteId: Long?, personId: Long?, onBack: () -> Unit) {
    val state by container.favorites.state.collectAsState(initial = FavoritesState(emptyList(), emptyList()))
    val settings by container.readerPrefs.settings.collectAsState()
    val saidEntries by container.said.entries.collectAsState(initial = emptyList())
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val favorite = favoriteId?.let { id -> state.favorites.firstOrNull { it.id == id } }
    val person = personId?.let { state.person(it) }
    val title: String
    val people: List<Person>
    val chapters: List<PsalmRef>
    when {
        favorite != null -> {
            title = favorite.name
            people = state.personsOf(favorite)
            chapters = favorite.chapters
        }
        person != null -> {
            title = person.name
            people = listOf(person)
            chapters = state.favoritesOf(person).flatMap { it.chapters }.distinct()
        }
        else -> {
            title = ""
            people = emptyList()
            chapters = emptyList()
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) }
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            people.forEach { PersonTag(it) }
        }
        PrayerBlocks(container, people.firstOrNull()?.kind)
        if (chapters.isEmpty()) {
            Text(
                stringResource(R.string.fav_reader_empty),
                modifier = Modifier.padding(24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            androidx.compose.runtime.key(favoriteId, personId, chapters) {
                BookmarkHost(container, Modifier.weight(1f)) { hooks ->
                DayReader(
                    bookmarks = hooks,
                    repo = container.psalms,
                    chapters = chapters,
                    settings = settings,
                    startAtEnd = false,
                    onPreviousDay = null,
                    onNextDay = null,
                    footer = {
                        val today = container.locations.logicalToday()
                        val ctx = if (favoriteId != null) SaidContext.favorite(favoriteId) else SaidContext.person(personId ?: 0L)
                        SaidBar(
                            said = saidEntries.any { it.date == today && it.context == ctx },
                            onToggle = { scope.launch { container.said.toggle(today, ctx) } },
                        )
                    },
                )
                }
            }
        }
    }
}

/** Collapsible "before" / "after" prayers; shown only when enabled in Settings and a *verified* text exists. */
@Composable
private fun PrayerBlocks(container: AppContainer, kind: com.ohadsam.tehilim.favorites.PersonKind?) {
    val prayer by container.prayerPrefs.settings.collectAsState()
    val audience = when (kind) {
        com.ohadsam.tehilim.favorites.PersonKind.REFUAH -> "refuah"
        com.ohadsam.tehilim.favorites.PersonKind.ILUI_NISHMAT -> "ilui"
        null -> "general"
    }
    listOf("before" to prayer.showBefore, "after" to prayer.showAfter).forEach { (k, enabled) ->
        val text = if (enabled) com.ohadsam.tehilim.prayers.PrayerCatalog.pick(container.prayers, k, audience, prayer.nusach) else null
        if (text != null) {
            var open by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            androidx.compose.material3.Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        text.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.fillMaxWidth().clickable { open = !open },
                    )
                    if (open) Text(text.text, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}
