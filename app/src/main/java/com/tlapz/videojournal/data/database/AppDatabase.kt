package com.tlapz.videojournal.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tlapz.videojournal.core.util.Constants

@Database(
    entities = [VideoEntry::class],
    version = Constants.DATABASE_VERSION,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
}
