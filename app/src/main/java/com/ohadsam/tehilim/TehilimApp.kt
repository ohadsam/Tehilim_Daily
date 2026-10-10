package com.ohadsam.tehilim

import android.app.Application
import com.ohadsam.tehilim.log.AppLogger
import kotlinx.coroutines.CoroutineScope
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
        log.purgeOlderThanDays(AppLogger.DEFAULT_RETENTION_DAYS)
        log.i("App", "start v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        container.notifications.createChannels()
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
}
