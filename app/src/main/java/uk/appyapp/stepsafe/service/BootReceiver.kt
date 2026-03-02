package uk.appyapp.stepsafe.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import uk.appyapp.stepsafe.data.repository.SettingsRepository
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        // super.onReceive(context, intent) is called automatically by the Hilt-generated class
        handleBoot(context, intent)
    }

    fun handleBoot(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d(TAG, "Boot completed detected")
            
            scope.launch {
                val isMonitoringActive = settingsRepository.monitoringActive.first()
                if (isMonitoringActive) {
                    Log.d(TAG, "Restarting GeofencingService after reboot")
                    val serviceIntent = Intent(context, GeofencingService::class.java)
                    ContextCompat.startForegroundService(context, serviceIntent)
                } else {
                    Log.d(TAG, "Monitoring was not active, skipping service restart")
                }
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
