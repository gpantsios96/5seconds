# Anasa — Feature Spec (v1)

Anasa puts a short, deliberate pause between you and the apps you open on autopilot.
When you launch a guarded app, Anasa shows a breathing screen first. After the breath
you either open the app or walk away. That's the whole idea.

All code, text and assets are original to this project.

---

## 1. Principles

- **100% free.** No paywall, no accounts, no ads.
- **Offline by construction.** The manifest does **not** declare `INTERNET`, so the
  app cannot make network calls. No analytics, crash reporting or third-party SDKs.
- **On-device data only.** Room for events, DataStore for settings. Uninstalling the
  app deletes everything.
- **Minimal permissions.** One special permission (Accessibility) plus `VIBRATE`.

---

## 2. Core interception

### 2.1 Guarded apps
- The user picks apps from a searchable list of installed, launchable apps (icon + label).
- Package visibility uses a `<queries>` block for `MAIN`/`LAUNCHER` intents, not
  `QUERY_ALL_PACKAGES`.
- Anasa, the launcher (home screen), System UI and Settings can never be guarded, so the
  user can't lock themselves out.

### 2.2 Detection: `AnasaAccessibilityService`
- Listens to `TYPE_WINDOW_STATE_CHANGED` only. No window content, no view IDs, no text.
  `canRetrieveWindowContent="false"`.
- The guarded list changes at runtime, so there is no static `packageNames` filter. The
  service compares `event.packageName` with an in-memory set kept up to date from
  DataStore.
- **Trigger rule.** A guarded package comes to the foreground, and:
  - it is not inside its *grace period* (§2.5), and
  - the previous foreground package was a different app (in-app activity changes don't
    re-trigger).
- **Debounce.** Ignore repeat events for the same package within ~1.5 s. Launch
  animations fire several events.

### 2.3 Pause screen: Activity, not overlay
The service starts `PauseActivity` (Compose, full-screen):
- `launchMode="singleTask"`, its own `taskAffinity`, `excludeFromRecents`, `noHistory`.
- Edge-to-edge, with a theme that has no window animation, so the guarded app is never
  usable behind it.

**Why an Activity instead of a `SYSTEM_ALERT_WINDOW` overlay**

| | Activity (chosen) | Overlay window |
|---|---|---|
| Permissions | None extra. Apps with a bound accessibility service are exempt from Android 10+ background-activity-launch limits. | Needs "Display over other apps" as a second special permission. |
| Back / home / IME / TalkBack | All work normally | Must be handled by hand; overlays easily break accessibility |
| Lifecycle, Compose, state | Standard | A Compose overlay needs a custom `LifecycleOwner` / `SavedStateRegistryOwner` |
| Covers app instantly | ~1 frame of the target app may flash first | Can be drawn over the app slightly faster |
| OEM quirks | Some aggressive OEMs (MIUI etc.) may block background starts. Mitigated by the accessibility exemption and a "pop-up in background" tip in onboarding. | Generally more robust on those OEMs |

Anasa v1 uses only the Activity. If testing shows a device where background starts get
blocked, an overlay fallback can be added later as an opt-in.

### 2.4 Pause screen flow
1. **Header:** the app's icon and name, plus "You've opened Instagram 7 times today".
   The count includes this attempt.
2. **Breathing:** an animated circle that grows on the inhale and shrinks on the exhale.
   - Spring/expressive motion, with a soft morphing shape (M3 Expressive shapes).
   - Labels: "Breathe in" / "Breathe out".
   - Breath level (per app, see §4): **Gentle** = 1 × 5 s (default), **Deeper** = 2 × 5 s,
     **Extreme** = 3 × 5 s. Each breath is 2.5 s in, 2.5 s out.
   - Optional haptics: a rising waveform during the inhale and a single soft tick at the
     turn.
   - The choice buttons stay hidden until the breathing finishes. Back goes home and
     counts as "Not now".
3. **Intention prompt** (optional): "What do you want to do on Instagram?" One-line text
   field with Skip. The text is stored locally with the event.
4. **Choice:**
   - **"Open Instagram"** logs `OPENED`, starts the grace period and launches the app via
     `getLaunchIntentForPackage`.
   - **"Not now"** logs `AVOIDED`, sends a `HOME` intent and finishes.

### 2.5 Grace period (no re-trigger loop)
Needed to avoid infinite loops: our own relaunch of the app must not trigger a new pause.
- After "Open", the app gets a **grace period** (per-app setting: 30 s / 1 / 3 / 5 / 10 min;
  default 3 min). The minimum is 30 s; there is no shorter option.
- The first entry right after "Open" is our own relaunch and is not counted.
- Re-entering the app during the grace period shows no pause, but is **recorded** as a
  `GRACE_OPEN` event. Every entry into a guarded app is logged, paused or not.
- Grace windows live in memory in the service. They are not persisted, so a reboot
  clears them, which is fine.

---

## 3. Stats (local only)

Room table `events(id, timestamp, packageName, type[ATTEMPT|OPENED|AVOIDED|GRACE_OPEN], intention?)`.

**Recording rules:**
- An `ATTEMPT` is written each time the pause screen appears.
- Exactly one outcome follows: `OPENED` or `AVOIDED`.
- A `GRACE_OPEN` is written each time the app is re-entered during its grace period.
- If the user leaves the pause screen in another way (e.g. a notification tap), the
  outcome is `AVOIDED`, because they didn't open the app.

**Screens:**
- **Today:** total attempts, opened and avoided; per-app rows with counts.
- **Last 7 days:** a bar chart per day (opened vs avoided, stacked) with an app filter.
- **Streaks:** the *avoided streak* is the number of consecutive days, ending today
  (or yesterday if today has none yet), with at least one "Not now". Shown overall and
  per app, together with the best streak.
- **Recent intentions:** the last few typed intentions per app (read-only).
- **Reset all data** button, with a confirmation step.

Days use the device's local time zone and start at midnight.

---

## 4. Settings (DataStore)
**Global defaults** (used by any app without its own override):
- Breath level: Gentle (1×5 s, default) / Deeper (2×5 s) / Extreme (3×5 s).
- Grace period: 30 s / 1 / 3 / 5 / 10 min (default 3 min).
- Haptics: on or off (default on).
- Intention prompt: on or off (default off).

**Per-app** (tap a guarded app):
- Breath level: "Use default" or a specific level.
- Grace period: "Use default" or a specific value.
- Remove from guarded list.

**Other:**
- Guarded apps (opens the picker).
- Status: shows whether the accessibility service is enabled, with a button to open
  system settings.
- About: version, "free & offline", and the privacy statement.

Per-app overrides are stored in Room (`app_config(packageName, breathLevel?, graceSeconds?)`,
null = use default); global settings are in DataStore.

---

## 5. Onboarding
1. **Welcome:** what Anasa does, in one sentence and one animation.
2. **Pick apps:** suggestions are pre-ticked if installed (Instagram, TikTok, YouTube,
   X, Facebook, Reddit, Snapchat).
3. **Accessibility:** a plain explanation of why it's needed and what we can and can't
   see.
   - We only see which app is in the foreground. We do not read screen content, and
     nothing leaves the phone.
   - "Open settings" opens `ACTION_ACCESSIBILITY_SETTINGS`.
   - The screen detects the return and checks whether the service is now enabled.
   - On Android 13+ the disclosure covers the "restricted setting" step for sideloaded
     apps: App info → ⋮ → *Allow restricted settings*.
4. **Done:** includes an optional tip about battery optimization and OEM "background
   pop-up" permissions.

The home screen shows a persistent banner whenever the service is disabled.

---

## 6. Explicitly out of scope for v1
Common in this app category; leaving these out keeps v1 small and private:
- Website blocking, and hiding in-app sections (Reels/Shorts). These would need
  window-content access.
- Scheduled, strict or full app blocks; daily time budgets; re-intervention timers.
- Uninstall protection (device admin) and notification suppression (notification
  listener).
- A foreground service and usage-stats access. Not needed, because the accessibility
  service is enough.
- Tips/articles, accounts, research studies, billing, analytics.

---

## 7. Tech
- Kotlin, Jetpack Compose, Material 3 (Expressive APIs where stable: expressive shapes,
  `MotionScheme.expressive()`), dynamic color on Android 12+ with a custom fallback
  palette.
- minSdk 26, targetSdk/compileSdk at the latest stable (36).
- Room + KSP, DataStore Preferences, Navigation Compose, Coroutines/Flow. No DI
  framework; a small manual `AppContainer`.
- Single `:app` module, organized by package: `service/`, `pause/`, `stats/`,
  `settings/`, `onboarding/`, `data/`.
- Builds with `./gradlew assembleDebug`. A few unit tests cover the streak and
  allow-window logic.

**Manifest permissions:** `VIBRATE` and `BIND_ACCESSIBILITY_SERVICE` (on the service).
Nothing else.

---

## 8. Decisions (approved)
- Per-app breath level and grace period: **yes**, with global defaults.
- Streak = consecutive days with ≥1 "Not now".
- Breath levels: Gentle 1×5 s (default), Deeper 2×5 s, Extreme 3×5 s.
- Pause screen is launched as an Activity only; no overlay fallback in v1.
- Entirely free. No paid tier, paywall or "Pro" features in v1. Any paid version is a
  later decision.
