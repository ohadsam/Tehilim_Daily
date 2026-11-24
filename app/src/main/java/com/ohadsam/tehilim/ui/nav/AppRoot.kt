package com.ohadsam.tehilim.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ohadsam.tehilim.AppContainer
import com.ohadsam.tehilim.ui.common.PlaceholderScreen
import com.ohadsam.tehilim.ui.browse.BrowseScreen
import com.ohadsam.tehilim.ui.bookmarks.BookmarksScreen
import com.ohadsam.tehilim.ui.favorites.FavoriteReaderScreen
import com.ohadsam.tehilim.ui.favorites.FavoritesScreen
import com.ohadsam.tehilim.ui.favorites.NameLettersScreen
import com.ohadsam.tehilim.ui.history.HistoryScreen
import com.ohadsam.tehilim.ui.search.SearchScreen
import com.ohadsam.tehilim.ui.location.LocationScreen
import com.ohadsam.tehilim.ui.logs.LogsScreen
import com.ohadsam.tehilim.ui.permissions.PermissionsScreen
import com.ohadsam.tehilim.ui.settings.SettingsScreen
import com.ohadsam.tehilim.ui.more.MoreScreen
import com.ohadsam.tehilim.ui.reminders.RemindersScreen
import com.ohadsam.tehilim.ui.today.TodayScreen

@Composable
fun AppRoot(container: AppContainer, startRoute: String? = null, onRouteHandled: () -> Unit = {}) {
    val nav = rememberNavController()
    LaunchedEffect(startRoute) {
        if (startRoute != null) {
            runCatching { nav.navigate(startRoute) }
            onRouteHandled()
        }
    }
    val incomingShare by container.shareInbox.collectAsState()
    LaunchedEffect(incomingShare) {
        if (incomingShare != null) runCatching { nav.navigate("share") { launchSingleTop = true } }
    }
    com.ohadsam.tehilim.ui.update.UpdatePrompt(container, onOpen = { nav.navigate("update") { launchSingleTop = true } })
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    Scaffold(
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { d ->
                    val label = stringResource(d.label)
                    NavigationBarItem(
                        selected = current?.substringBefore('?') == d.route || (d == Destination.FAVORITES && current.isFavoritesDetail()),
                        onClick = {
                            nav.navigate(d.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(d.icon, contentDescription = label) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Destination.TODAY.route, modifier = Modifier.padding(padding)) {
            composable(
                "today?mode={mode}",
                listOf(navArgument("mode") { type = NavType.StringType; defaultValue = "month" }),
            ) { entry ->
                TodayScreen(
                    container,
                    initialWeek = entry.arguments?.getString("mode") == "week",
                    onContinue = { nav.navigate("browse?chapter=$it&verse=0") },
                    onOpenChapter = { chapter, verse -> nav.navigate("browse?chapter=$chapter&verse=$verse") },
                    onOpenBookmarks = { nav.navigate("bookmarks") },
                    onOpenSearch = { nav.navigate("search") },
                    onOpenReminders = { nav.navigate(Destination.REMINDERS.route) },
                    onAddWidget = { nav.navigate("addwidget") },
                )
            }
            composable(
                "browse?chapter={chapter}&verse={verse}",
                listOf(
                    navArgument("chapter") { type = NavType.IntType; defaultValue = 1 },
                    navArgument("verse") { type = NavType.IntType; defaultValue = 0 },
                ),
            ) { entry ->
                BrowseScreen(
                    container,
                    initialChapter = entry.arguments?.getInt("chapter") ?: 1,
                    initialVerse = entry.arguments?.getInt("verse") ?: 0,
                    onOpenSearch = { nav.navigate("search") },
                    onOpenBookmarks = { nav.navigate("bookmarks") },
                )
            }
            composable(
                "favorites?add={add}",
                listOf(navArgument("add") { type = NavType.StringType; defaultValue = "" }),
            ) { entry ->
                FavoritesScreen(
                    container,
                    autoAdd = entry.arguments?.getString("add").orEmpty(),
                    onOpenFavorite = { nav.navigate("favorite/$it") },
                    onOpenPerson = { nav.navigate("person/$it") },
                    onOpenNameLetters = { nav.navigate("nameletters?person=${it ?: -1L}") },
                )
            }
            composable(
                "nameletters?person={person}",
                listOf(navArgument("person") { type = NavType.LongType; defaultValue = -1L }),
            ) { entry ->
                val pid = entry.arguments?.getLong("person")?.takeIf { it > 0 }
                NameLettersScreen(
                    container, pid, onBack = { nav.popBackStack() },
                    onSaved = { fid -> nav.popBackStack(); nav.navigate("favorite/$fid") },
                )
            }
            composable("favorite/{id}", listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                FavoriteReaderScreen(container, entry.arguments?.getLong("id"), null, onBack = { nav.popBackStack() })
            }
            composable("person/{id}", listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                FavoriteReaderScreen(container, null, entry.arguments?.getLong("id"), onBack = { nav.popBackStack() })
            }
            composable(Destination.REMINDERS.route) {
                RemindersScreen(container, onOpenRoute = { nav.navigate(it) }, onOpenLocation = { nav.navigate("location") })
            }
            composable("location") { LocationScreen(container, onBack = { nav.popBackStack() }) }
            composable(Destination.MORE.route) {
                MoreScreen(
                    container,
                    onOpenHistory = { nav.navigate("history") },
                    onOpenSettings = { nav.navigate("settings") },
                    onOpenLogs = { nav.navigate("logs") },
                    onOpenBookmarks = { nav.navigate("bookmarks") },
                    onAddWidget = { nav.navigate("addwidget") },
                    onOpenShare = { nav.navigate("share") },
                    onOpenUpdate = { nav.navigate("update") },
                )
            }
            composable("settings") {
                SettingsScreen(
                    container,
                    onBack = { nav.popBackStack() },
                    onOpenLocation = { nav.navigate("location") },
                    onOpenLogs = { nav.navigate("logs") },
                    onOpenBookmarks = { nav.navigate("bookmarks") },
                    onOpenReminders = { nav.navigate(Destination.REMINDERS.route) },
                    onOpenHistory = { nav.navigate("history") },
                    onShowWhatsNew = { nav.navigate("more") },
                    onOpenPermissions = { nav.navigate("permissions") },
                )
            }
            composable("update") { com.ohadsam.tehilim.ui.update.UpdateScreen(container, onBack = { nav.popBackStack() }) }
            composable("share") {
                com.ohadsam.tehilim.ui.share.ShareHubScreen(
                    container,
                    onBack = { nav.popBackStack() },
                    onOpenFavorite = { nav.navigate("favorite/$it") },
                    onOpenFavorites = { nav.navigate(Destination.FAVORITES.route) },
                )
            }
            composable("addwidget") { com.ohadsam.tehilim.ui.widgets.AddWidgetScreen(onBack = { nav.popBackStack() }) }
            composable("permissions") { PermissionsScreen(onBack = { nav.popBackStack() }) }
            composable("logs") { LogsScreen(container, onBack = { nav.popBackStack() }) }
            composable("bookmarks") {
                BookmarksScreen(container, onBack = { nav.popBackStack() }, onOpen = { nav.navigate("browse?chapter=${it.chapter}&verse=${it.verse}") })
            }
            composable("history") { HistoryScreen(container, onBack = { nav.popBackStack() }) }
            composable("search") {
                SearchScreen(container, onBack = { nav.popBackStack() }, onOpen = { ch, v -> nav.navigate("browse?chapter=$ch&verse=$v") })
            }
        }
    }
}

private fun String?.isFavoritesDetail(): Boolean = this != null && (startsWith("favorite/") || startsWith("person/"))
