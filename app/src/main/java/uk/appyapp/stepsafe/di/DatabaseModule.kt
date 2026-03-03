package uk.appyapp.stepsafe.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import uk.appyapp.stepsafe.data.local.AppDatabase
import uk.appyapp.stepsafe.data.local.dao.CaregiverContactDao
import uk.appyapp.stepsafe.data.local.dao.HomeLocationDao
import uk.appyapp.stepsafe.data.local.dao.ExitEventDao
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.RoomDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        // Migration from version 1 -> 2: create exit_event table
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `exit_event` (
                      `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                      `timestamp` INTEGER NOT NULL,
                      `latitude` REAL NOT NULL,
                      `longitude` REAL NOT NULL,
                      `eventType` TEXT NOT NULL,
                      `note` TEXT
                    )
                    """.trimIndent()
                )
            }
        }

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "stepsafe_db"
        ).addMigrations(MIGRATION_1_2).build()
    }

    @Provides
    fun provideHomeLocationDao(database: AppDatabase): HomeLocationDao {
        return database.homeLocationDao()
    }

    @Provides
    fun provideCaregiverContactDao(database: AppDatabase): CaregiverContactDao {
        return database.caregiverContactDao()
    }

    @Provides
    fun provideExitEventDao(database: AppDatabase): ExitEventDao {
        return database.exitEventDao()
    }
}
