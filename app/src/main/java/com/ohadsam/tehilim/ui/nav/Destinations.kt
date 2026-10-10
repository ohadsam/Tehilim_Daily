package com.ohadsam.tehilim.ui.nav

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Today
import androidx.compose.ui.graphics.vector.ImageVector
import com.ohadsam.tehilim.R

enum class Destination(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    TODAY("today", R.string.tab_today, Icons.Filled.Today),
    BROWSE("browse", R.string.tab_browse, Icons.AutoMirrored.Filled.MenuBook),
    FAVORITES("favorites", R.string.tab_favorites, Icons.Filled.Favorite),
    REMINDERS("reminders", R.string.tab_reminders, Icons.Filled.Notifications),
    MORE("more", R.string.tab_more, Icons.Filled.MoreHoriz),
}
