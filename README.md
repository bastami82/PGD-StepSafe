# StepSafe — Comprehensive Technical Implementation Report

## Executive summary

StepSafe is an assistive Android application that helps people in the early stages of Alzheimer's disease by providing active cognitive assistance. Instead of only passively tracking, StepSafe detects when a user leaves a configured "Safe Zone" and immediately triggers an emergency flow: a foreground alert for the patient (lock‑screen bypass + TTS) and an emergency SMS to a caregiver.

This repository contains a Kotlin/Jetpack Compose implementation using modern Android architecture and best practices (Room, DataStore, Dagger Hilt, Coroutines, StateFlow).

---

## Table of contents

- [Architecture](#architecture)
- [User interface & navigation](#user-interface--navigation)
  - [Main dashboard (MainActivity)](#main-dashboard-mainactivity)
  - [Alert interface (AlertActivity)](#alert-interface-alertactivity)
- [Core services and background processing](#core-services-and-background-processing)
  - [GeofencingService (foreground location service)](#geofencingservice-foreground-location-service)
  - [GeofenceBroadcastReceiver (trigger handler)](#geofencebroadcastreceiver-trigger-handler)
  - [BootReceiver (reboot persistence)](#bootreceiver-reboot-persistence)
- [Data persistence](#data-persistence)
- [Security, permissions & UX](#security-permissions--ux)
- [Testing & quality assurance](#testing--quality-assurance)
- [How to build & run (quick)](#how-to-build--run-quick)
- [Demo videos](#demo-videos)
- [Screenshots — App flow](#screenshots--app-flow)
- [Diagrams](#diagrams)
- [License & contact](#license--contact)

---

## Architecture

StepSafe follows MVVM with Clean Architecture principles:

- UI layer: Jetpack Compose, driven by StateFlow exposed by ViewModels.
- Presentation (ViewModel) layer: business logic and bridging UI <-> data.
- Data layer: Room for structured relational data + DataStore for lightweight preferences.
- DI: Dagger Hilt for providing database instances, DAOs, repositories, and services.

This separation keeps code testable and modular.

---

## User interface & navigation

### Main dashboard (MainActivity)

The dashboard is intended for caregivers to configure the app:

- Map preview via `maps-compose` (Google Maps) to set home location by tapping or postcode lookup.
- Safe zone radius slider (50m–500m).
- A "Set Geofence" action which debounces repeated clicks by using an `isRegisteringGeofence` state. When registering, the button shows a spinner and is disabled.

Example snippet (Compose pseudocode):

```kotlin
Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onSetGeofence() },
       enabled = !isRegisteringGeofence) {
    if (isRegisteringGeofence) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
        Text("Saving...", fontWeight = FontWeight.Bold)
    } else {
        Text("Set Geofence", fontWeight = FontWeight.Bold)
    }
}
```

### Alert interface (AlertActivity)

When a geofence EXIT is detected, `AlertActivity` is launched using a full-screen intent so it can wake the device and bypass the lock screen. The UI is intentionally minimal: a single prominent action ("Navigate Home") and optional TTS prompting to reduce cognitive load for the patient.

---

## Core services and background processing

Android 14+ places stricter limits on background work. To keep geofencing reliable we use a foreground service and follow OS guidance for long‑running location tasks.

### GeofencingService (foreground location service)

- Registers geofences with Google Location Services API.
- Runs as a foreground service with a non-dismissible notification of type `FOREGROUND_SERVICE_TYPE_LOCATION` on newer Android releases.

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
}
```

### GeofenceBroadcastReceiver (trigger handler)

- Receives `GEOFENCE_TRANSITION_EXIT` events and acts as the emergency trigger.
- Uses Coroutines (`Dispatchers.IO`) to query the `CaregiverContactDao` and send an SMS via `SmsManager`.
- Launches `AlertActivity` and posts a high priority notification.
- We persist an `ExitEvent` to Room on exit (so the database contains clear pre/post evidence rows for Appendix B).

### BootReceiver (reboot persistence)

- On device boot, the receiver checks DataStore to determine if monitoring was active before reboot and re-schedules the `GeofencingService` if needed.

---

## Data persistence

- Room (`AppDatabase`) stores structured entities:
  - `HomeLocation` — latitude, longitude, address.
  - `CaregiverContact` — name, phone number.
  - `ExitEvent` — timestamp, lat/lng, eventType, optional note (added to support Appendix evidence exports).

- Jetpack DataStore (`SettingsRepository`) stores preferences such as `safeZoneRadius` and `monitoringActive` as Flows.

---

## Security, permissions & UX

- Permission sequencing is managed via an Accompanist `PermissionProvider` composable.
- We request Fine/Coarse Location and SMS permissions, and (with a clear explanation) request Background Location when required so geofences work reliably.
- Foreground service notification explains why location is needed and provides a quick way for a caregiver to stop monitoring.

---

## Testing & quality assurance

- Unit testing stack: JUnit 4, MockK, Turbine, and Robolectric.
- Tests include:
  - ViewModel tests verifying state transitions.
  - DAO tests using in-memory Room databases.
  - BootReceiver tests ensuring reboot persistence behaviour.

---

## How to build & run (quick)

From the project root:

```bash
# Build the debug APK
./gradlew :app:assembleDebug

# Install to connected device (adb must be authorized)
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Notes:
- For DB exports and Appendix evidence capture I included a small script under `scripts/collect_appendix_b.sh` that automates logcat capture, screenshots and (when possible) pulls the app's Room DB via `run-as`.
- For reliable DB pulls, install the debug APK (so `run-as` works). The script can also export `exit_event` rows to CSV if `sqlite3` is installed on your host machine.

---

## Demo videos

Demo videos demonstrating StepSafe in action are included in the `projectFile/` directory. You can either open the demo HTML in your browser which embeds both videos, or play the MP4s directly with your OS video player.

**Autoplay in browser:**

- [StepSafe — Starting app, granting permission, setting up home, and starting monitoring](projectFile/StepSafe-in-action-starting-app-granting-permission-setting-up-home-and-start-monitoring.mp4)
  <br><video src="projectFile/StepSafe-in-action-starting-app-granting-permission-setting-up-home-and-start-monitoring.mp4" controls autoplay muted playsinline style="max-width:300px;aspect-ratio:9/19;border:1px solid #ccc;border-radius:24px;box-shadow:0 2px 8px #0002;"></video>

- [StepSafe — Stepping outside geofencing area](projectFile/StepSafe-in-action-when-stepping-outside-of-geofencing-area.mp4)
  <br><video src="projectFile/StepSafe-in-action-when-stepping-outside-of-geofencing-area.mp4" controls autoplay muted playsinline style="max-width:300px;aspect-ratio:9/19;border:1px solid #ccc;border-radius:24px;box-shadow:0 2px 8px #0002;"></video>

**Direct download (if browser playback fails):**

- [Download StepSafe — Starting app, granting permission, setting up home, and starting monitoring (MP4)](projectFile/StepSafe-in-action-starting-app-granting-permission-setting-up-home-and-start-monitoring.mp4)
- [Download StepSafe — Stepping outside geofencing area (MP4)](projectFile/StepSafe-in-action-when-stepping-outside-of-geofencing-area.mp4)

**Open the demo page in your browser:**

- [StepSafe Demo Page (demo.html)](projectFile/demo.html)

If your browser cannot play the videos above, download the MP4 files directly and play them locally with your preferred video player.

---

## Screenshots — App flow

Below is an ordered sequence of screenshots from `project-screenshots/` that follow the app's runtime flow (install/permission → configure home → start monitoring → background monitoring → exit alert → caregiver notification → navigation):

1. App installed on home screen

   ![App installed on home screen](./project-screenshots/StepSafe-app-icon-installed-homescreen.png)

2. First-run: notification permission prompt

   ![Request - show notifications](./project-screenshots/StepSafe-first-install-info-requesting-for-showing-app-notification.png)

3. First-run: device location permission prompt

   ![Request - device location](./project-screenshots/StepSafe-first-install-info-requesting-for-device-location.png)

4. First-run: background location information / request

   ![Background location info/request](./project-screenshots/StepSafe-first-install-info-background-location-permission.png)

5. First-run: SMS permission prompt

   ![Request - SMS permission](./project-screenshots/StepSafe-first-install-info-requesting-for-SMS-permission.png)

6. First-run: explanation about sending SMS in background

   ![SMS background requirement explanation](./project-screenshots/StepSafe-first-install-info-for-requiremnt-sending-SMS-in-background.png)

7. Main dashboard (caregiver/home screen)

   ![Main dashboard - caregiver home screen](./project-screenshots/StepSafe-mainscreen-care-giver-home-screen.png)

8. Setting home location (map / select location)

   ![Setting home location](./project-screenshots/StepSafe-mainscreen-setting-home-screen.png)

9. Increase / adjust geofencing radius

   ![Increase geofencing radius](./project-screenshots/StepSafe-mainscreen-increase-geofencing-radius-home-screen.png)

10. Saving geofence (in-progress)

   ![Saving geofence](./project-screenshots/StepSafe-mainscreen-saving-geoFencing.png)

11. Geofence saved (confirmation)

   ![Geofence saved](./project-screenshots/StepSafe-mainscreen-saved-geoFencing.png)

12. Start monitoring (foreground service)

   ![Start monitoring](./project-screenshots/StepSafe-mainscreen-start-monitoring-home-screen.png)

13. Foreground notification shown when app sent to background

   ![Foreground notification when app backgrounded](./project-screenshots/StepSafe-stepsafe-notification-when-main-app-sent-to-background.png)

14. Background service continues running after app closed

   ![Background service running after app closed](./project-screenshots/StepSafe-stepsafe-background-service-runs-even-after-app-closed1.png)

15. User steps outside the geofence — alert screen shown on device

   ![Alert screen on leaving geofence](./project-screenshots/StepSafe-navigate-home-alert-screen-when-step-outside-geofencing.png)

16. Caregiver receives SMS message

   ![Caregiver message received](./project-screenshots/StepSafe-messge-recieved-by-caregiver.png)

17. App notification launches Google Navigation

   ![App notification launching navigation](./project-screenshots/StepSafe-Screenshot_app_notification-GoogleNavigation.png)

18. Google Maps — navigation started (walk mode)

   ![Google Maps navigation started](./project-screenshots/StepSafe-GoogleNavigation-started-in-walk-mode-with-navigation-to-home.png)

19. Google Maps — zoomed-in navigation view

   ![Google Maps navigation zoomed](./project-screenshots/StepSafe-GoogleNavigation-started-in-walk-mode-with-navigation-to-home-zoom-in.png)

20. After pressing "Navigate Home" - navigation launches (alternative view)

   ![After pressing Navigate Home launches Google Navigation](./project-screenshots/StepSafe--after-pressing-navigate-home-button-lunches-GoogleNavigation-walk.png)

---

## Diagrams

Architecture and flow diagrams are included in `projectFile/`.

- System Architecture — Component Diagram

  ![System Architecture — Component Diagram](./projectFile/system-architecture-component-diagram.png)

---

- Caregiver Configuration Flow — Sequence Diagram

  ![Caregiver Configuration Flow Sequence Diagram](./projectFile/caregiver-configuration-flow-sequence-diagram.png)

---

- Emergency Geofence Trigger — Flowchart

  ![Emergency Geofence Trigger Flowchart](./projectFile/emergency-geofence-trigger-flowchart.png)

---

- Reboot Persistence Logic — Flowchart

  ![Reboot Persistence Logic Flowchart](./projectFile/reboot-persistence-logic-flowchart.png)

---

## License & contact

This project is provided for academic/educational purposes. For questions or to request additional exports or build assistance, please reply in this issue or contact the repository owner.

## How to Install StepSafe APK

You can install the StepSafe app directly on any compatible Android device (**Android 13, API 33 and above**; this app targets Android 13/14, API 33/36). The APK is provided for direct installation:

**Download APK:** [StepSafe.apk](installApp/StepSafe.apk)

### Installation Steps

1. **Download the APK**
   - Download the APK file from the link above to your Android device (or transfer it via USB, email, or cloud storage).

2. **Enable Installation from Unknown Sources**
   - Go to your device's **Settings** > **Security** (or **Apps & notifications** > **Special app access** > **Install unknown apps**).
   - Find your browser or file manager app and enable **Allow from this source**.
   - On some devices, you may be prompted to allow installation when you open the APK file.

3. **Install the APK**
   - Open your file manager and locate the downloaded `StepSafe.apk` file.
   - Tap the file and follow the prompts to install.
   - If prompted, confirm installation and accept any warnings about unknown sources.

4. **Launch the App**
   - Once installed, you can find StepSafe in your app drawer. Open it and grant any requested permissions (location, SMS, notifications) for full functionality.

**Note:**
- This APK is for testing and demonstration purposes. For production use, install only from trusted sources (e.g., Google Play Store).
- If you encounter issues, ensure your device meets the minimum SDK requirement (**Android 13, API 33+**).

---

## Minimum Supported OS

StepSafe requires **Android 13 (API level 33) or higher**. Devices running older versions of Android are not supported. This is enforced by the `minSdk = 33` setting in the build configuration.
