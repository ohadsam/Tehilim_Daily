package com.ohadsam.tehilim.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert

/** One row per whole chapter; [versesJson] is a JSON array of verse strings (index + 1 = verse number). */
@Entity(tableName = "psalm_chapter")
data class ChapterEntity(
    @PrimaryKey val chapter: Int,
    val versesJson: String,
    val verseCount: Int,
    val versionTitle: String?,
    val fetchedAt: Long,
)

@Dao
interface ChapterDao {
    @Query("SELECT * FROM psalm_chapter WHERE chapter = :chapter")
    suspend fun get(chapter: Int): ChapterEntity?

    @Upsert
    suspend fun put(entity: ChapterEntity)

    @Query("SELECT chapter FROM psalm_chapter")
    suspend fun chapters(): List<Int>

    @Query("DELETE FROM psalm_chapter")
    suspend fun clear()
}

@Database(
    entities = [ChapterEntity::class, FavoriteEntity::class, PersonEntity::class, FavoritePersonEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chapterDao(): ChapterDao

    abstract fun favoriteDao(): FavoriteDao
}
