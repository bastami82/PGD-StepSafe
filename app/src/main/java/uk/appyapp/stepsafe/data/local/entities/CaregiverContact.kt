package uk.appyapp.stepsafe.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "caregiver_contact")
data class CaregiverContact(
    @PrimaryKey val id: Int = 0,
    val name: String,
    val phoneNumber: String
)
