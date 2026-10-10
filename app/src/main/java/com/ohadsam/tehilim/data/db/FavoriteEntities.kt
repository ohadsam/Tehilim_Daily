package com.ohadsam.tehilim.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Entity(tableName = "favorite")
@Serializable
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Comma separated [com.ohadsam.tehilim.domain.PsalmRef.compact] values. */
    val chaptersCsv: String,
    val colorIndex: Int,
    val sortOrder: Int,
    val createdAt: Long,
)

@Entity(tableName = "person")
@Serializable
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val motherName: String?,
    val kind: String,
    val yahrzeitDay: Int?,
    val yahrzeitMonth: Int?,
    val yahrzeitYear: Int,
    val note: String?,
    val createdAt: Long,
)

@Entity(tableName = "favorite_person", primaryKeys = ["favoriteId", "personId"])
@Serializable
data class FavoritePersonEntity(val favoriteId: Long, val personId: Long)

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite ORDER BY sortOrder, id")
    fun favorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM person ORDER BY name")
    fun persons(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM favorite_person")
    fun links(): Flow<List<FavoritePersonEntity>>

    @Query("SELECT * FROM favorite ORDER BY sortOrder, id")
    suspend fun favoritesNow(): List<FavoriteEntity>

    @Query("SELECT * FROM person ORDER BY name")
    suspend fun personsNow(): List<PersonEntity>

    @Query("SELECT * FROM favorite_person")
    suspend fun linksNow(): List<FavoritePersonEntity>

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun putFavorites(items: List<FavoriteEntity>)

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun putPersons(items: List<PersonEntity>)

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun putLinks(items: List<FavoritePersonEntity>)

    @Query("DELETE FROM favorite")
    suspend fun clearFavorites()

    @Query("DELETE FROM person")
    suspend fun clearPersons()

    @Query("DELETE FROM favorite_person")
    suspend fun clearAllLinks()

    @Insert
    suspend fun insertFavorite(entity: FavoriteEntity): Long

    @Update
    suspend fun updateFavorite(entity: FavoriteEntity)

    @Query("DELETE FROM favorite WHERE id = :id")
    suspend fun deleteFavorite(id: Long)

    @Insert
    suspend fun insertPerson(entity: PersonEntity): Long

    @Update
    suspend fun updatePerson(entity: PersonEntity)

    @Query("DELETE FROM person WHERE id = :id")
    suspend fun deletePerson(id: Long)

    @Query("DELETE FROM favorite_person WHERE favoriteId = :favoriteId")
    suspend fun clearLinksOfFavorite(favoriteId: Long)

    @Query("DELETE FROM favorite_person WHERE personId = :personId")
    suspend fun clearLinksOfPerson(personId: Long)

    @Insert
    suspend fun insertLinks(links: List<FavoritePersonEntity>)

    @Query("SELECT COALESCE(MAX(sortOrder), 0) + 1 FROM favorite")
    suspend fun nextSortOrder(): Int

    @Transaction
    suspend fun saveFavorite(entity: FavoriteEntity, personIds: List<Long>): Long {
        val id = if (entity.id == 0L) insertFavorite(entity) else entity.id.also { updateFavorite(entity) }
        clearLinksOfFavorite(id)
        insertLinks(personIds.distinct().map { FavoritePersonEntity(id, it) })
        return id
    }

    @Transaction
    suspend fun removePerson(id: Long) {
        clearLinksOfPerson(id)
        deletePerson(id)
    }

    @Transaction
    suspend fun removeFavorite(id: Long) {
        clearLinksOfFavorite(id)
        deleteFavorite(id)
    }
}
