package uk.appyapp.stepsafe.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import uk.appyapp.stepsafe.data.local.dao.CaregiverContactDao
import uk.appyapp.stepsafe.data.local.dao.HomeLocationDao
import uk.appyapp.stepsafe.data.local.dao.ExitEventDao
import uk.appyapp.stepsafe.data.local.entities.CaregiverContact
import uk.appyapp.stepsafe.data.local.entities.HomeLocation
import uk.appyapp.stepsafe.data.local.entities.ExitEvent

@Database(
    entities = [HomeLocation::class, CaregiverContact::class, ExitEvent::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun homeLocationDao(): HomeLocationDao
    abstract fun caregiverContactDao(): CaregiverContactDao
    abstract fun exitEventDao(): ExitEventDao
}
