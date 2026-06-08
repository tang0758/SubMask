# SubMask Quick Settings Tile Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an Android Quick Settings Tile that starts or stops the SubMask overlay from the system quick settings panel.

**Architecture:** Reuse the existing `OverlayService`, permission flow, notification stop path, and portrait/landscape overlay configuration. Add a small `TileService` integration layer that only handles quick-settings state and routing.

**Tech Stack:** Native Android, Kotlin, Android `TileService`, existing foreground `OverlayService`, minSdk 30, targetSdk 36.

---

## Summary

Add an Android Quick Settings Tile so the user can pull down quick settings and tap once to start or stop the SubMask subtitle blocker.

Scope is limited to the system quick settings tile. Do not add notification enhancements in this feature.

The current MVP already has:

- `OverlayService`
- notification-based close
- overlay permission flow
- notification permission flow
- portrait and landscape geometry persistence

This feature should reuse those pieces and must not change the core overlay interaction model.

## Key Changes

### Task 1: Add Tile Service

**Files:**

- Create: `app/src/main/java/com/submask/app/tile/SubMaskTileService.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] Create `SubMaskTileService : TileService`.
- [ ] Register it with `android.service.quicksettings.action.QS_TILE`.
- [ ] Use tile label `SubMask`.
- [ ] Default tile state should be inactive.
- [ ] Active tile state should mean the overlay service is running.

Manifest service requirements:

```xml
<service
    android:name=".tile.SubMaskTileService"
    android:exported="true"
    android:icon="@android:drawable/ic_menu_view"
    android:label="SubMask"
    android:permission="android.permission.BIND_QUICK_SETTINGS_TILE">
    <intent-filter>
        <action android:name="android.service.quicksettings.action.QS_TILE" />
    </intent-filter>
</service>
```

### Task 2: Add OverlayService State And Intent Helpers

**Files:**

- Modify: `app/src/main/java/com/submask/app/overlay/OverlayService.kt`

- [ ] Add a reusable start intent factory for tile and activity callers.
- [ ] Keep existing `stopIntent(context)` behavior.
- [ ] Add a process-local running state flag, updated on service start and destroy.
- [ ] Expose a simple read API such as `OverlayService.isRunning`.
- [ ] Request tile state refresh when the service starts or stops.

Minimum behavior:

- Service start sets running state true.
- Service destroy sets running state false.
- Notification close and overlay close should both transition running state to false through service stop.

If real-device testing shows tile state refresh is unreliable after process death, switch the running flag to SharedPreferences in a follow-up fix. Do not add SharedPreferences persistence in the first implementation unless needed.

### Task 3: Implement Tile Click Routing

**Files:**

- Modify: `app/src/main/java/com/submask/app/tile/SubMaskTileService.kt`

Tile click behavior:

- If overlay is running:
  - Send `OverlayService.stopIntent(this)`.
  - Update tile inactive.
- If overlay is not running and permissions are complete:
  - Start `OverlayService`.
  - Update tile active.
- If overlay or notification permission is missing:
  - Collapse quick settings.
  - Open `MainActivity` for permission guidance.

Permission rules:

- Overlay permission: use `Settings.canDrawOverlays(this)`.
- Notification permission: on Android 13+, require `POST_NOTIFICATIONS`; below Android 13, treat as granted.
- Do not show only a Toast for missing permission.

Activity launch rules:

- Android 14+ should use `TileService.startActivityAndCollapse(PendingIntent)`.
- Lower versions should use the compatible `startActivityAndCollapse(Intent)` path.
- The target activity is `MainActivity`.
- After permissions are granted, the user still taps “开始遮挡” manually. Do not auto-start the overlay after returning from permission settings.

### Task 4: Tile State Refresh

**Files:**

- Modify: `app/src/main/java/com/submask/app/tile/SubMaskTileService.kt`
- Modify: `app/src/main/java/com/submask/app/overlay/OverlayService.kt`

- [ ] Implement a helper that sets `qsTile.state` to `Tile.STATE_ACTIVE` when the overlay is running.
- [ ] Set `qsTile.state` to `Tile.STATE_INACTIVE` when it is not running.
- [ ] Call `qsTile.updateTile()` after state changes.
- [ ] Refresh state in `onStartListening()`.
- [ ] Refresh state after tile click handling.
- [ ] Request tile refresh from `OverlayService` start and destroy so notification close and overlay close update the tile.

The tile must not control overlay geometry, opacity, lock state, or orientation behavior.

### Task 5: Verification

**Commands:**

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

**Real-device scenarios:**

- Install APK.
- Add `SubMask` tile to Quick Settings manually.
- With permissions missing, tap tile and confirm it opens the app permission flow.
- Grant overlay permission.
- On Android 13+, grant notification permission.
- Tap tile and confirm overlay starts and tile becomes active.
- Tap tile again and confirm overlay stops and tile becomes inactive.
- Start overlay, then close from notification; tile should become inactive.
- Start overlay, then close from overlay top-left close button; tile should become inactive.
- Rotate between portrait and landscape while overlay is active; tile should remain active and must not affect geometry.

## Assumptions

- Only Quick Settings Tile is in scope.
- Notification enhancement is out of scope.
- Android 11+ remains the supported range.
- Missing permission behavior is to open the app for guidance.
- No automatic subtitle detection, multiple masks, or per-app configuration in this feature.
