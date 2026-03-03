package uk.appyapp.stepsafe.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import uk.appyapp.stepsafe.data.local.entities.ExitEvent

@Dao
interface ExitEventDao {
    @Insert
    suspend fun insert(exitEvent: ExitEvent)

    @Query("SELECT * FROM exit_event ORDER BY timestamp DESC LIMIT :limit")
    fun recentEvents(limit: Int): Flow<List<ExitEvent>>
}

