# DeviceFaker (LSPosed)

Deep, per-app device spoof: makes a TARGET app see a real Google **Pixel 6
(oriole / Android 13)** instead of this device.

Hooks in the target app's process:
- `android.os.Build.*` static fields + `Build.VERSION.{RELEASE,SECURITY_PATCH,INCREMENTAL}` (SDK_INT left real)
- `SystemProperties.get(ro.*)` (props read directly)
- `Settings.Secure` ANDROID_ID
- `TelephonyManager` IMEI/IMSI/ICCID/line1
- `WifiInfo` MAC/BSSID
- `Build.getSerial()`

Per-device identifiers (IMEI valid-Luhn, ANDROID_ID, serial, IMSI, ICCID, phone,
MAC) are generated + persisted by ConfigActivity and read by the hook via
XSharedPreferences (so they're stable per identity, regenerate with the button).

## Use
1. Install, **enable in LSPosed**, set **Scope = target app(s)** — NEVER
   `com.google.android.gms` (would inject into DroidGuard -> Play Integrity).
2. Open config (sets/saves identifiers, world-readable):
   `adb shell am start -n com.qundev.devicefaker/.ConfigActivity`
   - "Randomize identifiers" = new IMEI/ANDROID_ID/... ; "Toggle" = on/off.
3. **Force-stop the target app**, reopen -> it sees the Pixel 6.

Build: `JAVA_HOME=~/Applications/android-studio/jbr ./gradlew :app:assembleDebug`
then `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

Change the spoofed model: edit the `P_*` constants in `DeviceHook.java`
(keep the fingerprint = brand/product/device:release/id/incremental:type/tags).
Keep SDK/release coherent with the chosen Pixel (Pixel 6 = Android 13 = SDK 33).
