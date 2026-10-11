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
}
