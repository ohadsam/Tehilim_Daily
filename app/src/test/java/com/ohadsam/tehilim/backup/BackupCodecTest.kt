package com.ohadsam.tehilim.backup

import com.ohadsam.tehilim.data.db.BookmarkEntity
import com.ohadsam.tehilim.data.db.FavoriteEntity
import com.ohadsam.tehilim.data.db.FavoritePersonEntity
import com.ohadsam.tehilim.data.db.PersonEntity
import com.ohadsam.tehilim.data.db.ReminderEntity
import com.ohadsam.tehilim.data.db.SaidEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupCodecTest {
    private val sample = Backup(
        exportedAt = 123,
        favorites = listOf(FavoriteEntity(1, "ישועה", "20,21,119:1-8", 2, 1, 5)),
        persons = listOf(PersonEntity(1, "דוד", null, "REFUAH", null, null, 0, "הערה", 9)),
        links = listOf(FavoritePersonEntity(1, 1)),
        bookmarks = listOf(BookmarkEntity(1, 23, 1, "תהילים כ״ג", "ה' רעי", 0, null, true, 7)),
        reminders = listOf(ReminderEntity(1, null, "MONTH_DAY", 0, 0, "07:30", "DAILY", 0, null, null, true, false, true, 10, 3, "BOTH", true)),
        said = listOf(SaidEntity(20000, "MONTH_DAY", "29", 55)),
    )

    @Test
    fun roundTrip() {
        assertEquals(sample, BackupCodec.decode(BackupCodec.encode(sample)))
    }

    @Test
    fun rejectsGarbageAndForeignOrNewerFiles() {
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode("not json") }
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode("""{"format":"other","version":1,"exportedAt":1}""") }
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode("""{"format":"tehilim-daily-backup","version":99,"exportedAt":1}""") }
    }

    @Test
    fun hebrewTextSurvivesExactly() {
        val text = BackupCodec.encode(sample)
        assertEquals("ישועה", BackupCodec.decode(text).favorites.single().name)
    }
}
