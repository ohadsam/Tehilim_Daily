package com.ohadsam.tehilim.ui.favorites

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ohadsam.tehilim.AppContainer
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.favorites.Favorite
import com.ohadsam.tehilim.favorites.FavoritesState
import com.ohadsam.tehilim.favorites.Person
import com.ohadsam.tehilim.favorites.PersonKind
import com.ohadsam.tehilim.favorites.summarizeChapters
import kotlinx.coroutines.launch

private enum class FavTab(val label: Int) {
    ALL(R.string.fav_tab_all),
    REFUAH(R.string.fav_tab_refuah),
    ILUI(R.string.fav_tab_ilui),
}

private sealed interface Editor {
    data class OfFavorite(val favorite: Favorite?) : Editor

    data class OfPerson(val person: Person?, val kind: PersonKind) : Editor
}

@Composable
fun FavoritesScreen(container: AppContainer, onOpenFavorite: (Long) -> Unit, onOpenPerson: (Long) -> Unit) {
    val state by container.favorites.state.collectAsState(initial = FavoritesState(emptyList(), emptyList()))
    var tab by rememberSaveable { mutableStateOf(FavTab.ALL) }
    var editor by remember { mutableStateOf<Editor?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val undoLabel = stringResource(R.string.action_undo)
    val deletedMsg = stringResource(R.string.fav_deleted)

    fun deleteFavoriteWithUndo(f: Favorite) {
        scope.launch {
            container.favorites.deleteFavorite(f.id)
            if (snackbar.showSnackbar(deletedMsg, undoLabel, duration = androidx.compose.material3.SnackbarDuration.Long) ==
                SnackbarResult.ActionPerformed
            ) {
                container.favorites.saveFavorite(f.copy(id = 0))
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = tab.ordinal) {
                FavTab.entries.forEach { t ->
                    Tab(selected = tab == t, onClick = { tab = t }, text = { Text(stringResource(t.label)) })
                }
            }
            val persons = when (tab) {
                FavTab.ALL -> emptyList()
                FavTab.REFUAH -> state.persons.filter { it.kind == PersonKind.REFUAH }
                FavTab.ILUI -> state.persons.filter { it.kind == PersonKind.ILUI_NISHMAT }
            }
            val empty = if (tab == FavTab.ALL) state.favorites.isEmpty() else persons.isEmpty()
            if (empty) {
                EmptyState(tab)
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (tab == FavTab.ALL) {
                        items(state.favorites, key = { "f${it.id}" }) { f ->
                            FavoriteCard(
                                f, state,
                                onOpen = { onOpenFavorite(f.id) },
                                onEdit = { editor = Editor.OfFavorite(f) },
                                onDelete = { deleteFavoriteWithUndo(f) },
                            )
                        }
                    } else {
                        items(persons, key = { "p${it.id}" }) { p ->
                            PersonCard(
                                p, state,
                                onOpen = { onOpenPerson(p.id) },
                                onEdit = { editor = Editor.OfPerson(p, p.kind) },
                                onOpenFavorite = onOpenFavorite,
                                onDelete = { scope.launch { container.favorites.deletePerson(p.id) } },
                            )
                        }
                    }
                }
            }
        }
        Box(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            ExtendedFloatingActionButton(
                onClick = { menuOpen = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_add)) },
            )
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.fav_new_favorite)) }, onClick = {
                    menuOpen = false
                    editor = Editor.OfFavorite(null)
                })
                DropdownMenuItem(text = { Text(stringResource(R.string.fav_new_refuah)) }, onClick = {
                    menuOpen = false
                    editor = Editor.OfPerson(null, PersonKind.REFUAH)
                })
                DropdownMenuItem(text = { Text(stringResource(R.string.fav_new_ilui)) }, onClick = {
                    menuOpen = false
                    editor = Editor.OfPerson(null, PersonKind.ILUI_NISHMAT)
                })
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    when (val e = editor) {
        null -> Unit
        is Editor.OfFavorite -> FavoriteEditorSheet(container, state, e.favorite, onDismiss = { editor = null })
        is Editor.OfPerson -> PersonEditorSheet(container, e.person, e.kind, onDismiss = { editor = null })
    }
}

@Composable
private fun EmptyState(tab: FavTab) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(
                when (tab) {
                    FavTab.ALL -> R.string.fav_empty_all
                    FavTab.REFUAH -> R.string.fav_empty_refuah
                    FavTab.ILUI -> R.string.fav_empty_ilui
                },
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FavoriteCard(f: Favorite, state: FavoritesState, onOpen: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val persons = state.personsOf(f)
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            ColorDot(f.colorIndex)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(f.name, style = MaterialTheme.typography.titleMedium)
                Text(summarizeChapters(f.chapters), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                persons.forEach { PersonTag(it) }
            }
            CardMenu(onEdit, onDelete)
        }
    }
}

@Composable
private fun PersonCard(
    p: Person,
    state: FavoritesState,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onOpenFavorite: (Long) -> Unit,
    onDelete: () -> Unit,
) {
    var confirm by remember { mutableStateOf(false) }
    val linked = state.favoritesOf(p)
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PersonTag(p, Modifier.weight(1f))
                CardMenu(onEdit, onDelete = { confirm = true })
            }
            if (linked.isEmpty()) {
                Text(stringResource(R.string.fav_person_no_links), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            linked.forEach { f ->
                TextButton(onClick = { onOpenFavorite(f.id) }) { Text("${f.name} · ${summarizeChapters(f.chapters)}") }
            }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.fav_delete_person_title)) },
            text = { Text(stringResource(R.string.fav_delete_person_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    onDelete()
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** The person's name, visually marked (gold container) so it stands out from chapter text everywhere it is shown. */
@Composable
fun PersonTag(p: Person, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(vertical = 2.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
    ) {
        Text(
            (if (p.kind == PersonKind.REFUAH) "לרפואת " else "לעילוי נשמת ") + p.displayName(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun CardMenu(onEdit: () -> Unit, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.action_more)) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.action_edit)) }, onClick = {
                open = false
                onEdit()
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.action_delete)) }, onClick = {
                open = false
                onDelete()
            })
        }
    }
}

val FavoriteColors = listOf(
    androidx.compose.ui.graphics.Color(0xFFB8860B),
    androidx.compose.ui.graphics.Color(0xFF2E7D6B),
    androidx.compose.ui.graphics.Color(0xFF3F5BA9),
    androidx.compose.ui.graphics.Color(0xFF8E4B8E),
    androidx.compose.ui.graphics.Color(0xFFB5483A),
    androidx.compose.ui.graphics.Color(0xFF5F6B73),
)

@Composable
fun ColorDot(index: Int, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 20.dp) {
    Surface(
        modifier = modifier.padding(2.dp).size(size),
        shape = androidx.compose.foundation.shape.CircleShape,
        color = FavoriteColors[index.mod(FavoriteColors.size)],
        content = {},
    )
}
