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
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "stepsafe_db"
        ).build()
    }

    @Provides
    fun provideHomeLocationDao(database: AppDatabase): HomeLocationDao {
        return database.homeLocationDao()
    }

    @Provides
    fun provideCaregiverContactDao(database: AppDatabase): CaregiverContactDao {
        return database.caregiverContactDao()
    }
}
