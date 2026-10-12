package com.ohadsam.tehilim.data

import com.ohadsam.tehilim.data.sefaria.CommentaryRemote
import com.ohadsam.tehilim.domain.CommentarySource
import com.ohadsam.tehilim.domain.PsalmErrorKind
import com.ohadsam.tehilim.domain.PsalmException
import com.ohadsam.tehilim.log.AppLogger
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommentaryRepositoryTest {
    private class FakeLocal : CommentaryLocal {
        val rows = mutableMapOf<String, Map<Int, List<String>>>()

        override suspend fun get(chapter: Int, source: CommentarySource) = rows["${source.key}:$chapter"]

        override suspend fun put(chapter: Int, source: CommentarySource, comments: Map<Int, List<String>>, fetchedAt: Long) {
            rows["${source.key}:$chapter"] = comments
        }

        override suspend fun clear() = rows.clear()
    }

    private class FakeRemote(var answer: () -> Map<Int, List<String>>) : CommentaryRemote {
        var calls = 0

        override suspend fun fetch(chapter: Int, source: CommentarySource): Map<Int, List<String>> {
            calls++
            return answer()
        }
    }

    private val logger = AppLogger(file = null, clock = { 0L })

    @Test
    fun secondReadComesFromTheLocalStore() = runTest {
        val remote = FakeRemote { mapOf(1 to listOf("א")) }
        val repo = CommentaryRepository(FakeLocal(), remote, logger)
        assertEquals(mapOf(1 to listOf("א")), repo.get(1, CommentarySource.RASHI).getOrThrow())
        repo.get(1, CommentarySource.RASHI).getOrThrow()
        assertEquals(1, remote.calls)
        repo.get(1, CommentarySource.RADAK).getOrThrow() // another commentator = another row
        assertEquals(2, remote.calls)
    }

    @Test
    fun emptyAnswersAndErrorsAreNeverStored() = runTest {
        val local = FakeLocal()
        val remote = FakeRemote { emptyMap() }
        val repo = CommentaryRepository(local, remote, logger)
        assertTrue(repo.get(2, CommentarySource.RASHI).getOrThrow().isEmpty())
        assertTrue(local.rows.isEmpty())
        remote.answer = { throw PsalmException(PsalmErrorKind.OFFLINE, "down") }
        val failed = repo.get(2, CommentarySource.RASHI)
        assertEquals(PsalmErrorKind.OFFLINE, (failed.exceptionOrNull() as PsalmException).kind)
        assertTrue(local.rows.isEmpty())
    }
}
