package com.ohadsam.tehilim.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "bookmark")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chapter: Int,
    val verse: Int,
    val label: String,
    val snippet: String,
    val colorIndex: Int,
    val note: String?,
    val pinned: Boolean,
    val createdAt: Long,
)

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmark")
    fun all(): Flow<List<BookmarkEntity>>

    @Insert
    suspend fun insert(entity: BookmarkEntity): Long

    @Update
    suspend fun update(entity: BookmarkEntity)

    @Query("DELETE FROM bookmark WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM bookmark")
    suspend fun clear()
}
