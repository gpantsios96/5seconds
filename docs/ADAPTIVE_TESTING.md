# Adaptive testing (Google Play Tier 3, "Adaptive ready")

Reference: <https://developer.android.com/docs/quality-guidelines/adaptive-app-quality/tier-3>

## What the app does for Tier 3

| Tier 3 item | How Techdox meets it |
|---|---|
| Fills the window, no letterboxing | No `screenOrientation`, `resizeableActivity`, aspect-ratio or `configChanges` entries in the manifest. targetSdk 37. |
| Content doesn't overflow | Every screen scrolls when the window is short. The pause screen scrolls and hides the app icon when the window height is compact (`isWindowHeightCompact()`). |
| Not stretched in wide windows | Screen content is capped at 640 dp and centred (`ui/Adaptive.kt`). The pause card and its buttons are capped at 480 dp. |
| State kept across rotation, fold/unfold and resize | The pause countdown is computed from the session's start time (`PauseTimer`), so recreating the screen neither restarts nor skips it. Text fields, tabs, scroll positions and open sheets use saveable state. The pause session itself lives in `PauseCoordinator`, which outlives the activity. |
| Multi-window and multi-resume | The pause screen counts as "left" only when it stops being visible (`onStop`), never just because another window has focus. The countdown keeps running while unfocused. |
| Keyboard, mouse, trackpad, stylus | Standard Compose components and text fields; no custom input handling. |
| Camera, media projection, playback | Not used by the app. |

## Automated tests

Run with `./gradlew testDebugUnitTest` (JVM, Robolectric; no emulator needed).

- `PauseTimerTest`: countdown maths, including a clock going backwards.
- `PauseConfigChangeTest`:
  - recreating the screen mid-countdown doesn't show the buttons early;
  - it keeps the remaining time;
  - folding to a landscape window keeps it;
  - typed intention text survives.
- `PauseLayoutTest`: the pause screen at these sizes. Both buttons must be reachable, inside the window and no wider than 480 dp.
  - phone portrait and landscape;
  - cover screen;
  - split-screen half;
  - 1:1 inner display;
  - 4:3 inner display;
  - tablet.
- `MainLayoutTest`: the first onboarding screen fits, and in wide windows is capped and centred.

These tests use Robolectric's simulated displays. They don't replace the manual checks below, especially for multi-window, which Robolectric can't simulate.

## Manual checklist

### Emulators to create in Android Studio
- Phone → **Foldable (1:1 inner display)**, API 36 or 37.
- Phone → **Foldable (4:3 inner display)**, API 36 or 37. It also has a cover screen.
- Phone → **Resizable (Experimental)**, or Desktop → **Desktop (Preview)** for freeform windows.
- Optional: Tablet → **Medium Tablet**.

### Setup on each emulator
1. Install the debug APK.
2. Finish onboarding and turn on the accessibility service.
3. Guard one app that's on the emulator (Chrome works).

### Pause screen

| # | Steps | Expected |
|---|---|---|
| P1 | Open the guarded app. While the progress bar runs, rotate to landscape. | Same card text. The progress bar continues from where it was; it doesn't restart, and the buttons don't appear early. |
| P2 | 4:3 foldable: fold to the cover screen mid-countdown, then unfold again. | Same pause session the whole time: same text, countdown continues. No "Not now" is logged (check Today afterwards). |
| P3 | Start a pause on the cover screen and unfold mid-countdown. | Same as P2: the session continues on the inner screen. |
| P4 | Unfolded, landscape, 1:1 and 4:3. | Card and buttons centred, at most about card width. No black bars, nothing cut off. |
| P5 | Turn on the intention prompt. Wait for the buttons, type text, rotate, then fold and unfold. | Typed text is still there. |
| P6 | Split-screen: put Techdox's main app in one half and the guarded app in the other, or open the guarded app into a split. | The pause appears. **Note which half or window it appears in**, and whether the guarded app is still usable beside it. It renders without overflow; in a short half it scrolls. |
| P7 | Freeform or desktop window: open the guarded app, then drag-resize the pause window small and large mid-countdown. | Resizes live, countdown continues, buttons always reachable. |
| P8 | Split-screen with the pause visible: tap into the other half so it takes focus. | The countdown keeps running; the pause is not dismissed. |
| P9 | Desktop: minimise the pause window. | Counts as "Not now" after about 2 s (by design). |

### App screens (Today, Stats, Apps, Settings, onboarding)

| # | Steps | Expected |
|---|---|---|
| A1 | Each tab: scroll down, then rotate, fold/unfold and resize. | Same tab, same scroll position. |
| A2 | Settings → Your reasons: add an empty field and type half a line, then rotate. | Fields and text kept. |
| A3 | Apps → open an app's sheet, then rotate. | Sheet still open. |
| A4 | Each screen on 1:1, 4:3 and tablet. | Content centred, at most 640 dp wide, no letterboxing. |
| A5 | Each screen in split-screen and freeform, dragged small and large. | Nothing cut off; everything reachable by scrolling. |
| A6 | Each screen combined: rotate, then fold, then resize (Tier 3 *Config_Combinations*). | Same as A1–A5. |
| A7 | Connect a physical keyboard (emulator: host keyboard). Type in search, reasons and intention. Disconnect it and type with the on-screen keyboard. | Typing works both ways; nothing is lost. |
| A8 | Mouse/trackpad: click every button, chip and switch; scroll lists with the wheel, including over the empty side margins on wide screens. | Everything responds; lists scroll from anywhere. |

## Play Store screenshots

Play recommends large-screen screenshots in three shapes. Capture these:

| Screenshot | Device / state | Screen to show |
|---|---|---|
| **1:1 unfolded** | Foldable (1:1 inner display), unfolded, landscape | The **pause card** over a guarded app, with the buttons visible: the app's core moment. |
| **4:3 unfolded** | Foldable (4:3 inner display), unfolded, landscape | **Stats**, with a week of data: chart, streak and totals. |
| **10:16 folded cover** | Foldable (4:3 inner display), folded, cover screen, portrait | **Today**, with the streak card and per-app list. |

Before capturing, use the app for a few days, or seed data, so Today and Stats aren't empty.

## Things to verify by hand (not covered by automated tests)

- **Where the pause opens in split-screen and desktop windows (P6).** The pause is a separate task started by the accessibility service. Android decides which window it opens in. If it doesn't cover the guarded app, the guarded app stays usable beside it. Record the result per Android version before deciding on a fix.
- **Samsung foldables.** Some settings close apps when the phone is folded instead of moving them to the cover screen. That would end the pause as "Not now" (P2). Test on a real Samsung device.
- **Physical keyboard.** Connecting a keyboard recreates the activity, as on every Android app without `configChanges`. All state is kept. Tier 3 says the app must switch keyboards "without relaunching the app"; I read recreation-with-state as passing, but the wording is ambiguous.
