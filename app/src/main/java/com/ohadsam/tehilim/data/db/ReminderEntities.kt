package com.ohadsam.tehilim.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "reminder")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String?,
    val targetType: String,
    val targetId1: Long,
    val targetId2: Long,
    /** "HH:mm,HH:mm" */
    val timesCsv: String,
    val repeatType: String,
    /** ONCE: epoch day · WEEKLY: bitmask (bit 0 = Monday) · MONTHLY_HEBREW: day · otherwise 0 */
    val repeatArg: Long,
    val startEpochDay: Long?,
    val endEpochDay: Long?,
    val sound: Boolean,
    val vibrate: Boolean,
    val snoozeEnabled: Boolean,
    val snoozeMinutes: Int,
    val snoozeMax: Int,
    val holyRule: String,
    val enabled: Boolean,
)

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminder ORDER BY id")
    fun all(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminder ORDER BY id")
    suspend fun snapshot(): List<ReminderEntity>

    @Query("SELECT * FROM reminder WHERE id = :id")
    suspend fun get(id: Long): ReminderEntity?

    @Insert
    suspend fun insert(entity: ReminderEntity): Long

    @Update
    suspend fun update(entity: ReminderEntity)

    @Query("DELETE FROM reminder WHERE id = :id")
    suspend fun delete(id: Long)
}
