package com.ohadsam.tehilim.data

import com.ohadsam.tehilim.data.db.SaidDao
import com.ohadsam.tehilim.data.db.SaidEntity
import com.ohadsam.tehilim.log.AppLogger
import com.ohadsam.tehilim.said.HISTORY_DAYS
import com.ohadsam.tehilim.said.SaidContext
import com.ohadsam.tehilim.said.SaidEntry
import com.ohadsam.tehilim.said.SaidType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** "Was it said today?" marks and the rolling 30-day history. */
class SaidRepository(
    private val dao: SaidDao,
    private val logger: AppLogger,
    private val clock: () -> Long = System::currentTimeMillis,
    private val today: () -> LocalDate = LocalDate::now,
) {
    val entries: Flow<List<SaidEntry>> = dao.since(today().minusDays(HISTORY_DAYS.toLong()).toEpochDay()).map { rows ->
        rows.mapNotNull { r ->
            val type = runCatching { SaidType.valueOf(r.contextType) }.getOrNull() ?: return@mapNotNull null
            SaidEntry(LocalDate.ofEpochDay(r.epochDay), SaidContext(type, r.contextId), r.markedAt)
        }
    }

    /** Flips the mark for [context] on [date]; returns the new state. */
    suspend fun toggle(date: LocalDate, context: SaidContext): Boolean {
        val day = date.toEpochDay()
        val nowSaid = dao.count(day, context.type.name, context.id) == 0
        if (nowSaid) dao.put(SaidEntity(day, context.type.name, context.id, clock())) else dao.remove(day, context.type.name, context.id)
        logger.i(TAG, "${context.type} ${context.id} on $date -> ${if (nowSaid) "said" else "not said"}")
        return nowSaid
    }

    suspend fun isSaid(date: LocalDate, context: SaidContext): Boolean = dao.count(date.toEpochDay(), context.type.name, context.id) > 0

    suspend fun mark(date: LocalDate, context: SaidContext) {
        dao.put(SaidEntity(date.toEpochDay(), context.type.name, context.id, clock()))
    }

    /** Keeps only [HISTORY_DAYS] days back. */
    suspend fun purgeOld() {
        val removed = dao.purgeBefore(today().minusDays(HISTORY_DAYS.toLong()).toEpochDay())
        if (removed > 0) logger.i(TAG, "purged $removed old said entries")
    }

    private companion object {
        const val TAG = "Said"
    }
}
