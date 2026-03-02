package uk.appyapp.stepsafe.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import uk.appyapp.stepsafe.data.local.entities.HomeLocation

@Dao
interface HomeLocationDao {
    @Query("SELECT * FROM home_location WHERE id = 0")
    fun getHomeLocation(): Flow<HomeLocation?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomeLocation(homeLocation: HomeLocation)
}
