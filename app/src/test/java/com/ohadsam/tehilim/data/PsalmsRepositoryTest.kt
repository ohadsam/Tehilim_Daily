package com.ohadsam.tehilim.data

import com.ohadsam.tehilim.data.sefaria.PsalmsRemote
import com.ohadsam.tehilim.data.sefaria.RemoteChapter
import com.ohadsam.tehilim.domain.PsalmChapter
import com.ohadsam.tehilim.domain.PsalmErrorKind
import com.ohadsam.tehilim.domain.PsalmException
import com.ohadsam.tehilim.domain.PsalmRef
import com.ohadsam.tehilim.domain.PsalmVerseCounts
import com.ohadsam.tehilim.log.AppLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeLocal : PsalmsLocal {
    val rows = mutableMapOf<Int, PsalmChapter>()
    override suspend fun get(chapter: Int) = rows[chapter]
    override suspend fun put(chapter: PsalmChapter) {
        rows[chapter.chapter] = chapter
    }
    override suspend fun cachedChapters() = rows.keys.toSet()
    override suspend fun all() = rows.values.toList()
    override suspend fun clear() = rows.clear()
}

private class FakeRemote(val size: (Int) -> Int = { PsalmVerseCounts.of(it) }) : PsalmsRemote {
    var calls = 0
    var failure: PsalmException? = null
    override suspend fun fetchChapter(chapter: Int): RemoteChapter {
        calls++
        failure?.let { throw it }
        return RemoteChapter(List(size(chapter)) { "פסוק ${it + 1}" }, "Fake")
    }
}

class PsalmsRepositoryTest {
    private val local = FakeLocal()
    private val remote = FakeRemote()
    private val repo = PsalmsRepository(local, remote, AppLogger(file = null), clock = { 1000L }, prefetchDelayMs = 0)

    @Test
    fun missFetchesAndStoresThenHitSkipsNetwork() = runTest {
        val first = repo.get(PsalmRef(1)).getOrThrow()
        assertFalse(first.fromCache)
        assertEquals(PsalmVerseCounts.of(1), first.verses.size)
        val second = repo.get(PsalmRef(1)).getOrThrow()
        assertTrue(second.fromCache)
        assertEquals(1, remote.calls)
        assertEquals(1000L, local.rows.getValue(1).fetchedAt)
    }

    @Test
    fun rangesAreSlicedFromTheWholeChapterWithRealVerseNumbers() = runTest {
        val a = repo.get(PsalmRef(119, 1, 96)).getOrThrow()
        val b = repo.get(PsalmRef(119, 97, 176)).getOrThrow()
        assertEquals((1..96).toList(), a.verses.map { it.number })
        assertEquals((97..176).toList(), b.verses.map { it.number })
        assertEquals("פסוק 97", b.verses.first().text)
        assertEquals(1, remote.calls)
    }

    @Test
    fun rangeBeyondChapterIsInvalidData() = runTest {
        val e = repo.get(PsalmRef(1, 1, 99)).exceptionOrNull() as PsalmException
        assertEquals(PsalmErrorKind.INVALID_DATA, e.kind)
    }

    @Test
    fun offlineWithoutRowFailsButCachedRowStillWorks() = runTest {
        remote.failure = PsalmException(PsalmErrorKind.OFFLINE, "down")
        assertEquals(PsalmErrorKind.OFFLINE, (repo.get(PsalmRef(2)).exceptionOrNull() as PsalmException).kind)
        assertTrue(local.rows.isEmpty())
        remote.failure = null
        repo.get(PsalmRef(3)).getOrThrow()
        remote.failure = PsalmException(PsalmErrorKind.OFFLINE, "down")
        assertTrue(repo.get(PsalmRef(3)).getOrThrow().fromCache)
    }

    @Test
    fun concurrentRequestsForOneChapterFetchOnce() = runTest {
        List(5) { async { repo.get(PsalmRef(7)).getOrThrow() } }.awaitAll()
        assertEquals(1, remote.calls)
    }

    @Test
    fun prefetchFillsAllAndSkipsCachedAndStopsWhenOffline() = runTest {
        repo.get(PsalmRef(1)).getOrThrow()
        val p = repo.prefetchAll()
        assertEquals(150, p.done)
        assertEquals(150, local.rows.size)
        assertEquals(150, remote.calls) // 1 earlier + 149 new

        local.rows.clear()
        remote.calls = 0
        remote.failure = PsalmException(PsalmErrorKind.OFFLINE, "down")
        val q = repo.prefetchAll()
        assertEquals(0, q.done)
        assertEquals(1, remote.calls)
    }

    @Test
    fun chapterWithWrongVerseCountIsRejectedAndNotStored() = runTest {
        val shortRemote = FakeRemote { PsalmVerseCounts.of(it) - 1 }
        val r = PsalmsRepository(local, shortRemote, AppLogger(file = null), clock = { 1L }, prefetchDelayMs = 0)
        val e = r.get(PsalmRef(23)).exceptionOrNull() as PsalmException
        assertEquals(PsalmErrorKind.INVALID_DATA, e.kind)
        assertTrue(local.rows.isEmpty())
    }
}
