package com.twinmind.voicerecorder.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [SessionEntity::class, ChunkEntity::class, SummaryEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(EnumsTc::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): RecordingDao
}
