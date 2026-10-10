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
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ohadsam.tehilim.AppContainer
import com.ohadsam.tehilim.ui.common.PlaceholderScreen
import com.ohadsam.tehilim.ui.more.MoreScreen

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
                        selected = current == d.route,
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
            composable(Destination.TODAY.route) { PlaceholderScreen(stringResource(Destination.TODAY.label)) }
            composable(Destination.BROWSE.route) { PlaceholderScreen(stringResource(Destination.BROWSE.label)) }
            composable(Destination.FAVORITES.route) { PlaceholderScreen(stringResource(Destination.FAVORITES.label)) }
            composable(Destination.REMINDERS.route) { PlaceholderScreen(stringResource(Destination.REMINDERS.label)) }
            composable(Destination.MORE.route) { MoreScreen(container) }
        }
    }
}
