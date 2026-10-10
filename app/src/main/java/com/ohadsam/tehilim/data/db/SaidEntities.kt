package com.ohadsam.tehilim.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Entity(tableName = "said", primaryKeys = ["epochDay", "contextType", "contextId"])
@Serializable
data class SaidEntity(val epochDay: Long, val contextType: String, val contextId: String, val markedAt: Long)

@Dao
interface SaidDao {
    @Query("SELECT * FROM said WHERE epochDay >= :fromDay ORDER BY epochDay DESC, markedAt DESC")
    fun since(fromDay: Long): Flow<List<SaidEntity>>

    @Query("SELECT * FROM said")
    suspend fun allNow(): List<SaidEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAll(items: List<SaidEntity>)

    @Query("DELETE FROM said")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: SaidEntity)

    @Query("DELETE FROM said WHERE epochDay = :epochDay AND contextType = :type AND contextId = :id")
    suspend fun remove(epochDay: Long, type: String, id: String)

    @Query("SELECT COUNT(*) FROM said WHERE epochDay = :epochDay AND contextType = :type AND contextId = :id")
    suspend fun count(epochDay: Long, type: String, id: String): Int

    @Query("DELETE FROM said WHERE epochDay < :beforeDay")
    suspend fun purgeBefore(beforeDay: Long): Int
}
