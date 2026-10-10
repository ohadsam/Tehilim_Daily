package com.ohadsam.tehilim.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.ohadsam.tehilim.ui.favorites.FavoriteReaderScreen
import com.ohadsam.tehilim.ui.favorites.FavoritesScreen
import com.ohadsam.tehilim.ui.more.MoreScreen
import com.ohadsam.tehilim.ui.today.TodayScreen

@Composable
fun AppRoot(container: AppContainer) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    Scaffold(
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { d ->
                    val label = stringResource(d.label)
                    NavigationBarItem(
                        selected = current == d.route || (d == Destination.FAVORITES && current.isFavoritesDetail()),
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
            composable(Destination.TODAY.route) { TodayScreen(container) }
            composable(Destination.BROWSE.route) { BrowseScreen(container) }
            composable(Destination.FAVORITES.route) {
                FavoritesScreen(
                    container,
                    onOpenFavorite = { nav.navigate("favorite/$it") },
                    onOpenPerson = { nav.navigate("person/$it") },
                )
            }
            composable("favorite/{id}", listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                FavoriteReaderScreen(container, entry.arguments?.getLong("id"), null, onBack = { nav.popBackStack() })
            }
            composable("person/{id}", listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                FavoriteReaderScreen(container, null, entry.arguments?.getLong("id"), onBack = { nav.popBackStack() })
            }
            composable(Destination.REMINDERS.route) { PlaceholderScreen(stringResource(Destination.REMINDERS.label)) }
            composable(Destination.MORE.route) { MoreScreen(container) }
        }
    }
}

private fun String?.isFavoritesDetail(): Boolean = this != null && (startsWith("favorite/") || startsWith("person/"))
