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

## Appendix: Evidence export

- `scripts/collect_appendix_b.sh` — automated evidence collection for Appendix B (logcat, screenshots, DB pull + CSV export of `exit_event`).
- I also added a debug helper UI to insert a test `ExitEvent` (use the debug build) for quick offline validation.

---

## License & contact

This project is provided for academic/educational purposes. For questions or to request additional exports or build assistance, please reply in this issue or contact the repository owner.
