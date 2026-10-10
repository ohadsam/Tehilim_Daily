package com.ohadsam.tehilim

import android.app.Application
import com.ohadsam.tehilim.log.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TehilimApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        val log = container.logger
        container.logPrefs.retentionDays.takeIf { it > 0 }?.let { log.purgeOlderThanDays(it) }
        log.i("App", "start v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        container.notifications.createChannels()
        com.ohadsam.tehilim.widget.WidgetUpdater.scheduleMidnight(this)
        watchDataForWidgets()
        appScope.launch {
            runCatching { container.said.purgeOld() }
            runCatching { container.reminderScheduler.rescheduleAll() }.onFailure { log.e("App", "reschedule failed", it) }
        }
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            log.e("Crash", "uncaught on ${t.name}", e)
            previous?.uncaughtException(t, e)
        }
    }

    /** Any change the user makes in the app is reflected in the home-screen widgets shortly after. */
    @OptIn(FlowPreview::class)
    private fun watchDataForWidgets() {
        val c = container
        appScope.launch {
            combine(c.favorites.state, c.bookmarks.bookmarks, c.said.entries, c.reminders.reminders) { _, _, _, _ -> Unit }
                .debounce(WIDGET_DEBOUNCE_MS)
                .collect { runCatching { com.ohadsam.tehilim.widget.WidgetUpdater.updateAll(this@TehilimApp) } }
        }
    }

    private companion object {
        const val WIDGET_DEBOUNCE_MS = 700L
    }
}
