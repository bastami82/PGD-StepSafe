package uk.appyapp.stepsafe.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import uk.appyapp.stepsafe.MainActivity
import uk.appyapp.stepsafe.R
import uk.appyapp.stepsafe.data.local.dao.HomeLocationDao
import uk.appyapp.stepsafe.data.repository.SettingsRepository
import javax.inject.Inject

@AndroidEntryPoint
class GeofencingService : Service() {

    @Inject
    lateinit var homeLocationDao: HomeLocationDao

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private lateinit var geofencingClient: GeofencingClient
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
    }

    override fun onCreate() {
        super.onCreate()
        geofencingClient = LocationServices.getGeofencingClient(this)
        createNotificationChannel()
        
        val notification = createNotification()
        
        // Android 14+ requirement: Specify foreground service type
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "StepSafe Geofencing",
            NotificationManager.IMPORTANCE_HIGH // HIGH importance for persistent presence
        )
        channel.description = "Persistent notification for StepSafe Safe Zone monitoring"
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val appName = getString(R.string.app_name)
        
        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(appName)
            .setContentText("Safe Zone monitoring is active")
            .setSmallIcon(R.mipmap.ic_launcher) // Branded launcher icon
            .setOngoing(true) // Crucial: non-dismissible via swipe
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_MAX) // Max priority for persistence
            .setContentIntent(pendingIntent) // Dashboard redirect
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        // Manually add flags to ensure it is ongoing and cannot be cleared by the system or user
        notification.flags = notification.flags or Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
        
        return notification
    }

    @SuppressLint("MissingPermission")
    private fun registerGeofence() {
        serviceScope.launch {
            val homeLocation = homeLocationDao.getHomeLocation().first()
            val radius = settingsRepository.safeZoneRadius.first()

            if (homeLocation != null) {
                val geofence = Geofence.Builder()
                    .setRequestId(GEOFENCE_ID)
                    .setCircularRegion(
                        homeLocation.latitude,
                        homeLocation.longitude,
                        radius.toFloat()
                    )
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build()

                val request = GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT)
                    .addGeofence(geofence)
                    .build()

                geofencingClient.addGeofences(request, geofencePendingIntent).run {
                    addOnSuccessListener {
                        Log.d(TAG, "Geofence added successfully at ${homeLocation.address} with radius $radius")
                    }
                    addOnFailureListener {
                        Log.e(TAG, "Failed to add geofence: ${it.message}")
                    }
                }
            } else {
                Log.w(TAG, "No home location set. Cannot register geofence.")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        registerGeofence()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        geofencingClient.removeGeofences(geofencePendingIntent)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "GeofencingService"
        private const val CHANNEL_ID = "geofencing_channel"
        private const val NOTIFICATION_ID = 1
        private const val GEOFENCE_ID = "SAFE_ZONE"
    }
}
