# SIAGA

**SIAGA** is an Android application for real-time **gas and air quality monitoring**. It connects to IoT hardware (e.g., NodeMCU with gas sensors), receives live data via MQTT, displays readings on a chart, and alerts you when gas levels exceed a configurable threshold.

---

## Features

- **Real-time gas monitoring** – Live gas level updates via MQTT (HiveMQ)
- **Line chart** – Recent gas readings with lowest/highest values
- **Alerts** – Notifications when gas levels exceed your set threshold
- **Product ID** – Link the app to your device by validating and storing a product ID from [siaga.site](https://siaga.site)
- **Settings** – Adjust max gas threshold, change product ID, and toggle vibration for alerts
- **WiFi configuration** – Configure the NodeMCU’s WiFi (SSID/password) when connected to the device’s hotspot
- **Foreground service** – Continuous monitoring with a persistent “SIAGA Gas Detector” notification

---

## Requirements

- **Android**: minSdk 31, targetSdk 34
- **Network**: Internet (API + MQTT) and optionally WiFi for device setup
- **Permissions**: Notifications, vibration, network state, WiFi state, location (for WiFi scan), foreground service

---

## Project structure

```
SIAGA/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/siaga/
│   │   │   ├── SplashScreen.java      # Entry: validates product ID, then Main or ProductIdInput
│   │   │   ├── ProductIdInput.java    # Enter/validate product ID via API
│   │   │   ├── MainActivity.java      # Dashboard: MQTT gas data, chart, alerts, settings
│   │   │   ├── Settings.java          # Threshold, product ID, vibration, WiFi settings
│   │   │   ├── WifiSetting.java       # NodeMCU WiFi config (scan + send SSID/password)
│   │   │   ├── NotificationService.java  # Foreground service for monitoring
│   │   │   ├── NotificationActivity.java # Notification UI
│   │   │   └── HiveMqttManager.java   # MQTT client (HiveMQ)
│   │   ├── res/                        # Layouts, drawables, values, themes
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
└── README.md
```

---

## Build and run

1. **Clone and open** the project in Android Studio (or use Gradle from the command line).
2. **Sync** Gradle (e.g. “Sync Project with Gradle Files”).
3. **Build**:
   ```bash
   ./gradlew assembleDebug
   ```
4. **Install** on a device or emulator:
   ```bash
   ./gradlew installDebug
   ```
   Or run from Android Studio.

---

## Configuration

### Product ID

- On first launch (or if none is saved), you’re asked for a **Product ID**.
- It is validated against `https://siaga.site/api/apps/{productId}`.
- A valid ID is stored in `SharedPreferences` and used for device association.

### MQTT (HiveMQ)

- The app connects to a **HiveMQ Cloud** broker (host/port/credentials are in `MainActivity`).
- Subscribed topics:
  - **`gas_level`** – JSON with `gas_level` (integer); drives the chart and alerts.
  - **`device_status`** – Device status messages (shown as toasts when they change).

Configure broker URL, port, username, and password in `MainActivity.initMqtt()` (consider moving to config/build config for production).

### Alerts

- **Threshold**: Set in **Settings** (default 300). When gas level ≥ this value, a high-priority notification is shown.
- **Vibration**: In Settings you can enable vibration for notifications; the foreground service uses this preference for its notification behavior.

### WiFi (NodeMCU)

- **WifiSetting** talks to the NodeMCU at **`http://192.168.4.1`**:
  - **GET** `/scan` – list WiFi networks (when connected to the device’s “Siaga Device” hotspot).
  - **POST** `/save-wifi` – body `{"ssid":"...","password":"..."}` to save WiFi credentials on the device.

Ensure the phone is connected to the NodeMCU’s AP when using this screen.

---

## Dependencies (main)

- **AndroidX**: AppCompat, Material, ConstraintLayout, Activity, LocalBroadcastManager
- **OkHttp** – HTTP client for API and NodeMCU
- **Gson** – JSON parsing
- **HiveMQ MQTT Client** (`com.hivemq:hivemq-mqtt-client:1.3.0`) – MQTT
- **MPAndroidChart** – Line chart for gas levels

---

## License

See the repository for license information.
