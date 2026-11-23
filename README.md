# CyberGuard Android

CyberGuard is a lightweight, defensive Wi-Fi awareness dashboard for Android phones.

## Current features
- Premium dark security-console interface with custom CyberGuard launcher icon.
- Home dashboard and separate Wi-Fi, Network, Security Check, Activity Logs, and About pages.
- Nearby Wi-Fi scan showing SSID, advertised security, signal strength, and frequency.
- Current connection overview with IP, gateway, subnet, DNS, RSSI, frequency, and link speed where Android exposes them.
- New access-point notifications during the current app session.
- Security guidance and permission checks.
- Designed to remain compatible with Android 11 devices and low-to-mid-range phones.

## Build
- JDK 17, Gradle 8.9, Android Gradle Plugin 8.7.3
- Run: `gradle assembleDebug`
- APK: `app/build/outputs/apk/debug/app-debug.apk`
- GitHub Actions builds a debug APK on pushes to `main`; download it from the workflow run Artifacts section.

## Android limitations
- Android 11 requires Location permission and Location services for nearby Wi-Fi scan results.
- Android can throttle scans; results may be cached or unavailable.
- Some connection details are restricted by Android and device vendors.
- Ordinary third-party apps cannot reliably list every router or hotspot client on all devices.
- Scan history is session-only in this release and clears when the app process ends.
- This app is a network-awareness helper, not antivirus, a penetration-testing tool, or a guarantee that a network is safe.

## Safe use
Use the app only to inspect your own device and networks you are authorized to manage. CyberGuard does not attempt to access, disrupt, or exploit other devices or networks.
