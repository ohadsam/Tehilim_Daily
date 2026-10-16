package com.ohadsam.tehilim.backup

import com.ohadsam.tehilim.data.db.BookmarkEntity
import com.ohadsam.tehilim.data.db.FavoriteEntity
import com.ohadsam.tehilim.data.db.FavoritePersonEntity
import com.ohadsam.tehilim.data.db.PersonEntity
import com.ohadsam.tehilim.data.db.ReminderEntity
import com.ohadsam.tehilim.data.db.SaidEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Everything the user created, for export / import. Psalm text is not included (it is re-downloadable). */
@Serializable
data class Backup(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val exportedAt: Long,
    val favorites: List<FavoriteEntity> = emptyList(),
    val persons: List<PersonEntity> = emptyList(),
    val links: List<FavoritePersonEntity> = emptyList(),
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val said: List<SaidEntity> = emptyList(),
    /** Lists and divisions made for sharing (older backups simply have none). */
    val shares: List<com.ohadsam.tehilim.share.SharePackage> = emptyList(),
) {
    companion object {
        const val FORMAT = "tehilim-daily-backup"
        const val VERSION = 1
    }
}

object BackupCodec {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(b: Backup): String = json.encodeToString(Backup.serializer(), b)

    /** Throws [IllegalArgumentException] for anything that is not a compatible backup, so a wrong file never wipes data. */
    fun decode(text: String): Backup {
        val b = try {
            json.decodeFromString(Backup.serializer(), text)
        } catch (e: Exception) {
            throw IllegalArgumentException("not a valid backup file", e)
        }
        require(b.format == Backup.FORMAT) { "not a Tehilim Daily backup" }
        require(b.version <= Backup.VERSION) { "backup was made by a newer version of the app" }
        return b
    }
}
