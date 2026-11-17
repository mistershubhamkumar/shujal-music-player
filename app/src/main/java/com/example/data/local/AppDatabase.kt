package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        TrackStatsEntity::class,
        RecentlyDeletedPlaylistEntity::class,
        LyricsEntity::class,
        OnlineFavoriteEntity::class,
        SearchHistoryEntity::class,
        ListeningSessionEntity::class,
        ExcludedFolderEntity::class,
        OnlinePlayHistoryEntity::class,
        SavedVideoEntity::class,
        VideoPlaybackProgressEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `online_play_history` (`trackId` TEXT NOT NULL, `trackJson` TEXT NOT NULL, `playedAt` INTEGER NOT NULL, PRIMARY KEY(`trackId`))"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `saved_videos` (`videoId` TEXT NOT NULL, `videoJson` TEXT NOT NULL, `isOnline` INTEGER NOT NULL, `isFavorite` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`videoId`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `video_progress` (`videoId` TEXT NOT NULL, `videoJson` TEXT NOT NULL, `positionMs` INTEGER NOT NULL, `durationMs` INTEGER NOT NULL, `lastWatchedAt` INTEGER NOT NULL, PRIMARY KEY(`videoId`))"
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shujal_music_db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
