package uk.appyapp.stepsafe

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationPersistenceTest {

    private lateinit var device: UiDevice
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        
        // Ensure device is awake and at home screen
        device.wakeUp()
        device.pressHome()
        
        val launcherPackage: String = device.launcherPackageName
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), 5000)
        
        // Grant all required permissions via shell to ensure test runs without interruption
        val permissions = arrayOf(
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_BACKGROUND_LOCATION",
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.SEND_SMS"
        )
        
        permissions.forEach { permission ->
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                "pm grant uk.appyapp.stepsafe $permission"
            )
        }
        
        // Launch the StepSafe app
        val intent = context.packageManager.getLaunchIntentForPackage("uk.appyapp.stepsafe")?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
        
        // Wait for app to reach the foreground
        device.wait(Until.hasObject(By.pkg("uk.appyapp.stepsafe").depth(0)), 10000)
    }

    @Test
    fun testNotificationIsPersistentAndNonDismissible() {
        // 1. Find and click "Start Monitoring"
        // Since the UI is scrollable, we use UiScrollable to ensure the button is in view
        val scrollable = UiScrollable(UiSelector().scrollable(true))
        
        // Ensure we are in a clean state (stop monitoring if active)
        try {
            val stopBtn = device.findObject(By.text("Stop Monitoring"))
            if (stopBtn != null) {
                stopBtn.click()
                device.waitForIdle(2000)
            }
        } catch (e: Exception) {}

        scrollable.scrollTextIntoView("Start Monitoring")
        val startButton = device.findObject(By.text("Start Monitoring"))
        assertNotNull("Start Monitoring button not found", startButton)
        startButton.click()
        
        // 2. The app should move to background automatically after starting monitoring
        // Verify we returned to the home screen/launcher
        device.wait(Until.hasObject(By.pkg(device.launcherPackageName).depth(0)), 10000)
        
        // 3. Open the notification shade to verify the foreground service notification
        device.openNotification()
        
        // 4. Verify notification presence and correct StepSafe branding
        val notificationText = "Safe Zone monitoring is active"
        val notification = device.wait(Until.findObject(By.textContains(notificationText)), 10000)
        assertNotNull("Persistent notification not found in shade", notification)
        
        val appTitle = device.findObject(By.text("StepSafe"))
        assertNotNull("Notification app title 'StepSafe' not found", appTitle)
        
        // 5. Attempt to swipe the notification away (simulating a dismissal attempt)
        // We swipe from left to right across the notification's horizontal center
        val bounds = notification.visibleBounds
        val startX = bounds.left + 5
        val endX = bounds.right - 5
        val centerY = bounds.centerY()
        
        device.swipe(startX, centerY, endX, centerY, 25) // deliberate swipe attempt
        device.waitForIdle(2000)
        
        // 6. Verify notification STILL exists after swipe attempt (confirming setOngoing(true) and FLAG_NO_CLEAR)
        val notificationAfterSwipe = device.findObject(By.textContains(notificationText))
        assertNotNull("Notification was dismissed! It should be non-dismissible (ongoing).", notificationAfterSwipe)
        
        // 7. Tap notification to verify it brings the app back to foreground
        notificationAfterSwipe.click()
        
        // 8. Verify app is in foreground and showing the active monitoring state
        device.wait(Until.hasObject(By.pkg("uk.appyapp.stepsafe").depth(0)), 10000)
        
        try {
            scrollable.scrollTextIntoView("Stop Monitoring")
        } catch (e: Exception) {}
        
        val stopButton = device.findObject(By.text("Stop Monitoring"))
        assertNotNull("App did not return to foreground or Stop Monitoring button missing", stopButton)
        
        // 9. Stop monitoring
        stopButton.click()
        
        // 10. Verify notification is removed after stopping monitoring
        device.openNotification()
        val isGone = device.wait(Until.gone(By.textContains(notificationText)), 5000)
        assertTrue("Notification should be removed after stopping monitoring", isGone)
        
        device.pressHome()
    }
    
    private fun assertTrue(message: String, condition: Boolean) {
        if (!condition) org.junit.Assert.fail(message)
    }
}
