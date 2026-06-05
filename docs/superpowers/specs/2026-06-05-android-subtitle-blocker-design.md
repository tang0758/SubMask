# Android Subtitle Blocker Design

## Summary

Build a native Android app for Android 11 and above that places a manually controlled floating black mask over embedded video subtitles. The first version targets learning scenarios where the user watches videos in apps such as Douyin, YouTube, and Bilibili and wants to hide hardcoded Chinese or English subtitles without modifying the video.

The MVP is a simple system overlay: the user opens the app, taps Start, grants overlay permission if needed, then switches to the video app. A floating mask can be moved and resized over the subtitle area. The mask can be locked to avoid accidental movement and stopped from the notification shade.

## Goals

- Support Android 11+ only, with minimum API level 30.
- Work across third-party video apps by using Android's system overlay capability.
- Let the user manually place a black mask over subtitle areas.
- Support portrait and landscape videos with separate saved geometry.
- Keep the first version simple, reliable, and easy to exit.

## Non-Goals

- No iOS support in the first version.
- No automatic subtitle detection.
- No OCR, video analysis, screenshot analysis, or AI recognition.
- No per-app saved positions in the first version.
- No built-in video player.
- No boot-time automatic restore.
- No support for Android 10 or below.

## Target Users And Use Case

The user watches videos on a phone for language learning. Many videos include embedded bilingual subtitles. The user wants to hide the subtitles so they can listen first, infer meaning, or practice recall, while still using existing video platforms and apps.

The blocker should not require downloading videos, modifying videos, or using a special player. It should behave like a temporary study tool that can be started before watching and stopped immediately after.

## MVP User Flow

1. The user opens the Subtitle Blocker app.
2. The app shows a simple configuration screen with:
   - opacity control
   - Start Blocking button
   - basic status text for overlay permission
3. If overlay permission is missing, the app guides the user to Android's "Display over other apps" settings.
4. After permission is granted, the user taps Start Blocking again.
5. The app starts a foreground overlay service.
6. A black floating mask appears on screen.
7. The user switches to Douyin, YouTube, Bilibili, or another video app.
8. The user unlocks the mask if needed, drags and resizes it over the subtitle region, then locks it.
9. The user can stop the blocker from the persistent notification.

## Overlay Behavior

The mask is black with configurable opacity. The first version uses a single mask. It defaults to a horizontal subtitle-blocking bar, but resizing can turn it into a rectangle.

When unlocked:

- Dragging the mask moves it.
- Dragging a resize handle changes width and height.
- The lock button remains visible.
- Geometry changes are saved for the current orientation.

When locked:

- Dragging and resizing are disabled to avoid accidental movement while watching.
- The lock button remains available so the user can unlock the mask.
- The locked state is saved separately for portrait and landscape.

The notification must provide a Stop action. Stop removes the overlay and stops the foreground service.

## Orientation Rules

Portrait and landscape must not share mask geometry. Their screen shapes and subtitle layouts differ too much.

Persisted configuration:

- Portrait geometry: x, y, width, height
- Landscape geometry: x, y, width, height
- Portrait locked state
- Landscape locked state
- Shared opacity

When the screen orientation or window size changes:

1. Detect whether the active screen is portrait or landscape.
2. Save the previous orientation's latest geometry.
3. Load the new orientation's geometry.
4. If the new orientation has no saved geometry, use that orientation's default geometry.
5. Never apply portrait width or height directly to landscape, or landscape width or height directly to portrait.

Default geometry:

- Portrait default: lower subtitle area, medium width, medium height.
- Landscape default: lower subtitle area, wider and shorter than portrait.

## Android Architecture

### MainActivity

Responsibilities:

- Render the configuration screen.
- Show current overlay permission state.
- Open the Android overlay permission settings when needed.
- Control shared opacity.
- Start `OverlayService`.

### OverlayService

Responsibilities:

- Run as a foreground service while the blocker is active.
- Create and remove the system overlay window.
- Own the persistent notification.
- Handle notification Stop action.
- Coordinate orientation changes and config persistence.

The service should use the Android 11+ compatible overlay window type for application overlays. It should avoid hidden APIs and old-version compatibility branches that are outside the target platform.

### OverlayView

Responsibilities:

- Render the mask, lock button, and resize affordance.
- Handle drag gestures.
- Handle resize gestures.
- Handle lock and unlock.
- Report geometry changes to the service or config store.

The view should be intentionally minimal. It should not include explanatory text while placed over video content.

### OverlayConfigStore

Responsibilities:

- Persist portrait geometry.
- Persist landscape geometry.
- Persist portrait and landscape lock states.
- Persist shared opacity.
- Return default orientation geometry when no saved value exists.

The first version can use SharedPreferences or DataStore. DataStore is preferred if the project uses Kotlin and coroutines; SharedPreferences is acceptable for the small MVP state.

### OrientationWatcher

Responsibilities:

- Detect orientation or screen size class changes while the service is running.
- Trigger saving of the old orientation config.
- Trigger loading and applying of the new orientation config.

Orientation should be derived from current window/display bounds rather than assuming app activity orientation, because the overlay runs above other apps.

## Permissions And System Constraints

Required permission:

- `SYSTEM_ALERT_WINDOW`, requested through Android's overlay permission settings.

Foreground service:

- The blocker runs as a foreground service while active.
- The notification should be ongoing and clear about the active blocker.
- The notification includes a Stop action.

The app should not start automatically after reboot. The user must manually open the app and start blocking.

## Data Model

Conceptual configuration:

```text
OverlayConfig
  opacity: Float
  portrait: OrientationConfig
  landscape: OrientationConfig

OrientationConfig
  x: Int
  y: Int
  width: Int
  height: Int
  locked: Boolean
  initialized: Boolean
```

`initialized` distinguishes a real saved geometry from a default value. This prevents accidental reuse of the wrong orientation's previous size.

## Error Handling

- If overlay permission is missing, Start Blocking should not fail silently. It should show the permission requirement and provide a clear action to open settings.
- If the service cannot create the overlay, stop the service and show a user-visible error when returning to the app.
- If saved geometry is outside the current screen bounds, clamp it into the visible area.
- If width or height is too small after a screen change, clamp to a minimum usable size.
- If orientation changes rapidly, apply the latest orientation state only and avoid stacking repeated layout operations.

## Testing Strategy

Manual test cases:

- Start without overlay permission and verify permission guidance.
- Grant overlay permission and start blocker.
- Verify overlay appears above Douyin, YouTube, and Bilibili.
- Drag the unlocked mask in portrait and verify the position is saved.
- Resize the unlocked mask in portrait and verify the size is saved.
- Lock the mask and verify drag/resize no longer move it.
- Unlock the mask and verify drag/resize work again.
- Rotate to landscape and verify landscape default geometry is used if no landscape config exists.
- Adjust landscape mask, rotate back to portrait, and verify portrait geometry is restored.
- Rotate back to landscape and verify landscape geometry is restored.
- Change opacity and verify it applies in both orientations.
- Tap notification Stop and verify the overlay disappears and service stops.
- Relaunch manually and verify saved settings load.

Automated test targets:

- Config store returns defaults for uninitialized orientation state.
- Config store saves portrait and landscape independently.
- Geometry clamping keeps rectangles visible.
- Orientation switching does not copy geometry across orientations.

## Future Extensions

- Per-app saved geometry for Douyin, YouTube, Bilibili, and other apps.
- Multiple masks for separate Chinese and English subtitle lines.
- Quick settings tile for faster start and stop.
- Optional floating mini button for show/hide.
- Browser or web-player version for future non-Android-app scenarios.
- Automatic subtitle area suggestion after the manual MVP proves useful.

## Open Decisions

None for the MVP. The agreed first version is Android 11+ only, manually started, single floating mask, notification-based stop, shared opacity, and orientation-specific geometry.
