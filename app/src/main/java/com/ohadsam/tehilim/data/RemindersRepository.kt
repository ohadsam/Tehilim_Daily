package com.ohadsam.tehilim.data

import com.ohadsam.tehilim.data.db.ReminderDao
import com.ohadsam.tehilim.log.AppLogger
import com.ohadsam.tehilim.reminders.Reminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RemindersRepository(private val dao: ReminderDao, private val logger: AppLogger) {
    val reminders: Flow<List<Reminder>> = dao.all().map { rows -> rows.mapNotNull(ReminderCodec::decode) }

    suspend fun snapshot(): List<Reminder> = dao.snapshot().mapNotNull(ReminderCodec::decode)

    suspend fun get(id: Long): Reminder? = dao.get(id)?.let(ReminderCodec::decode)

    /** Insert (id == 0) or update; returns the row id. */
    suspend fun save(r: Reminder): Long {
        val e = ReminderCodec.encode(r)
        val id = if (r.id == 0L) dao.insert(e) else r.id.also { dao.update(e) }
        logger.i(TAG, "reminder saved id=$id times=${r.times.size} repeat=${r.repeat::class.simpleName}")
        return id
    }

    suspend fun delete(id: Long) {
        dao.delete(id)
        logger.i(TAG, "reminder deleted id=$id")
    }

    private companion object {
        const val TAG = "Reminders"
    }
}
