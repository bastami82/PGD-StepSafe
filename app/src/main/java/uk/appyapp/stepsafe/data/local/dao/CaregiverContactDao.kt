package uk.appyapp.stepsafe.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import uk.appyapp.stepsafe.data.local.entities.CaregiverContact

@Dao
interface CaregiverContactDao {
    @Query("SELECT * FROM caregiver_contact WHERE id = 0")
    fun getCaregiverContact(): Flow<CaregiverContact?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCaregiverContact(caregiverContact: CaregiverContact)
}
