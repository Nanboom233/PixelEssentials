# Wireless Debugging Launcher Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a minimal Android Java app that exposes only one launcher icon and opens the Wireless Debugging settings page via root.

**Architecture:** A transparent launcher activity dispatches to a use case that executes a validated shell command through `su -c`. Command composition and root execution are isolated so future tile/widget/notification entry points can reuse them.

**Tech Stack:** Android Gradle Plugin, Gradle Wrapper, Java, AndroidX, compileSdk 36, minSdk 26

---

### Task 1: Initialize Android project skeleton

**Files:**
- Create: `settings.gradle`
- Create: `build.gradle`
- Create: `gradle.properties`
- Create: `local.properties`
- Create: `app/build.gradle`
- Create: `app/proguard-rules.pro`
- Create: `app/src/main/AndroidManifest.xml`

**Step 1: Create root Gradle settings**

Create `settings.gradle` with project name and `:app` include.

**Step 2: Create root build files**

Add plugin management and repository configuration in `build.gradle`/`gradle.properties`.

**Step 3: Create app module build file**

Set namespace/applicationId, compileSdk 36, minSdk 26, targetSdk 36, and Java 17+ compatibility.

**Step 4: Add local SDK path**

Set `sdk.dir=D\\:\\Android_SDK` in `local.properties` unless user corrects it.

**Step 5: Verify file consistency**

Check that all root/app Gradle files reference the same namespace and SDK levels.

### Task 2: Add manifest and launcher entry

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`

**Step 1: Declare application**

Add minimal `<application>` block with icon/label placeholders.

**Step 2: Declare `ShortcutDispatchActivity`**

Set:
- `android:exported="true"`
- `android:noHistory="true"`
- `android:excludeFromRecents="true"`
- transparent theme

**Step 3: Add launcher intent filter**

Add `MAIN` + `LAUNCHER` so the app exposes only one icon.

**Step 4: Verify no extra UI components**

Ensure manifest contains no additional activities for app UI.

### Task 3: Add resources for invisible launcher flow

**Files:**
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/values/colors.xml`
- Create: `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`

**Step 1: Add app name and error strings**

Create strings for app label and root failure fallback.

**Step 2: Add transparent theme**

Create a launcher theme with transparent window background and no visible UI chrome.

**Step 3: Add basic launcher icon resources**

Use a simple placeholder adaptive icon so the project builds.

### Task 4: Implement command and execution core

**Files:**
- Create: `app/src/main/java/com/nanboom/pixelessentials/root/ExecResult.java`
- Create: `app/src/main/java/com/nanboom/pixelessentials/root/RootExecutor.java`
- Create: `app/src/main/java/com/nanboom/pixelessentials/root/ProcessRootExecutor.java`
- Create: `app/src/main/java/com/nanboom/pixelessentials/wireless/WirelessDebuggingCommandProvider.java`
- Create: `app/src/main/java/com/nanboom/pixelessentials/wireless/OpenWirelessDebuggingUseCase.java`

**Step 1: Add `ExecResult` model**

Represent success, exitCode, stdout, stderr.

**Step 2: Add `RootExecutor` interface**

Define a synchronous execution contract for the first version.

**Step 3: Add `ProcessRootExecutor`**

Implement `su -c <command>` with `ProcessBuilder`.

**Step 4: Add command provider**

Return the validated `am start -n com.android.settings/.SubSettings ...` command.

**Step 5: Add use case**

Wire command provider + root executor into one method.

### Task 5: Implement transparent launcher activity

**Files:**
- Create: `app/src/main/java/com/nanboom/pixelessentials/ShortcutDispatchActivity.java`

**Step 1: Create activity class**

Subclass `Activity` or `AppCompatActivity`.

**Step 2: Trigger use case in `onCreate()`**

Run the use case once and finish immediately afterward.

**Step 3: Add minimal failure feedback**

On failure, show a short `Toast` and log stderr to Logcat.

**Step 4: Prevent duplicate launches**

Guard against repeated execution on configuration changes or re-entry.

### Task 6: Add focused tests / validation artifacts

**Files:**
- Create: `app/src/test/java/com/nanboom/pixelessentials/wireless/WirelessDebuggingCommandProviderTest.java`
- Create: `app/src/test/java/com/nanboom/pixelessentials/wireless/OpenWirelessDebuggingUseCaseTest.java`
- Create: `scripts/test-launcher-flow.ps1`

**Step 1: Test command provider**

Assert the generated command contains `SubSettings` and `AdbWirelessDebuggingFragment`.

**Step 2: Test use case**

Mock/stub `RootExecutor` and assert result propagation.

**Step 3: Add verification script**

Create a PowerShell script that runs unit tests and, if build succeeds, prints the APK path.

### Task 7: Build and verify

**Files:**
- Modify as needed: all above files

**Step 1: Add Gradle wrapper or bootstrap strategy**

Create/restore wrapper files required to run `gradlew.bat`.

**Step 2: Run unit tests**

Run the focused test task or full JVM test task.

**Step 3: Build debug APK**

Run debug build and verify APK output path.

**Step 4: Record results**

Save build/test status in vibe phase receipt.
