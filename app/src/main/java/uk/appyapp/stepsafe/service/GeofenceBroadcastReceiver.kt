package uk.appyapp.stepsafe.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import uk.appyapp.stepsafe.R
import uk.appyapp.stepsafe.data.local.dao.CaregiverContactDao
import uk.appyapp.stepsafe.ui.AlertActivity
import javax.inject.Inject

@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var caregiverContactDao: CaregiverContactDao

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, "Geofencing error: $errorMessage")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            Log.d(TAG, "Geofence Exit Detected!")
            val location = geofencingEvent.triggeringLocation
            
            showExitNotification(context)
            launchAlertActivity(context)
            sendEmergencySms(context, location)
        } else {
            Log.e(TAG, "Invalid transition type: $geofenceTransition")
        }
    }

    private fun launchAlertActivity(context: Context) {
        val intent = Intent(context, AlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }

    private fun sendEmergencySms(context: Context, location: Location?) {
        scope.launch {
            val contact = caregiverContactDao.getCaregiverContact().first() ?: return@launch
            val lat = location?.latitude ?: 0.0
            val lng = location?.longitude ?: 0.0
            
            val message = "StepSafe Alert: ${contact.name}, the user has left their safe zone. View location: https://www.google.com/maps/search/?api=1&query=$lat,$lng"
            
            try {
                val smsManager =
                    context.getSystemService(SmsManager::class.java)
                smsManager.sendTextMessage(contact.phoneNumber, null, message, null, null)
                Log.d(TAG, "SMS sent to ${contact.phoneNumber}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send SMS: ${e.message}")
            }
        }
    }

    private fun showExitNotification(context: Context) {
        val channelId = "geofence_alerts"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "StepSafe Alerts",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("StepSafe Alert!")
            .setContentText("You have left the Safe Zone. Do you need help navigating home?")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2, notification)
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
    }
}
