package com.ohadsam.tehilim.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.glance.action.Action
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.material3.ColorProviders
import com.ohadsam.tehilim.AppContainer
import com.ohadsam.tehilim.MainActivity
import com.ohadsam.tehilim.TehilimApp
import com.ohadsam.tehilim.ui.theme.Palettes

/** Widget colours follow the app palette (light/dark), so widgets match the app and react to the system theme. */
fun widgetColors(context: Context) = run {
    val s = context.container.themePrefs.settings.value
    val p = Palettes.byKey(s.paletteKey)
    ColorProviders(light = p.light, dark = p.dark)
}

val Context.container: AppContainer get() = (applicationContext as TehilimApp).container

/** Opens the app on [route]; the unique data URI keeps PendingIntents of different widgets/rows from colliding. */
fun openApp(context: Context, route: String): Action = actionStartActivity(
    Intent(context, MainActivity::class.java)
        .setData(Uri.parse("tehilim://open/" + Uri.encode(route)))
        .putExtra(MainActivity.EXTRA_ROUTE, route)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
)
