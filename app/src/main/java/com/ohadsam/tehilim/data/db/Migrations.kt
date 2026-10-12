package com.ohadsam.tehilim.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Release builds never wipe user data: every schema change ships with a migration here. */
object Migrations {
    /** v2: "name letters" favorites remember the names they were made from. */
    val V1_V2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE favorite ADD COLUMN nameSpec TEXT")
        }
    }

    /** v3: cached commentary (פירושים) per chapter and commentator. */
    val V2_V3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `commentary_chapter` (`chapter` INTEGER NOT NULL, `source` TEXT NOT NULL, " +
                    "`json` TEXT NOT NULL, `fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`chapter`, `source`))",
            )
        }
    }
}
