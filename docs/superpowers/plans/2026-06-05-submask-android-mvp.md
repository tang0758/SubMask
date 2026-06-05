# SubMask Android MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build SubMask, an Android 11+ native app that starts a movable, resizable, lockable floating subtitle mask with separate portrait and landscape geometry.

**Architecture:** Use one Android app module with a plain Kotlin `Activity`, a foreground `Service`, a custom `OverlayView`, and small pure Kotlin geometry/config helpers. Keep overlay behavior in the service/view layer and keep orientation/config math testable without Android framework dependencies.

**Tech Stack:** Kotlin, Android Gradle Plugin 9.2.0, Gradle 9.4.1, JDK 17, minSdk 30, targetSdk 36, programmatic Android Views, SharedPreferences, JUnit 4.

---

## Tooling Baseline

Use Android Gradle Plugin 9.2.0 with Gradle 9.4.1 and JDK 17. The official Android Gradle Plugin 9.2.0 release notes list Gradle 9.4.1 and JDK 17 in the compatibility table, and AGP 9.2 supports API level 36.1. Use `compileSdk = 36` and `targetSdk = 36` unless the local SDK installation only has a lower API; in that case install API 36 before continuing.

## File Structure

Create this project structure:

```text
settings.gradle.kts
build.gradle.kts
gradle.properties
app/
  build.gradle.kts
  src/
    main/
      AndroidManifest.xml
      java/com/submask/app/
        MainActivity.kt
        overlay/
          OverlayService.kt
          OverlayView.kt
          OverlayWindowController.kt
        config/
          OverlayConfigStore.kt
          OverlayModels.kt
        orientation/
          OrientationModels.kt
          OrientationResolver.kt
        ui/
          MainScreen.kt
    test/
      java/com/submask/app/
        config/OverlayConfigKeysTest.kt
        orientation/OrientationResolverTest.kt
```

Responsibilities:

- `MainActivity.kt`: Activity lifecycle, overlay permission checks, and service start.
- `MainScreen.kt`: Programmatic main screen UI for opacity and Start Blocking.
- `OverlayService.kt`: Foreground service lifecycle, notification, Stop action, and overlay orchestration.
- `OverlayWindowController.kt`: `WindowManager` add/update/remove calls.
- `OverlayView.kt`: Mask drawing, lock button, drag gestures, resize gestures.
- `OverlayModels.kt`: `OverlayConfig`, `OrientationConfig`, and rectangle clamping helpers.
- `OverlayConfigStore.kt`: SharedPreferences persistence.
- `OrientationModels.kt`: Orientation enum and screen bounds model.
- `OrientationResolver.kt`: Pure orientation/default geometry logic.

---

### Task 1: Scaffold Android Project

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/submask/app/MainActivity.kt`
- Create: `app/src/main/java/com/submask/app/ui/MainScreen.kt`

- [ ] **Step 1: Create Gradle settings**

Create `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SubMask"
include(":app")
```

- [ ] **Step 2: Create root build file**

Create `build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application") version "9.2.0" apply false
    id("org.jetbrains.kotlin.android") version "2.3.21" apply false
}
```

- [ ] **Step 3: Create Gradle properties**

Create `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=false
kotlin.code.style=official
```

- [ ] **Step 4: Create app module build file**

Create `app/build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.submask.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.submask.app"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "android.test.InstrumentationTestRunner"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = false
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
```

- [ ] **Step 5: Create initial manifest**

Create `app/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

    <application
        android:allowBackup="false"
        android:label="SubMask"
        android:theme="@style/AppTheme">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 6: Create minimal theme resource**

Create `app/src/main/res/values/styles.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="AppTheme" parent="android:style/Theme.Material.Light.NoActionBar">
        <item name="android:fontFamily">sans</item>
        <item name="android:windowLightStatusBar">true</item>
        <item name="android:colorAccent">#111111</item>
    </style>
</resources>
```

- [ ] **Step 7: Create minimal Activity and screen**

Create `app/src/main/java/com/submask/app/MainActivity.kt`:

```kotlin
package com.submask.app

import android.app.Activity
import android.os.Bundle
import com.submask.app.ui.MainScreen

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(MainScreen(this))
    }
}
```

Create `app/src/main/java/com/submask/app/ui/MainScreen.kt`:

```kotlin
package com.submask.app.ui

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class MainScreen(context: Context) : LinearLayout(context) {
    val opacitySeekBar: SeekBar
    val startButton: Button
    val statusText: TextView

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(48, 72, 48, 48)
        setBackgroundColor(Color.WHITE)

        addView(TextView(context).apply {
            text = "SubMask"
            textSize = 28f
            setTextColor(Color.rgb(24, 28, 32))
        })

        addView(TextView(context).apply {
            text = "字幕遮挡器"
            textSize = 16f
            setTextColor(Color.rgb(96, 104, 112))
        })

        opacitySeekBar = SeekBar(context).apply {
            max = 100
            progress = 72
        }
        addView(opacitySeekBar, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = 48
        })

        statusText = TextView(context).apply {
            text = "点击开始后会检查悬浮窗权限"
            textSize = 14f
            setTextColor(Color.rgb(96, 104, 112))
        }
        addView(statusText)

        startButton = Button(context).apply {
            text = "开始遮挡"
        }
        addView(startButton, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = 32
        })
    }
}
```

- [ ] **Step 8: Generate or verify Gradle wrapper**

If `gradlew.bat` is not present, run this from a machine with Gradle installed:

```powershell
gradle wrapper --gradle-version 9.4.1
```

Expected: `gradlew`, `gradlew.bat`, and `gradle/wrapper/` are created.

- [ ] **Step 9: Run build**

Run:

```powershell
.\gradlew.bat :app:assembleDebug
```

Expected: build succeeds and creates `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 10: Commit**

```powershell
git add settings.gradle.kts build.gradle.kts gradle.properties gradlew gradlew.bat gradle app
git commit -m "chore: scaffold Android app"
```

---

### Task 2: Add Orientation Models And Geometry Defaults

**Files:**
- Create: `app/src/main/java/com/submask/app/orientation/OrientationModels.kt`
- Create: `app/src/main/java/com/submask/app/config/OverlayModels.kt`
- Create: `app/src/main/java/com/submask/app/orientation/OrientationResolver.kt`
- Create: `app/src/test/java/com/submask/app/orientation/OrientationResolverTest.kt`

- [ ] **Step 1: Write failing orientation tests**

Create `app/src/test/java/com/submask/app/orientation/OrientationResolverTest.kt`:

```kotlin
package com.submask.app.orientation

import com.submask.app.config.MaskRect
import org.junit.Assert.assertEquals
import org.junit.Test

class OrientationResolverTest {
    @Test
    fun resolvesPortraitWhenHeightIsGreaterThanOrEqualToWidth() {
        assertEquals(ScreenOrientation.PORTRAIT, OrientationResolver.resolve(ScreenBounds(1080, 1920)))
        assertEquals(ScreenOrientation.PORTRAIT, OrientationResolver.resolve(ScreenBounds(1000, 1000)))
    }

    @Test
    fun resolvesLandscapeWhenWidthIsGreaterThanHeight() {
        assertEquals(ScreenOrientation.LANDSCAPE, OrientationResolver.resolve(ScreenBounds(1920, 1080)))
    }

    @Test
    fun portraitDefaultUsesLowerSubtitleArea() {
        val rect = OrientationResolver.defaultRect(ScreenOrientation.PORTRAIT, ScreenBounds(1080, 1920))
        assertEquals(MaskRect(x = 108, y = 1392, width = 864, height = 154), rect)
    }

    @Test
    fun landscapeDefaultUsesWiderShorterLowerSubtitleArea() {
        val rect = OrientationResolver.defaultRect(ScreenOrientation.LANDSCAPE, ScreenBounds(1920, 1080))
        assertEquals(MaskRect(x = 288, y = 799, width = 1344, height = 86), rect)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.submask.app.orientation.OrientationResolverTest"
```

Expected: FAIL because `ScreenOrientation`, `ScreenBounds`, `MaskRect`, and `OrientationResolver` do not exist.

- [ ] **Step 3: Implement models**

Create `app/src/main/java/com/submask/app/orientation/OrientationModels.kt`:

```kotlin
package com.submask.app.orientation

enum class ScreenOrientation {
    PORTRAIT,
    LANDSCAPE
}

data class ScreenBounds(
    val width: Int,
    val height: Int
) {
    init {
        require(width > 0) { "width must be positive" }
        require(height > 0) { "height must be positive" }
    }
}
```

Create `app/src/main/java/com/submask/app/config/OverlayModels.kt`:

```kotlin
package com.submask.app.config

data class MaskRect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

data class OrientationConfig(
    val rect: MaskRect,
    val locked: Boolean,
    val initialized: Boolean
)

data class OverlayConfig(
    val opacity: Float,
    val portrait: OrientationConfig,
    val landscape: OrientationConfig
)
```

- [ ] **Step 4: Implement orientation resolver**

Create `app/src/main/java/com/submask/app/orientation/OrientationResolver.kt`:

```kotlin
package com.submask.app.orientation

import com.submask.app.config.MaskRect
import kotlin.math.roundToInt

object OrientationResolver {
    fun resolve(bounds: ScreenBounds): ScreenOrientation {
        return if (bounds.width > bounds.height) {
            ScreenOrientation.LANDSCAPE
        } else {
            ScreenOrientation.PORTRAIT
        }
    }

    fun defaultRect(orientation: ScreenOrientation, bounds: ScreenBounds): MaskRect {
        return when (orientation) {
            ScreenOrientation.PORTRAIT -> {
                val width = (bounds.width * 0.80f).roundToInt()
                val height = (bounds.height * 0.08f).roundToInt()
                val x = ((bounds.width - width) / 2f).roundToInt()
                val y = (bounds.height * 0.725f).roundToInt()
                MaskRect(x = x, y = y, width = width, height = height)
            }
            ScreenOrientation.LANDSCAPE -> {
                val width = (bounds.width * 0.70f).roundToInt()
                val height = (bounds.height * 0.08f).roundToInt()
                val x = ((bounds.width - width) / 2f).roundToInt()
                val y = (bounds.height * 0.74f).roundToInt()
                MaskRect(x = x, y = y, width = width, height = height)
            }
        }
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.submask.app.orientation.OrientationResolverTest"
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/submask/app/orientation app/src/main/java/com/submask/app/config app/src/test/java/com/submask/app/orientation
git commit -m "feat: add orientation defaults"
```

---

### Task 3: Add Geometry Clamping

**Files:**
- Modify: `app/src/main/java/com/submask/app/config/OverlayModels.kt`
- Create: `app/src/test/java/com/submask/app/config/MaskRectTest.kt`

- [ ] **Step 1: Write failing geometry tests**

Create `app/src/test/java/com/submask/app/config/MaskRectTest.kt`:

```kotlin
package com.submask.app.config

import com.submask.app.orientation.ScreenBounds
import org.junit.Assert.assertEquals
import org.junit.Test

class MaskRectTest {
    @Test
    fun clampsRectInsideScreenBounds() {
        val rect = MaskRect(x = 950, y = 1800, width = 300, height = 300)
        val clamped = rect.clampTo(ScreenBounds(1080, 1920), minWidth = 80, minHeight = 48)
        assertEquals(MaskRect(x = 780, y = 1620, width = 300, height = 300), clamped)
    }

    @Test
    fun expandsTooSmallRectToMinimumSize() {
        val rect = MaskRect(x = 20, y = 30, width = 10, height = 10)
        val clamped = rect.clampTo(ScreenBounds(1080, 1920), minWidth = 80, minHeight = 48)
        assertEquals(MaskRect(x = 20, y = 30, width = 80, height = 48), clamped)
    }

    @Test
    fun clampsNegativePositionToZero() {
        val rect = MaskRect(x = -40, y = -20, width = 200, height = 80)
        val clamped = rect.clampTo(ScreenBounds(1080, 1920), minWidth = 80, minHeight = 48)
        assertEquals(MaskRect(x = 0, y = 0, width = 200, height = 80), clamped)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.submask.app.config.MaskRectTest"
```

Expected: FAIL because `clampTo` does not exist.

- [ ] **Step 3: Implement clamping**

Update `app/src/main/java/com/submask/app/config/OverlayModels.kt`:

```kotlin
package com.submask.app.config

import com.submask.app.orientation.ScreenBounds
import kotlin.math.max
import kotlin.math.min

data class MaskRect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
) {
    fun clampTo(bounds: ScreenBounds, minWidth: Int, minHeight: Int): MaskRect {
        val clampedWidth = min(max(width, minWidth), bounds.width)
        val clampedHeight = min(max(height, minHeight), bounds.height)
        val maxX = max(0, bounds.width - clampedWidth)
        val maxY = max(0, bounds.height - clampedHeight)
        return copy(
            x = x.coerceIn(0, maxX),
            y = y.coerceIn(0, maxY),
            width = clampedWidth,
            height = clampedHeight
        )
    }
}

data class OrientationConfig(
    val rect: MaskRect,
    val locked: Boolean,
    val initialized: Boolean
)

data class OverlayConfig(
    val opacity: Float,
    val portrait: OrientationConfig,
    val landscape: OrientationConfig
)
```

- [ ] **Step 4: Run tests**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/submask/app/config/OverlayModels.kt app/src/test/java/com/submask/app/config/MaskRectTest.kt
git commit -m "feat: clamp overlay geometry"
```

---

### Task 4: Add SharedPreferences Config Store

**Files:**
- Create: `app/src/main/java/com/submask/app/config/OverlayConfigStore.kt`
- Create: `app/src/test/java/com/submask/app/config/OverlayConfigKeysTest.kt`

- [ ] **Step 1: Write failing key tests**

Create `app/src/test/java/com/submask/app/config/OverlayConfigKeysTest.kt`:

```kotlin
package com.submask.app.config

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayConfigKeysTest {
    @Test
    fun keysSeparatePortraitAndLandscapeGeometry() {
        assertEquals("portrait_x", OverlayConfigKeys.portraitX)
        assertEquals("landscape_x", OverlayConfigKeys.landscapeX)
        assertEquals("portrait_initialized", OverlayConfigKeys.portraitInitialized)
        assertEquals("landscape_initialized", OverlayConfigKeys.landscapeInitialized)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.submask.app.config.OverlayConfigKeysTest"
```

Expected: FAIL because `OverlayConfigKeys` does not exist.

- [ ] **Step 3: Implement config store**

Create `app/src/main/java/com/submask/app/config/OverlayConfigStore.kt`:

```kotlin
package com.submask.app.config

import android.content.Context
import android.content.SharedPreferences
import com.submask.app.orientation.OrientationResolver
import com.submask.app.orientation.ScreenBounds
import com.submask.app.orientation.ScreenOrientation

object OverlayConfigKeys {
    const val opacity = "opacity"

    const val portraitX = "portrait_x"
    const val portraitY = "portrait_y"
    const val portraitWidth = "portrait_width"
    const val portraitHeight = "portrait_height"
    const val portraitLocked = "portrait_locked"
    const val portraitInitialized = "portrait_initialized"

    const val landscapeX = "landscape_x"
    const val landscapeY = "landscape_y"
    const val landscapeWidth = "landscape_width"
    const val landscapeHeight = "landscape_height"
    const val landscapeLocked = "landscape_locked"
    const val landscapeInitialized = "landscape_initialized"
}

class OverlayConfigStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("overlay_config", Context.MODE_PRIVATE)

    fun getOpacity(): Float = prefs.getFloat(OverlayConfigKeys.opacity, 0.72f)

    fun setOpacity(opacity: Float) {
        prefs.edit()
            .putFloat(OverlayConfigKeys.opacity, opacity.coerceIn(0.20f, 1.00f))
            .apply()
    }

    fun loadOrientationConfig(
        orientation: ScreenOrientation,
        bounds: ScreenBounds
    ): OrientationConfig {
        val keys = keysFor(orientation)
        val initialized = prefs.getBoolean(keys.initialized, false)
        if (!initialized) {
            return OrientationConfig(
                rect = OrientationResolver.defaultRect(orientation, bounds),
                locked = false,
                initialized = false
            )
        }

        val rect = MaskRect(
            x = prefs.getInt(keys.x, 0),
            y = prefs.getInt(keys.y, 0),
            width = prefs.getInt(keys.width, bounds.width),
            height = prefs.getInt(keys.height, bounds.height)
        ).clampTo(bounds, MIN_MASK_WIDTH, MIN_MASK_HEIGHT)

        return OrientationConfig(
            rect = rect,
            locked = prefs.getBoolean(keys.locked, false),
            initialized = true
        )
    }

    fun saveOrientationConfig(
        orientation: ScreenOrientation,
        config: OrientationConfig
    ) {
        val keys = keysFor(orientation)
        prefs.edit()
            .putInt(keys.x, config.rect.x)
            .putInt(keys.y, config.rect.y)
            .putInt(keys.width, config.rect.width)
            .putInt(keys.height, config.rect.height)
            .putBoolean(keys.locked, config.locked)
            .putBoolean(keys.initialized, true)
            .apply()
    }

    private fun keysFor(orientation: ScreenOrientation): OrientationKeys {
        return when (orientation) {
            ScreenOrientation.PORTRAIT -> OrientationKeys(
                OverlayConfigKeys.portraitX,
                OverlayConfigKeys.portraitY,
                OverlayConfigKeys.portraitWidth,
                OverlayConfigKeys.portraitHeight,
                OverlayConfigKeys.portraitLocked,
                OverlayConfigKeys.portraitInitialized
            )
            ScreenOrientation.LANDSCAPE -> OrientationKeys(
                OverlayConfigKeys.landscapeX,
                OverlayConfigKeys.landscapeY,
                OverlayConfigKeys.landscapeWidth,
                OverlayConfigKeys.landscapeHeight,
                OverlayConfigKeys.landscapeLocked,
                OverlayConfigKeys.landscapeInitialized
            )
        }
    }

    private data class OrientationKeys(
        val x: String,
        val y: String,
        val width: String,
        val height: String,
        val locked: String,
        val initialized: String
    )

    companion object {
        const val MIN_MASK_WIDTH = 80
        const val MIN_MASK_HEIGHT = 48
    }
}
```

- [ ] **Step 4: Run tests**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/submask/app/config/OverlayConfigStore.kt app/src/test/java/com/submask/app/config/OverlayConfigKeysTest.kt
git commit -m "feat: add overlay config store"
```

---

### Task 5: Wire Main Screen Permission And Service Start

**Files:**
- Modify: `app/src/main/java/com/submask/app/MainActivity.kt`
- Modify: `app/src/main/java/com/submask/app/ui/MainScreen.kt`
- Create: `app/src/main/java/com/submask/app/overlay/OverlayService.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Add service skeleton**

Create `app/src/main/java/com/submask/app/overlay/OverlayService.kt`:

```kotlin
package com.submask.app.overlay

import android.app.Service
import android.content.Intent
import android.os.IBinder

class OverlayService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}
```

- [ ] **Step 2: Register service in manifest**

Update `app/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

    <application
        android:allowBackup="false"
        android:label="SubMask"
        android:theme="@style/AppTheme">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".overlay.OverlayService"
            android:exported="false" />
    </application>
</manifest>
```

- [ ] **Step 3: Update screen callback API**

Replace `app/src/main/java/com/submask/app/ui/MainScreen.kt` with:

```kotlin
package com.submask.app.ui

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class MainScreen(
    context: Context,
    initialOpacityPercent: Int,
    onOpacityChanged: (Int) -> Unit,
    onStartClicked: () -> Unit
) : LinearLayout(context) {
    private val statusText: TextView

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(48, 72, 48, 48)
        setBackgroundColor(Color.WHITE)

        addView(TextView(context).apply {
            text = "SubMask"
            textSize = 28f
            setTextColor(Color.rgb(24, 28, 32))
        })

        addView(TextView(context).apply {
            text = "字幕遮挡器"
            textSize = 16f
            setTextColor(Color.rgb(96, 104, 112))
        })

        addView(TextView(context).apply {
            text = "遮挡透明度"
            textSize = 15f
            setTextColor(Color.rgb(24, 28, 32))
        }, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = 48
        })

        addView(SeekBar(context).apply {
            max = 100
            progress = initialOpacityPercent
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) onOpacityChanged(progress)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        statusText = TextView(context).apply {
            text = "点击开始后会检查悬浮窗权限"
            textSize = 14f
            setTextColor(Color.rgb(96, 104, 112))
        }
        addView(statusText, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = 16
        })

        addView(Button(context).apply {
            text = "开始遮挡"
            setOnClickListener { onStartClicked() }
        }, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = 32
        })
    }

    fun setPermissionStatus(hasPermission: Boolean) {
        statusText.text = if (hasPermission) {
            "悬浮窗权限已开启"
        } else {
            "需要开启“显示在其他应用上层”权限"
        }
    }
}
```

- [ ] **Step 4: Wire permission and start behavior**

Replace `app/src/main/java/com/submask/app/MainActivity.kt` with:

```kotlin
package com.submask.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import com.submask.app.config.OverlayConfigStore
import com.submask.app.overlay.OverlayService
import com.submask.app.ui.MainScreen

class MainActivity : Activity() {
    private lateinit var configStore: OverlayConfigStore
    private lateinit var mainScreen: MainScreen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configStore = OverlayConfigStore(this)
        mainScreen = MainScreen(
            context = this,
            initialOpacityPercent = (configStore.getOpacity() * 100).toInt(),
            onOpacityChanged = { percent ->
                configStore.setOpacity(percent / 100f)
            },
            onStartClicked = {
                startBlockingOrRequestPermission()
            }
        )
        setContentView(mainScreen)
    }

    override fun onResume() {
        super.onResume()
        mainScreen.setPermissionStatus(Settings.canDrawOverlays(this))
    }

    private fun startBlockingOrRequestPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return
        }

        startForegroundService(Intent(this, OverlayService::class.java))
    }
}
```

- [ ] **Step 5: Build**

Run:

```powershell
.\gradlew.bat :app:assembleDebug
```

Expected: PASS.

- [ ] **Step 6: Manual check**

Install and launch:

```powershell
.\gradlew.bat :app:installDebug
```

Expected:

- App opens with title `SubMask`.
- Start opens Android overlay permission settings when permission is missing.
- After permission is granted, Start returns to app behavior without crash.

- [ ] **Step 7: Commit**

```powershell
git add app/src/main
git commit -m "feat: wire overlay permission flow"
```

---

### Task 6: Add Foreground Service Notification And Stop Action

**Files:**
- Modify: `app/src/main/java/com/submask/app/overlay/OverlayService.kt`

- [ ] **Step 1: Replace service with foreground notification logic**

Replace `app/src/main/java/com/submask/app/overlay/OverlayService.kt` with:

```kotlin
package com.submask.app.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

class OverlayService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        ensureNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SubMask",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "字幕遮挡器运行状态"
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("SubMask 正在运行")
            .setContentText("正在显示字幕遮挡条")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "关闭遮挡", stopPendingIntent)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "submask_overlay"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_STOP = "com.submask.app.action.STOP"

        fun stopIntent(context: Context): Intent {
            return Intent(context, OverlayService::class.java).apply {
                action = ACTION_STOP
            }
        }
    }
}
```

- [ ] **Step 2: Build**

Run:

```powershell
.\gradlew.bat :app:assembleDebug
```

Expected: PASS.

- [ ] **Step 3: Manual check**

Install, grant overlay permission, tap Start:

```powershell
.\gradlew.bat :app:installDebug
```

Expected:

- Notification appears with title `SubMask 正在运行`.
- Notification has `关闭遮挡` action.
- Tapping `关闭遮挡` stops the foreground service and removes the notification.

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/java/com/submask/app/overlay/OverlayService.kt
git commit -m "feat: add foreground overlay notification"
```

---

### Task 7: Add Overlay Window And Static Mask

**Files:**
- Create: `app/src/main/java/com/submask/app/overlay/OverlayWindowController.kt`
- Create: `app/src/main/java/com/submask/app/overlay/OverlayView.kt`
- Modify: `app/src/main/java/com/submask/app/overlay/OverlayService.kt`

- [ ] **Step 1: Create static overlay view**

Create `app/src/main/java/com/submask/app/overlay/OverlayView.kt`:

```kotlin
package com.submask.app.overlay

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView

class OverlayView(context: Context) : FrameLayout(context) {
    private val lockButton: TextView

    init {
        setBackgroundColor(Color.argb(184, 0, 0, 0))

        lockButton = TextView(context).apply {
            text = "锁"
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(80, 255, 255, 255))
        }

        addView(lockButton, LayoutParams(56, 56, Gravity.END or Gravity.TOP))
    }

    fun setMaskOpacity(opacity: Float) {
        val alpha = (opacity.coerceIn(0.20f, 1.00f) * 255).toInt()
        setBackgroundColor(Color.argb(alpha, 0, 0, 0))
    }
}
```

- [ ] **Step 2: Create window controller**

Create `app/src/main/java/com/submask/app/overlay/OverlayWindowController.kt`:

```kotlin
package com.submask.app.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.submask.app.config.MaskRect

class OverlayWindowController(context: Context) {
    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(WindowManager::class.java)
    private var view: View? = null

    fun show(overlayView: OverlayView, rect: MaskRect) {
        if (view != null) return
        view = overlayView
        windowManager.addView(overlayView, layoutParams(rect))
    }

    fun update(rect: MaskRect) {
        val currentView = view ?: return
        windowManager.updateViewLayout(currentView, layoutParams(rect))
    }

    fun remove() {
        val currentView = view ?: return
        windowManager.removeView(currentView)
        view = null
    }

    private fun layoutParams(rect: MaskRect): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            rect.width,
            rect.height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.TOP
            x = rect.x
            y = rect.y
        }
    }
}
```

- [ ] **Step 3: Show overlay from service**

Replace `app/src/main/java/com/submask/app/overlay/OverlayService.kt` with the Task 6 code plus this overlay creation in `onStartCommand`:

```kotlin
private lateinit var configStore: OverlayConfigStore
private lateinit var windowController: OverlayWindowController
private var overlayView: OverlayView? = null

override fun onCreate() {
    super.onCreate()
    configStore = OverlayConfigStore(this)
    windowController = OverlayWindowController(this)
}

override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP) {
        stopSelf()
        return START_NOT_STICKY
    }

    ensureNotificationChannel()
    startForeground(NOTIFICATION_ID, buildNotification())
    showOverlay()
    return START_STICKY
}

override fun onDestroy() {
    windowController.remove()
    overlayView = null
    super.onDestroy()
}

private fun showOverlay() {
    if (overlayView != null) return
    val bounds = currentScreenBounds()
    val orientation = OrientationResolver.resolve(bounds)
    val config = configStore.loadOrientationConfig(orientation, bounds)
    val view = OverlayView(this).apply {
        setMaskOpacity(configStore.getOpacity())
    }
    overlayView = view
    windowController.show(view, config.rect)
}

private fun currentScreenBounds(): ScreenBounds {
    val metrics = resources.displayMetrics
    return ScreenBounds(metrics.widthPixels, metrics.heightPixels)
}
```

Also add imports:

```kotlin
import com.submask.app.config.OverlayConfigStore
import com.submask.app.orientation.OrientationResolver
import com.submask.app.orientation.ScreenBounds
```

- [ ] **Step 4: Build**

Run:

```powershell
.\gradlew.bat :app:assembleDebug
```

Expected: PASS.

- [ ] **Step 5: Manual check**

Install, grant permission, tap Start.

Expected:

- A black semi-transparent rectangle appears over all apps.
- Notification Stop removes the rectangle.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/submask/app/overlay
git commit -m "feat: show static overlay mask"
```

---

### Task 8: Add Drag, Resize, Lock, And Persistence

**Files:**
- Modify: `app/src/main/java/com/submask/app/overlay/OverlayView.kt`
- Modify: `app/src/main/java/com/submask/app/overlay/OverlayService.kt`

- [ ] **Step 1: Replace overlay view with interactive version**

Replace `app/src/main/java/com/submask/app/overlay/OverlayView.kt`:

```kotlin
package com.submask.app.overlay

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.TextView
import com.submask.app.config.MaskRect

class OverlayView(
    context: Context,
    initialRect: MaskRect,
    initialLocked: Boolean,
    private val onRectChanged: (MaskRect) -> Unit,
    private val onLockChanged: (Boolean) -> Unit
) : FrameLayout(context) {
    private val lockButton: TextView
    private val resizeHandle: TextView
    private var rect = initialRect
    private var locked = initialLocked
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var startRect = initialRect
    private var mode = TouchMode.NONE

    init {
        setMaskOpacity(0.72f)

        lockButton = TextView(context).apply {
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(90, 255, 255, 255))
            setOnClickListener {
                setLocked(!locked)
            }
        }
        addView(lockButton, LayoutParams(56, 56, Gravity.END or Gravity.TOP))

        resizeHandle = TextView(context).apply {
            text = "↘"
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(90, 255, 255, 255))
        }
        addView(resizeHandle, LayoutParams(56, 56, Gravity.END or Gravity.BOTTOM))

        setLocked(initialLocked)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (locked) return true

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragStartX = event.rawX
                dragStartY = event.rawY
                startRect = rect
                mode = if (event.x > width - 96 && event.y > height - 96) {
                    TouchMode.RESIZE
                } else {
                    TouchMode.DRAG
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - dragStartX).toInt()
                val dy = (event.rawY - dragStartY).toInt()
                rect = when (mode) {
                    TouchMode.DRAG -> startRect.copy(x = startRect.x + dx, y = startRect.y + dy)
                    TouchMode.RESIZE -> startRect.copy(
                        width = startRect.width + dx,
                        height = startRect.height + dy
                    )
                    TouchMode.NONE -> rect
                }
                onRectChanged(rect)
                return true
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                mode = TouchMode.NONE
                return true
            }
        }

        return true
    }

    fun setMaskOpacity(opacity: Float) {
        val alpha = (opacity.coerceIn(0.20f, 1.00f) * 255).toInt()
        setBackgroundColor(Color.argb(alpha, 0, 0, 0))
    }

    fun setRect(newRect: MaskRect) {
        rect = newRect
    }

    private fun setLocked(newLocked: Boolean) {
        locked = newLocked
        lockButton.text = if (locked) "解" else "锁"
        resizeHandle.visibility = if (locked) GONE else VISIBLE
        onLockChanged(locked)
    }

    private enum class TouchMode {
        NONE,
        DRAG,
        RESIZE
    }
}
```

- [ ] **Step 2: Wire persistence in service**

Update `showOverlay()` in `OverlayService.kt`:

```kotlin
private var currentOrientation: ScreenOrientation? = null
private var currentRect: MaskRect? = null
private var currentLocked: Boolean = false

private fun showOverlay() {
    if (overlayView != null) return
    val bounds = currentScreenBounds()
    val orientation = OrientationResolver.resolve(bounds)
    val config = configStore.loadOrientationConfig(orientation, bounds)
    currentOrientation = orientation
    currentRect = config.rect
    currentLocked = config.locked

    val view = OverlayView(
        context = this,
        initialRect = config.rect,
        initialLocked = config.locked,
        onRectChanged = { rect ->
            val clamped = rect.clampTo(
                bounds = currentScreenBounds(),
                minWidth = OverlayConfigStore.MIN_MASK_WIDTH,
                minHeight = OverlayConfigStore.MIN_MASK_HEIGHT
            )
            currentRect = clamped
            windowController.update(clamped)
            saveCurrentConfig()
        },
        onLockChanged = { locked ->
            currentLocked = locked
            saveCurrentConfig()
        }
    ).apply {
        setMaskOpacity(configStore.getOpacity())
    }

    overlayView = view
    windowController.show(view, config.rect)
}

private fun saveCurrentConfig() {
    val orientation = currentOrientation ?: return
    val rect = currentRect ?: return
    configStore.saveOrientationConfig(
        orientation,
        OrientationConfig(rect = rect, locked = currentLocked, initialized = true)
    )
}
```

Add imports:

```kotlin
import com.submask.app.config.MaskRect
import com.submask.app.config.OrientationConfig
import com.submask.app.orientation.ScreenOrientation
```

- [ ] **Step 3: Build**

Run:

```powershell
.\gradlew.bat :app:assembleDebug
```

Expected: PASS.

- [ ] **Step 4: Manual check**

Expected:

- Unlocked mask drags.
- Resize handle changes width and height.
- Lock button disables drag and resize.
- Unlock enables drag and resize.
- Stop action removes overlay.
- Restart restores the last geometry for the current orientation.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/submask/app/overlay
git commit -m "feat: add interactive overlay mask"
```

---

### Task 9: Add Orientation Switching

**Files:**
- Modify: `app/src/main/java/com/submask/app/overlay/OverlayService.kt`
- Create: `app/src/test/java/com/submask/app/orientation/OrientationSwitchingTest.kt`

- [ ] **Step 1: Write failing pure orientation switching test**

Create `app/src/test/java/com/submask/app/orientation/OrientationSwitchingTest.kt`:

```kotlin
package com.submask.app.orientation

import com.submask.app.config.MaskRect
import com.submask.app.config.OrientationConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class OrientationSwitchingTest {
    @Test
    fun usesDefaultForUninitializedNewOrientation() {
        val landscapeDefault = OrientationResolver.defaultRect(
            ScreenOrientation.LANDSCAPE,
            ScreenBounds(1920, 1080)
        )
        val selected = OrientationStateSelector.select(
            orientation = ScreenOrientation.LANDSCAPE,
            bounds = ScreenBounds(1920, 1080),
            stored = OrientationConfig(
                rect = MaskRect(10, 10, 10, 10),
                locked = false,
                initialized = false
            )
        )
        assertEquals(landscapeDefault, selected.rect)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.submask.app.orientation.OrientationSwitchingTest"
```

Expected: FAIL because `OrientationStateSelector` does not exist.

- [ ] **Step 3: Add orientation selector**

Create `app/src/main/java/com/submask/app/orientation/OrientationStateSelector.kt`:

```kotlin
package com.submask.app.orientation

import com.submask.app.config.OrientationConfig
import com.submask.app.config.OverlayConfigStore

object OrientationStateSelector {
    fun select(
        orientation: ScreenOrientation,
        bounds: ScreenBounds,
        stored: OrientationConfig
    ): OrientationConfig {
        return if (stored.initialized) {
            stored.copy(
                rect = stored.rect.clampTo(
                    bounds,
                    OverlayConfigStore.MIN_MASK_WIDTH,
                    OverlayConfigStore.MIN_MASK_HEIGHT
                )
            )
        } else {
            stored.copy(rect = OrientationResolver.defaultRect(orientation, bounds))
        }
    }
}
```

- [ ] **Step 4: Run tests**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Expected: PASS.

- [ ] **Step 5: Wire configuration changes in service**

Add a configuration listener to `OverlayService.kt`:

```kotlin
override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
    super.onConfigurationChanged(newConfig)
    applyCurrentOrientation()
}

private fun applyCurrentOrientation() {
    val view = overlayView ?: return
    val bounds = currentScreenBounds()
    val newOrientation = OrientationResolver.resolve(bounds)
    if (newOrientation == currentOrientation) return

    saveCurrentConfig()

    val stored = configStore.loadOrientationConfig(newOrientation, bounds)
    val selected = OrientationStateSelector.select(newOrientation, bounds, stored)
    currentOrientation = newOrientation
    currentRect = selected.rect
    currentLocked = selected.locked
    view.setRect(selected.rect)
    windowController.update(selected.rect)
}
```

Add import:

```kotlin
import com.submask.app.orientation.OrientationStateSelector
```

- [ ] **Step 6: Build**

Run:

```powershell
.\gradlew.bat :app:assembleDebug
```

Expected: PASS.

- [ ] **Step 7: Manual orientation check**

Expected:

- Configure portrait mask.
- Rotate to landscape.
- Landscape uses landscape default if no landscape state exists.
- Configure landscape mask.
- Rotate back to portrait.
- Portrait geometry is restored.
- Rotate again to landscape.
- Landscape geometry is restored.
- Opacity remains shared.

- [ ] **Step 8: Commit**

```powershell
git add app/src/main/java/com/submask/app/orientation app/src/main/java/com/submask/app/overlay app/src/test/java/com/submask/app/orientation
git commit -m "feat: switch overlay geometry by orientation"
```

---

### Task 10: Final QA, Documentation, And Push

**Files:**
- Create: `README.md`
- Modify: `docs/superpowers/specs/2026-06-05-android-subtitle-blocker-design.md` only if implementation discovers a mismatch.

- [ ] **Step 1: Create README**

Create `README.md`:

```markdown
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
4. Start the mask.
5. Open Douyin, YouTube, or Bilibili.
6. Drag and resize the mask over subtitles.
7. Lock the mask.
8. Rotate between portrait and landscape and verify each orientation restores its own geometry.
9. Use the notification action to stop the mask.
```

- [ ] **Step 2: Run full verification**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

Expected: both commands PASS.

- [ ] **Step 3: Install and run final manual QA**

Run:

```powershell
.\gradlew.bat :app:installDebug
```

Manual expected results:

- Permission flow works.
- Foreground notification appears.
- Stop action works.
- Overlay appears above video apps.
- Drag works.
- Resize works.
- Lock works.
- Portrait and landscape state remain independent.
- No automatic reboot restore behavior exists.

- [ ] **Step 4: Commit**

```powershell
git add README.md
git commit -m "docs: add project readme"
```

- [ ] **Step 5: Push**

```powershell
git push
```

Expected: local `main` is pushed to `origin/main`.

---

## Self-Review Checklist

- Spec coverage:
  - Android 11+ only: Task 1 sets `minSdk = 30`.
  - Cross-app overlay: Tasks 5-8 implement overlay permission and `TYPE_APPLICATION_OVERLAY`.
  - Manual start: Task 5 starts service only from Start button.
  - Notification close: Task 6 adds Stop action.
  - Black opacity-controlled mask: Tasks 5, 7, and 8 wire opacity and black overlay view.
  - Drag/resize/lock: Task 8 implements interactions.
  - Separate portrait/landscape geometry: Tasks 2, 4, and 9 implement orientation-specific state.
  - No iOS, OCR, automatic detection, per-app config, built-in player, or boot restore: no task adds these features.
- Test coverage:
  - Pure orientation/default/clamping logic has JVM unit tests.
  - Overlay permission, WindowManager, foreground service, and third-party app behavior require manual Android verification.
- Dependency boundary:
  - Android framework code stays in Activity, Service, WindowController, View, and SharedPreferences store.
  - Geometry and orientation math remain pure Kotlin and testable.
