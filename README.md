# CyberGuard Android

A lightweight Wi-Fi awareness helper for Android 6.0+ (tested build target: Android 11 / API 30 devices).

## Features
- Scan nearby Wi-Fi access points and view SSID, signal strength, frequency, and advertised security type.
- View the current Wi-Fi connection's reported signal, link speed, frequency, IP, gateway, subnet, and DNS where Android exposes them.
- Show a local notification when a scan observes a new access point during the current app session.
- Simple dark interface designed for low-to-mid-range Android phones.

## Build
- JDK 17
- Gradle 8.9
- Android Gradle Plugin 8.7.3
- Run: `gradle assembleDebug`
- APK: `app/build/outputs/apk/debug/app-debug.apk`

GitHub Actions builds a debug APK on pushes to `main` and on manual workflow dispatch. Download it from the run's Artifacts section.

## Android permissions and limitations
- Nearby Wi-Fi scan results on Android 11 require Location permission and Location services enabled.
- Android may throttle Wi-Fi scans; results can be cached or unavailable between permitted scans.
- Some connection details are redacted by Android depending on OS version and permission state.
- Android does not provide ordinary third-party apps with a reliable universal way to enumerate every device connected to a router or the phone's hotspot. Hotspot-client monitoring is not included.
- This is a network-awareness utility, not antivirus, a penetration-testing tool, or a guarantee that a network is safe.
