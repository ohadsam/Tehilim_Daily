package com.ohadsam.tehilim.backup

import androidx.room.withTransaction
import com.ohadsam.tehilim.data.db.AppDatabase
import com.ohadsam.tehilim.log.AppLogger
import com.ohadsam.tehilim.share.SharedListsStore
import com.ohadsam.tehilim.share.validate

class BackupRepository(
    private val db: AppDatabase,
    private val logger: AppLogger,
    private val shares: SharedListsStore? = null,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun export(): String {
        val fav = db.favoriteDao()
        val b = Backup(
            exportedAt = clock(),
            favorites = fav.favoritesNow(), persons = fav.personsNow(), links = fav.linksNow(),
            bookmarks = db.bookmarkDao().allNow(), reminders = db.reminderDao().snapshot(), said = db.saidDao().allNow(),
            shares = shares?.items?.value.orEmpty(),
        )
        logger.i(TAG, "export: ${b.favorites.size} favorites, ${b.persons.size} persons, ${b.bookmarks.size} bookmarks, ${b.reminders.size} reminders")
        return BackupCodec.encode(b)
    }

    /** Replaces all user data with the backup's, atomically. */
    suspend fun import(text: String): Backup {
        val b = BackupCodec.decode(text)
        db.withTransaction {
            val fav = db.favoriteDao()
            fav.clearAllLinks()
            fav.clearFavorites()
            fav.clearPersons()
            db.reminderDao().clearAll()
            db.saidDao().clearAll()
            db.bookmarkDao().let { dao -> dao.allNow().forEach { dao.delete(it.id) } }
            fav.putFavorites(b.favorites)
            fav.putPersons(b.persons)
            fav.putLinks(b.links)
            db.bookmarkDao().putAll(b.bookmarks)
            db.reminderDao().putAll(b.reminders)
            db.saidDao().putAll(b.said)
        }
        shares?.replaceAll(b.shares.filter { it.validate() == null })
        logger.i(TAG, "import: ${b.favorites.size} favorites, ${b.persons.size} persons, ${b.bookmarks.size} bookmarks, ${b.reminders.size} reminders")
        return b
    }

    private companion object {
        const val TAG = "Backup"
    }
}
