# Anasa

A free, offline Android app that puts a short breath between you and the apps you open on
autopilot. Open a guarded app → a breathing pause appears → choose **Not now** or **Open**.

- No accounts, no ads, no analytics, **no internet permission**. All data stays on the device.
- Kotlin · Jetpack Compose · Material 3 Expressive · Room · DataStore. minSdk 26, targetSdk 37.
- Spec: [FEATURES.md](FEATURES.md).

## Build

Requirements: JDK 17+ and the Android SDK (platform `android-37.0`, build-tools 37).

```bash
echo "sdk.dir=$ANDROID_HOME" > local.properties   # or open the project in Android Studio
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/app-debug.apk`. Unit tests: `./gradlew testDebugUnitTest`.

## Sideload

1. Copy `app-debug.apk` to the phone and open it (allow "Install unknown apps" for your file
   manager when asked) — or with USB debugging on: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
2. Open Anasa and follow onboarding: pick apps, then enable the **Anasa** Accessibility service.
3. **Android 13+:** sideloaded apps can't enable Accessibility until you allow it. If the switch is
   greyed out ("Restricted setting"): Settings → Apps → Anasa → ⋮ → **Allow restricted settings**,
   then enable the service again.
4. On Xiaomi/Huawei/Oppo/Vivo/Samsung, set Anasa's battery usage to *Unrestricted* so the system
   doesn't kill the service.

## Code map

| Path | What |
|---|---|
| `service/AnasaAccessibilityService.kt` | Listens for foreground-app changes (window-state events only). |
| `service/InterceptionPolicy.kt` | Pure trigger rules: grace windows, debounce, same-app changes. Unit-tested. |
| `service/PauseCoordinator.kt` | Session lifecycle, event logging, per-app config resolution. |
| `pause/` | Pause Activity, breathing shape (morphing `graphics-shapes`), haptics. |
| `data/` | Room (events, per-app config), DataStore (global settings), stats/streak logic. |
| `ui/` | Onboarding, Today, Stats, Apps, Settings screens. |
