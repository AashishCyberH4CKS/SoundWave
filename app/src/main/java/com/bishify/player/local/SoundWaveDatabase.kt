package com.bishify.player.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        FavoriteSongEntity::class,
        ListeningHistoryEntity::class,
        ExcludedFolderEntity::class,
        QueueItemEntity::class,
        SavedQueueState::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SoundWaveDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteSongDao(): FavoriteSongDao
    abstract fun historyDao(): HistoryDao
    abstract fun excludedFolderDao(): ExcludedFolderDao
    abstract fun queueDao(): QueueDao

    companion object {
        @Volatile
        private var INSTANCE: SoundWaveDatabase? = null

        fun getInstance(context: Context): SoundWaveDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SoundWaveDatabase::class.java,
                    "soundwave_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

