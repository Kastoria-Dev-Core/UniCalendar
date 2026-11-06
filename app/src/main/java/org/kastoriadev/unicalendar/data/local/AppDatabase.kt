package org.kastoriadev.unicalendar.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [EventEntity::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * v4 adds the `feed` column so timetable, exam and calendar imports can be replaced
         * independently. Every event synced before v4 came from the timetable sync.
         * Keeps users' notes and custom events (the previous fallback wiped them).
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `events` ADD COLUMN `feed` TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE `events` SET `feed` = 'timetable' WHERE `isSynced` = 1")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "unicalendar.db"
                )
                    .addMigrations(MIGRATION_3_4)
                    // Only used for versions without a migration path (pre-release builds).
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
