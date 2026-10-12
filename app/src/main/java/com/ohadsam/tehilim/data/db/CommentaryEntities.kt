package com.ohadsam.tehilim.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Upsert
import com.ohadsam.tehilim.data.CommentaryLocal
import com.ohadsam.tehilim.domain.CommentarySource
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** One row per (chapter, commentator); [json] is a map verse number → list of comments. */
@Entity(tableName = "commentary_chapter", primaryKeys = ["chapter", "source"])
data class CommentaryEntity(
    val chapter: Int,
    val source: String,
    val json: String,
    val fetchedAt: Long,
)

@Dao
interface CommentaryDao {
    @Query("SELECT * FROM commentary_chapter WHERE chapter = :chapter AND source = :source")
    suspend fun get(chapter: Int, source: String): CommentaryEntity?

    @Upsert
    suspend fun put(entity: CommentaryEntity)

    @Query("DELETE FROM commentary_chapter")
    suspend fun clear()
}

class RoomCommentaryLocal(private val dao: CommentaryDao) : CommentaryLocal {
    private val serializer = MapSerializer(Int.serializer(), ListSerializer(String.serializer()))

    override suspend fun get(chapter: Int, source: CommentarySource): Map<Int, List<String>>? {
        val e = dao.get(chapter, source.key) ?: return null
        // A corrupt row is a miss so the repository refetches it.
        return runCatching { Json.decodeFromString(serializer, e.json) }.getOrNull()?.takeIf { it.isNotEmpty() }
    }

    override suspend fun put(chapter: Int, source: CommentarySource, comments: Map<Int, List<String>>, fetchedAt: Long) {
        dao.put(CommentaryEntity(chapter, source.key, Json.encodeToString(serializer, comments), fetchedAt))
    }

    override suspend fun clear() = dao.clear()
}
