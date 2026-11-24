package com.ohadsam.tehilim

import android.content.Context
import android.util.Log
import com.ohadsam.tehilim.log.AppLogger
import com.ohadsam.tehilim.log.LogLevel
import androidx.room.Room
import com.ohadsam.tehilim.data.BookmarksRepository
import com.ohadsam.tehilim.data.FavoritesRepository
import com.ohadsam.tehilim.data.PrefetchController
import com.ohadsam.tehilim.data.PsalmsRepository
import com.ohadsam.tehilim.data.RemindersRepository
import com.ohadsam.tehilim.data.SaidRepository
import com.ohadsam.tehilim.data.db.AppDatabase
import com.ohadsam.tehilim.data.db.RoomPsalmsLocal
import com.ohadsam.tehilim.backup.BackupRepository
import com.ohadsam.tehilim.data.prefs.LogPrefs
import com.ohadsam.tehilim.data.prefs.PositionStore
import com.ohadsam.tehilim.data.prefs.PrayerPrefs
import com.ohadsam.tehilim.data.prefs.ReaderPrefs
import com.ohadsam.tehilim.data.prefs.ThemePrefs
import com.ohadsam.tehilim.data.sefaria.SefariaApi
import com.ohadsam.tehilim.location.LocationStore
import com.ohadsam.tehilim.reminders.NotificationHelper
import com.ohadsam.tehilim.reminders.ReminderScheduler
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/** Manual dependency container (no DI framework); created once in [TehilimApp]. */
class AppContainer(context: Context) {
    val logger: AppLogger = AppLogger(
        file = File(context.filesDir, "logs/app.log"),
        echo = { e ->
            when (e.level) {
                LogLevel.DEBUG -> Log.d(e.tag, e.message)
                LogLevel.INFO -> Log.i(e.tag, e.message)
                LogLevel.WARN -> Log.w(e.tag, e.message)
                LogLevel.ERROR -> Log.e(e.tag, e.message)
            }
        },
    )

    val readerPrefs: ReaderPrefs = ReaderPrefs(context.applicationContext)

    val autoScrollPrefs: com.ohadsam.tehilim.data.prefs.AutoScrollPrefs = com.ohadsam.tehilim.data.prefs.AutoScrollPrefs(context.applicationContext)

    val positions: PositionStore = PositionStore(context.applicationContext)

    val themePrefs: ThemePrefs = ThemePrefs(context.applicationContext)

    val prayerPrefs: PrayerPrefs = PrayerPrefs(context.applicationContext)

    val logPrefs: LogPrefs = LogPrefs(context.applicationContext)

    val prayers: List<com.ohadsam.tehilim.prayers.PrayerText> by lazy { com.ohadsam.tehilim.prayers.PrayerCatalog.load(context.applicationContext) }

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "tehilim.db")
            // Pre-release debug builds may change the schema between installs; release builds never wipe data (add migrations instead).
            .addMigrations(com.ohadsam.tehilim.data.db.Migrations.V1_V2, com.ohadsam.tehilim.data.db.Migrations.V2_V3)
            .apply { if (com.ohadsam.tehilim.BuildConfig.DEBUG) fallbackToDestructiveMigration(dropAllTables = true) }
            .build()
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val psalms: PsalmsRepository by lazy {
        PsalmsRepository(RoomPsalmsLocal(database.chapterDao()), SefariaApi(httpClient), logger)
    }

    val commentary: com.ohadsam.tehilim.data.CommentaryRepository by lazy {
        com.ohadsam.tehilim.data.CommentaryRepository(
            com.ohadsam.tehilim.data.db.RoomCommentaryLocal(database.commentaryDao()),
            com.ohadsam.tehilim.data.sefaria.SefariaCommentaryApi(httpClient),
            logger,
        )
    }

    val favorites: FavoritesRepository by lazy { FavoritesRepository(database.favoriteDao(), logger) }

    val said: SaidRepository by lazy { SaidRepository(database.saidDao(), logger) }

    val bookmarks: BookmarksRepository by lazy { BookmarksRepository(database.bookmarkDao(), logger) }

    val reminders: RemindersRepository by lazy { RemindersRepository(database.reminderDao(), logger) }

    val locations: LocationStore = LocationStore(context.applicationContext)

    val notifications: NotificationHelper = NotificationHelper(context.applicationContext)

    val reminderScheduler: ReminderScheduler by lazy {
        ReminderScheduler(context.applicationContext, reminders, locations, logger)
    }

    val sharedLists: com.ohadsam.tehilim.share.SharedListsStore = com.ohadsam.tehilim.share.SharedListsStore(context.applicationContext)

    /** Text of a share that arrived from outside (opened file / forwarded message), waiting for the sharing screen to show it. */
    val shareInbox = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    val shareImporter: com.ohadsam.tehilim.share.ShareImporter by lazy { com.ohadsam.tehilim.share.ShareImporter(favorites) }

    val updates: com.ohadsam.tehilim.update.UpdateController by lazy {
        com.ohadsam.tehilim.update.UpdateController(
            context.applicationContext, httpClient, appScope, logger,
            com.ohadsam.tehilim.BuildConfig.VERSION_CODE.toLong(), com.ohadsam.tehilim.BuildConfig.VERSION_NAME,
        )
    }

    val backup: BackupRepository by lazy { BackupRepository(database, logger, sharedLists) }

    private val appScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)

    val prefetch: PrefetchController by lazy { PrefetchController(psalms, appScope) }

    private val appContext = context.applicationContext

    /** A string resource formatted with [args], for places outside composables. */
    fun appString(res: Int, vararg args: Any): String = appContext.getString(res, *args)

    val nameLetters: com.ohadsam.tehilim.favorites.NameLettersService by lazy {
        com.ohadsam.tehilim.favorites.NameLettersService(favorites, reminders, reminderScheduler)
    }
}
