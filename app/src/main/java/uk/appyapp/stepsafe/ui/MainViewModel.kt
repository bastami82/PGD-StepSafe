package uk.appyapp.stepsafe.ui

import android.app.Application
import android.content.Intent
import android.location.Geocoder
import android.telephony.SmsManager
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.appyapp.stepsafe.data.local.dao.CaregiverContactDao
import uk.appyapp.stepsafe.data.local.dao.ExitEventDao
import uk.appyapp.stepsafe.data.local.dao.HomeLocationDao
import uk.appyapp.stepsafe.data.local.entities.CaregiverContact
import uk.appyapp.stepsafe.data.local.entities.ExitEvent
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
    private val exitEventDao: ExitEventDao,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    private val _homeLocation = MutableStateFlow<HomeLocation?>(null)
    val homeLocation: StateFlow<HomeLocation?> = _homeLocation

    private val _caregiverContact = MutableStateFlow<CaregiverContact?>(null)
    val caregiverContact: StateFlow<CaregiverContact?> = _caregiverContact

    private val _safeZoneRadius = MutableStateFlow<Double>(100.0)
    val safeZoneRadius: StateFlow<Double> = _safeZoneRadius

    private val _monitoringActive = MutableStateFlow<Boolean>(false)
    val monitoringActive: StateFlow<Boolean> = _monitoringActive

    private val _isGeocoding = mutableStateOf(false)
    val isGeocoding: State<Boolean> = _isGeocoding

    private val _geocodingError = mutableStateOf<String?>(null)
    val geocodingError: State<String?> = _geocodingError

    private val _isRegisteringGeofence = mutableStateOf(false)
    val isRegisteringGeofence: State<Boolean> = _isRegisteringGeofence

    private val _geofenceResult = mutableStateOf<String?>(null)
    val geofenceResult: State<String?> = _geofenceResult

    init {
        viewModelScope.launch {
            homeLocationDao.getHomeLocation().collect { location ->
                _homeLocation.value = location
            }
        }

        viewModelScope.launch {
            caregiverContactDao.getCaregiverContact().collect { contact ->
                _caregiverContact.value = contact
            }
        }

        viewModelScope.launch {
            settingsRepository.safeZoneRadius.collect { radius ->
                _safeZoneRadius.value = radius
            }
        }

        viewModelScope.launch {
            settingsRepository.monitoringActive.collect { active ->
                _monitoringActive.value = active
            }
        }
    }

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

    fun insertSampleExitEvent() {
        viewModelScope.launch {
            try {
                val loc = homeLocation.value
                val lat = loc?.latitude ?: 51.5074
                val lng = loc?.longitude ?: -0.1278
                val event = ExitEvent(
                    timestamp = System.currentTimeMillis(),
                    latitude = lat,
                    longitude = lng,
                    eventType = "TEST_EXIT",
                    note = "Inserted via debug UI"
                )
                exitEventDao.insert(event)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
