package uk.appyapp.stepsafe.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import uk.appyapp.stepsafe.data.local.dao.HomeLocationDao
import uk.appyapp.stepsafe.data.local.entities.HomeLocation
import uk.appyapp.stepsafe.data.repository.SettingsRepository
import javax.inject.Inject

@HiltViewModel
class AlertViewModel @Inject constructor(
    private val homeLocationDao: HomeLocationDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val homeLocation: StateFlow<HomeLocation?> = homeLocationDao.getHomeLocation()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val voicePromptEnabled: StateFlow<Boolean> = settingsRepository.voicePromptEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
}
