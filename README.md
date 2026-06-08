# SubMask

SubMask is an Android 11+ learning utility that displays a movable subtitle mask over other video apps.

## MVP Features

- Android 11+ only
- Manual start from the app
- System overlay mask
- Notification action to stop the mask
- Drag and resize when unlocked
- Lock button to prevent accidental movement
- Separate portrait and landscape mask geometry
- Shared opacity

## Build

Prerequisites:

- JDK 17+
- Android SDK with API 36 installed

```powershell
.\gradlew.bat :app:assembleDebug
```

## Test

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

## Manual Verification

1. Install the debug APK.
2. Open SubMask.
3. Tap Start and grant Display over other apps permission.
4. Grant notification permission on Android 13+.
5. Start the mask.
6. Open Douyin, YouTube, or Bilibili.
7. Drag and resize the mask over subtitles.
8. Lock the mask.
9. Rotate between portrait and landscape and verify each orientation restores its own geometry.
10. Use the notification action to stop the mask.
