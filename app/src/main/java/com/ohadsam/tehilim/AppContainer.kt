package com.ohadsam.tehilim

import android.content.Context
import android.util.Log
import com.ohadsam.tehilim.log.AppLogger
import com.ohadsam.tehilim.log.LogLevel
import androidx.room.Room
import com.ohadsam.tehilim.data.FavoritesRepository
import com.ohadsam.tehilim.data.PsalmsRepository
import com.ohadsam.tehilim.data.db.AppDatabase
import com.ohadsam.tehilim.data.db.RoomPsalmsLocal
import com.ohadsam.tehilim.data.prefs.ReaderPrefs
import com.ohadsam.tehilim.data.sefaria.SefariaApi
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

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "tehilim.db").build()
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

    val favorites: FavoritesRepository by lazy { FavoritesRepository(database.favoriteDao(), logger) }
}
