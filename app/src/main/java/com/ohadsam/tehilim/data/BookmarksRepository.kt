package com.ohadsam.tehilim.data

import com.ohadsam.tehilim.bookmarks.Bookmark
import com.ohadsam.tehilim.bookmarks.BookmarkText
import com.ohadsam.tehilim.data.db.BookmarkDao
import com.ohadsam.tehilim.data.db.BookmarkEntity
import com.ohadsam.tehilim.log.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BookmarksRepository(
    private val dao: BookmarkDao,
    private val logger: AppLogger,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    val bookmarks: Flow<List<Bookmark>> = dao.all().map { rows -> BookmarkText.sorted(rows.map(::toDomain)) }

    /** Creates a bookmark at a verse; the snippet comes from the already-loaded verse text. */
    suspend fun add(chapter: Int, verse: Int, verseText: String): Bookmark {
        val b = Bookmark(
            chapter = chapter,
            verse = verse,
            label = BookmarkText.defaultLabel(chapter, verse),
            snippet = BookmarkText.snippet(verseText),
            createdAt = clock(),
        )
        val id = dao.insert(toEntity(b))
        logger.i(TAG, "bookmark added Psalms.$chapter:$verse")
        return b.copy(id = id)
    }

    suspend fun update(b: Bookmark) = dao.update(toEntity(b))

    suspend fun delete(id: Long) {
        dao.delete(id)
        logger.i(TAG, "bookmark deleted id=$id")
    }

    /** Used by Undo: re-inserts with a fresh id but the same content and creation time. */
    suspend fun restore(b: Bookmark) {
        dao.insert(toEntity(b.copy(id = 0)))
    }

    suspend fun clear() {
        dao.clear()
        logger.i(TAG, "bookmarks cleared")
    }

    private fun toEntity(b: Bookmark) =
        BookmarkEntity(b.id, b.chapter, b.verse, b.label, b.snippet, b.colorIndex, b.note, b.pinned, b.createdAt)

    private fun toDomain(e: BookmarkEntity) =
        Bookmark(e.id, e.chapter, e.verse, e.label, e.snippet, e.colorIndex, e.note, e.pinned, e.createdAt)

    private companion object {
        const val TAG = "Bookmarks"
    }
}
