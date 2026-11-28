# RakshaKavach (रक्षाकवच) — Road Crash Detection & Emergency SOS

**RakshaKavach** (**रक्षाकवच**) is a privacy-first, on-device Android road-crash detection and emergency SOS safety application designed for Indian roads (supporting emergency helpline **112**, offline SMS fallback, and **English**, **Hindi (हिंदी)**, and **Marathi (मराठी)** localization).

---

## Key Features

### 1. Automatic & Manual Driving Trip Detection
- **Automatic Driving Mode**: Uses Android `ActivityRecognitionClient` (`IN_VEHICLE`) combined with a 30-second GPS speed confirmation ($\ge 10\text{ km/h}$) to start monitoring automatically without draining the battery when stationary.
- **Manual Driving Override**: Instant one-tap start/stop driving protection from the Home screen or Side Menu.
- **Driver / Passenger Role Selection**: Configurable trip role selector (`Driver`, `Passenger`, `Not Driving`).

### 2. Multi-Sensor Crash Detection Engine
- **9-Axis Sensor Fusion**: Continuously evaluates high-rate accelerometer (`TYPE_ACCELEROMETER`) and gyroscope (`TYPE_GYROSCOPE`) telemetry with a 2-second rolling gyro buffer.
- **Multi-Signal Confidence Scoring**:
  - Impact G-force threshold ($\ge 4.5\,g$ in Live Mode; configurable in Tester Mode)
  - Gyroscope angular velocity rollover detection ($\ge 3.5\text{ rad/s}$)
  - Rapid GPS speed drop ($\ge 10\text{ km/h}$ deceleration within 3 seconds)
  - Post-impact stillness detection
  - Screen touch interaction suppression
  - Car Bluetooth audio connectivity confidence boost
- **False-Positive Rejection**: Suppresses phone drops (free-fall preceding impact), isolated single-sample vibration spikes, and low-speed bumps.

### 3. 45-Second Full-Screen Crash Check-In (`CrashAlertActivity`)
- Wakes the screen over the lock screen at 100% brightness with looping alarm audio and heavy waveform vibration.
- Displays a 45-second countdown with three immediate actions:
  - **"I'm okay"** (cancels alert)
  - **"Need help"** (escalates immediately)
  - **"False alarm"** (cancels and logs false alarm for diagnostics)
- Automatically escalates to Emergency SOS if the user is unresponsive when the timer reaches `0s`.

### 4. Emergency SOS Escalation & Live Location Sharing
- **Multipart Emergency SMS**: Dispatches GPS coordinates, Google Maps link, timestamp, battery %, optional consented medical info, and emergency notes to up to 5 trusted contacts.
- **3x Automatic Retry & Fallback**: Uses `SmsManager` with `WorkManager` retry (`SmsRetryWorker`) and automatic SMS intent fallback if cellular delivery fails.
- **Live Location Updates**: Sends updated GPS coordinates every 20 seconds for 15 minutes (`IncidentService`), with a one-tap **Extend +15 min** control.
- **112 Emergency Helpline**: Prominent one-tap **CALL 112** button and optional 10-second auto-dial countdown.

### 5. Live Mode vs. Tester Mode
- **LIVE MODE**: Production road-safety mode with real hardware sensors, GPS speed gating, and clean driver-focused UI.
- **TESTER MODE**: Field-testing & QA mode featuring:
  - Live 9-axis hardware accelerometer/gyroscope telemetry (`Current G`, `Peak G`, `Gyro Peak`)
  - Armable **Physical Shake-to-Crash Hardware Test** ($1.8\,g$, $2.2\,g$, $3.0\,g$, $4.5\,g$)
  - Simulated driving trip bypass
  - Safe Test SOS alerts (`TEST ALERT from RakshaKavach — No emergency. Please ignore.`)
  - Synthetic crash scenario triggers (High-Speed Impact, Rollover, Phone Drop Rejection, Low-Speed Rejection)

### 6. Privacy-First & Hardware-Encrypted Medical Profile
- **100% On-Device Processing**: Sensor and GPS data never leave the device unless an emergency SOS is triggered.
- **Android Keystore AES-256-GCM Encryption**: Medical profile fields (blood group, allergies, medical conditions, medications, organ donor status, emergency notes) are encrypted at rest with per-field SMS sharing consent checkboxes.
- **Structured Incident Reports**: Export any trip or incident report as Plain Text or JSON via Android's native share sheet.

---

## Tech Stack
- **Language**: Kotlin 2.0
- **UI**: Jetpack Compose (Material 3) + Navigation Drawer (`ModalNavigationDrawer`)
- **DI**: Hilt (`Dagger-Hilt`)
- **Storage**: Room SQLite Database + Android Keystore (`AES/GCM/NoPadding`)
- **Background & Location**: Foreground Services (`location`, `specialUse`), Google Play Services `FusedLocationProviderClient` & `ActivityRecognitionClient`, `WorkManager`

---

## Build & Run

```bash
# Run unit tests and assemble debug APK
./gradlew testDebugUnitTest assembleDebug
```

The generated APK is output to `app/build/outputs/apk/debug/app-debug.apk`.
