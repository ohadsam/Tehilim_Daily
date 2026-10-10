package com.ohadsam.tehilim.data.db

import com.ohadsam.tehilim.data.PsalmsLocal
import com.ohadsam.tehilim.domain.PsalmChapter
import com.ohadsam.tehilim.domain.Verse
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class RoomPsalmsLocal(private val dao: ChapterDao) : PsalmsLocal {
    private val versesSerializer = ListSerializer(String.serializer())

    override suspend fun get(chapter: Int): PsalmChapter? {
        val e = dao.get(chapter) ?: return null
        val texts = runCatching { Json.decodeFromString(versesSerializer, e.versesJson) }.getOrNull() ?: return null
        // A corrupt/short row is treated as a miss so the repository refetches it.
        if (texts.size != e.verseCount || texts.isEmpty()) return null
        return PsalmChapter(chapter, texts.mapIndexed { i, t -> Verse(i + 1, t) }, e.versionTitle, e.fetchedAt)
    }

    override suspend fun put(chapter: PsalmChapter) {
        val texts = chapter.verses.map { it.text }
        dao.put(
            ChapterEntity(
                chapter = chapter.chapter,
                versesJson = Json.encodeToString(versesSerializer, texts),
                verseCount = texts.size,
                versionTitle = chapter.versionTitle,
                fetchedAt = chapter.fetchedAt,
            ),
        )
    }

    override suspend fun cachedChapters(): Set<Int> = dao.chapters().toSet()

    override suspend fun clear() = dao.clear()
}
