package uk.appyapp.stepsafe.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "home_location")
data class HomeLocation(
    @PrimaryKey val id: Int = 0,
    val latitude: Double,
    val longitude: Double,
    val address: String
)
