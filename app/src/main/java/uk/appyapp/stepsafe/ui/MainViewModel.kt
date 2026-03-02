package uk.appyapp.stepsafe.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.os.Build
import android.telephony.SmsManager
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.appyapp.stepsafe.data.local.dao.CaregiverContactDao
import uk.appyapp.stepsafe.data.local.dao.HomeLocationDao
import uk.appyapp.stepsafe.data.local.entities.CaregiverContact
import uk.appyapp.stepsafe.data.local.entities.HomeLocation
import uk.appyapp.stepsafe.data.repository.SettingsRepository
import uk.appyapp.stepsafe.service.GeofencingService
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val homeLocationDao: HomeLocationDao,
    private val caregiverContactDao: CaregiverContactDao,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    val homeLocation: StateFlow<HomeLocation?> = homeLocationDao.getHomeLocation()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val caregiverContact: StateFlow<CaregiverContact?> = caregiverContactDao.getCaregiverContact()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        
    val safeZoneRadius: StateFlow<Double> = settingsRepository.safeZoneRadius
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100.0)

    val monitoringActive: StateFlow<Boolean> = settingsRepository.monitoringActive
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isGeocoding = mutableStateOf(false)
    val isGeocoding: State<Boolean> = _isGeocoding

    private val _geocodingError = mutableStateOf<String?>(null)
    val geocodingError: State<String?> = _geocodingError

    private val _isRegisteringGeofence = mutableStateOf(false)
    val isRegisteringGeofence: State<Boolean> = _isRegisteringGeofence

    private val _geofenceResult = mutableStateOf<String?>(null)
    val geofenceResult: State<String?> = _geofenceResult

    fun clearGeocodingError() {
        _geocodingError.value = null
    }

    fun clearGeofenceResult() {
        _geofenceResult.value = null
    }

    fun startMonitoring() {
        viewModelScope.launch {
            _isRegisteringGeofence.value = true
            try {
                startServiceInternal()
                settingsRepository.updateMonitoringActive(true)
                delay(800)
                _geofenceResult.value = "Monitoring started successfully"
            } catch (e: Exception) {
                _geofenceResult.value = "Failed to start monitoring"
            } finally {
                _isRegisteringGeofence.value = false
            }
        }
    }

    fun updateGeofenceSettings() {
        viewModelScope.launch {
            _isRegisteringGeofence.value = true
            try {
                // Decoupled: only saves parameters to local state/DB (already persistent via radius/location updates)
                // This confirms settings are saved. Does NOT start the service.
                delay(800)
                _geofenceResult.value = "Settings saved"
            } catch (e: Exception) {
                _geofenceResult.value = "Failed to save settings"
            } finally {
                _isRegisteringGeofence.value = false
            }
        }
    }

    private fun startServiceInternal() {
        val intent = Intent(getApplication(), GeofencingService::class.java)
        getApplication<Application>().startForegroundService(intent)
    }

    fun stopGeofencing() {
        viewModelScope.launch {
            val intent = Intent(getApplication(), GeofencingService::class.java)
            getApplication<Application>().stopService(intent)
            settingsRepository.updateMonitoringActive(false)
        }
    }
    
    fun updateHomeLocation(latitude: Double, longitude: Double, address: String) {
        viewModelScope.launch {
            homeLocationDao.insertHomeLocation(HomeLocation(latitude = latitude, longitude = longitude, address = address))
        }
    }

    fun searchPostcode(postcode: String) {
        if (postcode.isBlank()) return
        
        viewModelScope.launch {
            _isGeocoding.value = true
            _geocodingError.value = null
            
            try {
                val geocoder = Geocoder(getApplication(), Locale.getDefault())
                val addresses = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(postcode, 1)
                }

                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val fullAddress = address.getAddressLine(0) ?: postcode
                    updateHomeLocation(address.latitude, address.longitude, fullAddress)
                } else {
                    _geocodingError.value = "Postcode not found."
                }
            } catch (e: Exception) {
                _geocodingError.value = "Network error or Geocoder unavailable."
            } finally {
                _isGeocoding.value = false
            }
        }
    }

    fun updateLocationFromMap(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _isGeocoding.value = true
            try {
                val geocoder = Geocoder(getApplication(), Locale.getDefault())
                val addresses = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)
                }

                val addressName = if (!addresses.isNullOrEmpty()) {
                    addresses[0].getAddressLine(0) ?: "Selected Location"
                } else {
                    "Selected Location"
                }
                updateHomeLocation(latitude, longitude, addressName)
            } catch (e: Exception) {
                updateHomeLocation(latitude, longitude, String.format(Locale.getDefault(), "%.4f, %.4f", latitude, longitude))
            } finally {
                _isGeocoding.value = false
            }
        }
    }
    
    fun updateCaregiverContact(name: String, phoneNumber: String) {
        viewModelScope.launch {
            caregiverContactDao.insertCaregiverContact(CaregiverContact(name = name, phoneNumber = phoneNumber))
        }
    }
    
    fun updateSafeZoneRadius(radius: Double) {
        viewModelScope.launch {
            settingsRepository.updateSafeZoneRadius(radius)
        }
    }

    fun sendEmergencySms() {
        viewModelScope.launch {
            val contact = caregiverContact.value ?: return@launch
            val location = homeLocation.value
            
            val message = "StepSafe Alert: ${contact.name}, the user has left their safe zone. View location: https://www.google.com/maps/search/?api=1&query=${location?.latitude ?: 0.0},${location?.longitude ?: 0.0}"
            
            try {
                val smsManager =
                    getApplication<Application>().getSystemService(SmsManager::class.java)
                smsManager.sendTextMessage(contact.phoneNumber, null, message, null, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun saveTestData() {
        homeLocationDao.insertHomeLocation(HomeLocation(latitude = 51.5074, longitude = -0.1278, address = "London Eye"))
        caregiverContactDao.insertCaregiverContact(CaregiverContact(name = "Caregiver", phoneNumber = "5550101"))
    }
}
