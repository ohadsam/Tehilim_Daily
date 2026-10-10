package com.ohadsam.tehilim.data

import com.ohadsam.tehilim.data.sefaria.PsalmsRemote
import com.ohadsam.tehilim.domain.PSALMS_COUNT
import com.ohadsam.tehilim.domain.PsalmChapter
import com.ohadsam.tehilim.domain.PsalmErrorKind
import com.ohadsam.tehilim.domain.PsalmException
import com.ohadsam.tehilim.domain.PsalmRef
import com.ohadsam.tehilim.domain.PsalmText
import com.ohadsam.tehilim.domain.Verse
import com.ohadsam.tehilim.domain.slice
import com.ohadsam.tehilim.log.AppLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

interface PsalmsLocal {
    suspend fun get(chapter: Int): PsalmChapter?
    suspend fun put(chapter: PsalmChapter)
    suspend fun cachedChapters(): Set<Int>
    suspend fun clear()
}

data class PrefetchProgress(val done: Int, val total: Int, val failed: Int)

/**
 * The only way the app obtains psalm text: local store first, Sefaria as the fallback.
 * A chapter is stored only after passing the integrity checks in [com.ohadsam.tehilim.data.sefaria.SefariaParser],
 * so a transient bad answer can never become permanent.
 */
class PsalmsRepository(
    private val local: PsalmsLocal,
    private val remote: PsalmsRemote,
    private val logger: AppLogger,
    private val clock: () -> Long = System::currentTimeMillis,
    private val prefetchDelayMs: Long = 350,
) {
    private val locks = ConcurrentHashMap<Int, Mutex>()

    suspend fun get(ref: PsalmRef): Result<PsalmText> = runCatchingPsalm {
        val (chapter, fromCache) = loadChapter(ref.chapter)
        PsalmText(ref, chapter.slice(ref), chapter.versionTitle, fromCache)
    }

    suspend fun cachedChapters(): Set<Int> = local.cachedChapters()

    suspend fun clearCache() {
        local.clear()
        logger.i(TAG, "cache cleared")
    }

    /** Downloads every missing chapter, throttled; stops early (returning the progress) when the device is offline. */
    suspend fun prefetchAll(onProgress: (PrefetchProgress) -> Unit = {}): PrefetchProgress {
        val have = local.cachedChapters().toMutableSet()
        var done = have.size
        var failed = 0
        onProgress(PrefetchProgress(done, PSALMS_COUNT, failed))
        for (ch in 1..PSALMS_COUNT) {
            if (ch in have) continue
            val result = runCatchingPsalm { loadChapter(ch) }
            if (result.isSuccess) {
                done++
            } else {
                failed++
                if ((result.exceptionOrNull() as? PsalmException)?.kind == PsalmErrorKind.OFFLINE) {
                    onProgress(PrefetchProgress(done, PSALMS_COUNT, failed))
                    break
                }
            }
            onProgress(PrefetchProgress(done, PSALMS_COUNT, failed))
            delay(prefetchDelayMs)
        }
        logger.i(TAG, "prefetch finished: $done/$PSALMS_COUNT, failed=$failed")
        return PrefetchProgress(done, PSALMS_COUNT, failed)
    }

    private suspend fun loadChapter(chapter: Int): Pair<PsalmChapter, Boolean> {
        local.get(chapter)?.let { return it to true }
        return locks.getOrPut(chapter) { Mutex() }.withLock {
            // Another caller may have stored it while we waited for the lock.
            local.get(chapter)?.let { return@withLock it to true }
            logger.d(TAG, "cache miss Psalms.$chapter → Sefaria")
            val fetched = remote.fetchChapter(chapter)
            val stored = PsalmChapter(
                chapter = chapter,
                verses = fetched.verses.mapIndexed { i, t -> Verse(i + 1, t) },
                versionTitle = fetched.versionTitle,
                fetchedAt = clock(),
            )
            local.put(stored)
            stored to false
        }
    }

    private inline fun <T> runCatchingPsalm(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: PsalmException) {
        logger.w(TAG, "${e.kind}: ${e.message}")
        Result.failure(e)
    }

    private companion object {
        const val TAG = "Psalms"
    }
}
