package com.ohadsam.tehilim.data

import com.ohadsam.tehilim.data.sefaria.CommentaryRemote
import com.ohadsam.tehilim.domain.CommentarySource
import com.ohadsam.tehilim.domain.PsalmErrorKind
import com.ohadsam.tehilim.domain.PsalmException
import com.ohadsam.tehilim.log.AppLogger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

interface CommentaryLocal {
    suspend fun get(chapter: Int, source: CommentarySource): Map<Int, List<String>>?

    suspend fun put(chapter: Int, source: CommentarySource, comments: Map<Int, List<String>>, fetchedAt: Long)

    suspend fun clear()
}

/**
 * Commentary on a chapter: local store first, Sefaria as the fallback (same contract as [PsalmsRepository]).
 * Only non-empty answers are stored, so a transient empty/bad reply never becomes permanent.
 */
class CommentaryRepository(
    private val local: CommentaryLocal,
    private val remote: CommentaryRemote,
    private val logger: AppLogger,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val locks = ConcurrentHashMap<String, Mutex>()

    /** verse number → comments of that verse (verses without commentary are absent). */
    suspend fun get(chapter: Int, source: CommentarySource): Result<Map<Int, List<String>>> = try {
        Result.success(load(chapter, source))
    } catch (e: PsalmException) {
        logger.w(TAG, "commentary ${source.key} $chapter failed: ${e.kind} ${e.message}")
        Result.failure(e)
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.w(TAG, "commentary ${source.key} $chapter failed: ${e.message}")
        Result.failure(PsalmException(PsalmErrorKind.INVALID_DATA, e.message ?: "commentary failed", e))
    }

    suspend fun clearCache() = local.clear()

    private suspend fun load(chapter: Int, source: CommentarySource): Map<Int, List<String>> {
        local.get(chapter, source)?.let { return it }
        return locks.getOrPut("${source.key}:$chapter") { Mutex() }.withLock {
            local.get(chapter, source)?.let { return@withLock it }
            val fetched = remote.fetch(chapter, source)
            if (fetched.isNotEmpty()) local.put(chapter, source, fetched, clock())
            fetched
        }
    }

    private companion object {
        const val TAG = "Commentary"
    }
}
