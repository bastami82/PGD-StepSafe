package uk.appyapp.stepsafe.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import uk.appyapp.stepsafe.data.local.dao.CaregiverContactDao
import uk.appyapp.stepsafe.data.local.dao.HomeLocationDao
import uk.appyapp.stepsafe.data.local.entities.CaregiverContact
import uk.appyapp.stepsafe.data.local.entities.HomeLocation

@Database(
    entities = [HomeLocation::class, CaregiverContact::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun homeLocationDao(): HomeLocationDao
    abstract fun caregiverContactDao(): CaregiverContactDao
}
