# Device Risk Checker

An Android library (`riskchecker`, built as an AAR) that checks a phone for security risks and
finds installed apps by permission, plus a small Jetpack Compose app (`samplehost`) that uses it.
Kotlin, minSdk 26, targetSdk 36, compileSdk 36.

## How to run

You need JDK 17 or newer (the one bundled with Android Studio works) and the Android SDK with
platform 36. If Gradle says the SDK is not found, open the folder once in Android Studio (it writes
`local.properties`) or set `ANDROID_HOME` to your SDK folder.

Windows, from this folder:

```
gradlew.bat assembleDebug
gradlew.bat :riskchecker:testDebugUnitTest
gradlew.bat :samplehost:installDebug
```

macOS / Linux, from this folder (the first line is needed because a zip made on Windows does not
keep the "executable" flag on `gradlew`):

```
chmod +x gradlew
./gradlew assembleDebug
./gradlew :riskchecker:testDebugUnitTest
./gradlew :samplehost:installDebug
```

- `assembleDebug` builds both modules. The AAR is `riskchecker/build/outputs/aar/riskchecker-debug.aar`
  and the app is `samplehost/build/outputs/apk/debug/samplehost-debug.apk`.
- `testDebugUnitTest` runs the library's unit tests on the JVM (no phone needed).
- `installDebug` installs the app on a connected phone or a running emulator. Then open **Risk Checker**.

Or open the folder in Android Studio and run the `samplehost` configuration.

## Project structure

```
Android_Sanjeet_Kumar/
├── settings.gradle.kts            Declares the two modules: riskchecker and samplehost
├── build.gradle.kts               Declares the Gradle plugins once, for both modules
├── gradle.properties              Gradle and AndroidX settings
├── gradle/libs.versions.toml      Every library and plugin version, in one place
├── gradlew, gradlew.bat, gradle/wrapper/   Gradle wrapper: builds with the right Gradle version
│
├── riskchecker/                   THE LIBRARY (AAR). All checking logic, no screens
│   ├── build.gradle.kts           Android library module; only dependency is androidx.annotation
│   ├── consumer-rules.pro         R8 rules handed to any host app that turns on minify
│   └── src/
│       ├── main/AndroidManifest.xml    <queries> for package visibility + the auto-scan provider
│       ├── main/java/com/bugsmirror/riskchecker/
│       │   ├── RiskChecker.kt          Public entry point: scan(), getAppsByPermissions(), openAppSettings()
│       │   ├── RiskReport.kt           Score, time and findings; toJson()
│       │   ├── Finding.kt              Finding data class, Severity and FindingId constants
│       │   ├── PermissionResult.kt     PermissionResult, AppInfo, MatchedPermission
│       │   ├── RiskCheckerAutoScan.kt  Bonus: holds the report of the scan started at app start
│       │   └── internal/               Not part of the public API
│       │       ├── PermissionLabels.kt     Friendly name -> permissions table, input parsing, matching
│       │       ├── RiskScore.kt            Score formula (100 - 30/15/5 per HIGH/MEDIUM/LOW, min 0)
│       │       ├── InstalledApps.kt        Wrapper around PackageManager (apps, labels, icons, system flag)
│       │       ├── AutoScanProvider.kt     Bonus: ContentProvider that starts a scan when the app starts
│       │       └── checks/
│       │           ├── RiskCheck.kt        RiskCheck interface + CheckRunner (turns a crash into CHECK_FAILED)
│       │           ├── SettingsChecks.kt   DEVELOPER_OPTIONS_ON, USB_DEBUGGING_ON, NO_SCREEN_LOCK
│       │           ├── AppChecks.kt        ACCESSIBILITY_APPS, OVERLAY_APPS
│       │           └── DeviceChecks.kt     Bonus: ROOTED_DEVICE, EMULATOR_DETECTED
│       └── test/java/...                   JVM unit tests: names mapping, score, JSON, CheckRunner, bonus checks
│
└── samplehost/                    THE SAMPLE APP. All screens
    ├── build.gradle.kts           App module; depends on project(":riskchecker"); Compose + Material 3
    ├── proguard-rules.pro         Empty: the library brings its own rules
    └── src/main/
        ├── AndroidManifest.xml    Only the app and its Activity. No permissions, no <queries>
        ├── res/                   Strings, launcher icon, window theme
        └── java/com/bugsmirror/samplehost/
            ├── MainActivity.kt         The only Activity; shows RiskCheckerApp
            ├── MainViewModel.kt        Calls the library (search on Dispatchers.IO) and holds the UI state
            ├── MainUiState.kt          Screen state: scan state, search text, search status
            └── ui/
                ├── RiskCheckerApp.kt   Top bar + two tabs; shows a snackbar if App info cannot open
                ├── ScanScreen.kt       Scan button, score, findings coloured by severity, JSON view
                ├── AppSearchScreen.kt  Search box, "Not recognised" message, app list, tap -> App info
                └── theme/Theme.kt      Material 3 theme and the severity colours
```

### Flow

1. **Scan:** the Scan button calls `MainViewModel.scan()`, which calls `RiskChecker.scan { }`.
   The library runs every check on its own background thread through `CheckRunner`, which catches
   each check on its own. It works out the score and posts the `RiskReport` to the main thread.
   The ViewModel puts it in a `StateFlow` and Compose redraws.
2. **Search:** the ViewModel splits the text on commas and calls `getAppsByPermissions` on
   `Dispatchers.IO`. The library parses the names (`PermissionLabels`), reads the visible apps with
   their requested permissions (`InstalledApps`) and returns the matching ones and the unknown names.
3. **Tap an app:** `openAppSettings(packageName)` checks that the app exists, then starts the
   system App info screen. If it returns false, the app shows a snackbar.
4. **At app start (bonus):** Android creates `AutoScanProvider` from the library's manifest before
   the app's own code runs. It starts one scan, and the sample app shows that result until you scan yourself.

## What works and what does not

**Works**
- Both modules build with `gradlew.bat assembleDebug`, and the AAR appears in `riskchecker/build/outputs/aar/`.
- All the Part A rules: friendly names, case and extra spaces ignored, any `android.permission.X`,
  unknown names returned in `unknownLabels` and never an error, name/package/icon/matched names/system flag.
- All five Part B checks with the exact ids, severities, score formula and JSON keys, plus CHECK_FAILED for each check that throws.
- `scan()` runs on a background thread and calls back on the main thread. The search runs off the main thread.
- The library declares `<queries>` instead of the restricted "see all apps" permission, and the
  host manifest is empty (checked in the merged manifest).
- Bonus: 27 unit tests (all pass), `consumer-rules.pro` (a minified release build of the sample app builds),
  granted vs. requested per permission, root and emulator checks (LOW), auto-scan at start-up through a ContentProvider.

**Does not / limits**
- On Android 11+ the library cannot see apps without a launcher icon (see Question 1), so their
  permissions and overlays are missed.
- OVERLAY_APPS reports apps that *request* "Draw over other apps", not only the ones the user has
  allowed. That permission is switched on in a special settings screen, and the "granted" flag
  shown for it is not reliable.
- App info opens in the Settings app's own task. If Settings was already open (for example after
  changing the screen lock), Back can go to the previous Settings page before returning to our app.
- The emulator and root checks are heuristics. They can be fooled and can give false alarms.

## How AI helped

- Tools I used: Claude Code (Claude Opus) inside VS Code.
- Two prompts I used (paste them as you typed them):
  1. "Act as a Senior Android Engineer. Build a complete, production-grade Android project in the current workspace directory (`Android_Sanjeet_Kumar`) implementing the **Device Risk Checker** specification below. …" (followed by the build rules and the API from the task)
  2. "tell me how to use this android app in mobile ... cause i have to screen record this as well"
- One thing the AI got wrong, and how I found and fixed it: in `RiskReportTest` the AI checked the
  JSON keys with `JSONObject.keySet()`. That method exists in the desktop org.json library but not in
  Android's `JSONObject`, and unit tests compile against Android's version. I found it when
  `gradlew.bat :riskchecker:testDebugUnitTest` failed with `Unresolved reference 'keySet'`. I fixed
  it with a small helper that uses `keys()`, which both versions have.
- One part I wrote or changed myself without AI: TODO

## Questions

**1. Which apps can your library NOT see on Android 11 and newer, and why?**

Android 11 added *package visibility*: `PackageManager` only returns apps that the caller declares
in `<queries>`, plus a few that are always visible. The library does not use the restricted
"see all apps" permission. Instead it declares two queries in
[riskchecker/src/main/AndroidManifest.xml](riskchecker/src/main/AndroidManifest.xml): apps with a
launcher activity (MAIN/LAUNCHER) and apps with an accessibility service. So it cannot see:
- apps with no launcher icon and no accessibility service: background-only apps, plugins, add-ons,
  and many system components. This includes spyware that hides its icon, which is a real gap for a security tool;
- apps in another user or in the work profile.

For those apps `getAppsByPermissions` returns nothing and OVERLAY_APPS cannot list them.

**2. Which of your checks could give a false alarm on a normal person's phone? Why?**

- **ACCESSIBILITY_APPS (HIGH):** password managers, screen readers, automation apps (like Tasker)
  and some launchers use accessibility for good reasons, but they cost 30 points.
- **OVERLAY_APPS:** it counts apps that *request* the permission. Messengers with chat bubbles,
  caller-ID apps and screen dimmers request it legitimately, and many never get it granted.
- **DEVELOPER_OPTIONS_ON / USB_DEBUGGING_ON:** people turn these on once (for example to change
  animation speed or to follow a tutorial) and forget. That is a risk, but not an attack.
- An accessibility service whose app we cannot look up is reported by its package name, because
  we cannot tell whether it is a system app. That can flag a hidden system component.
- **Bonus checks:** the emulator check looks at `Build` values that some custom ROMs share.
  The root check can fire on custom ROMs that ship a `su` binary.

**3. If one check throws an exception, what happens in your code? Point to the lines.**

- Each check runs inside its own `try` in `CheckRunner.runAll`
  ([RiskCheck.kt:21-30](riskchecker/src/main/java/com/bugsmirror/riskchecker/internal/checks/RiskCheck.kt#L21-L30)).
  The `catch (t: Throwable)` logs the error and replaces that check's result with one
  `CHECK_FAILED` finding (LOW, -5 points), built by `checkFailed`
  ([RiskCheck.kt:36-45](riskchecker/src/main/java/com/bugsmirror/riskchecker/internal/checks/RiskCheck.kt#L36-L45)).
  Its `details` name the check, for example `NO_SCREEN_LOCK check failed (SecurityException: …)`.
  The loop then continues, so the other checks still run.
- `RiskChecker.runChecks` has a second guard around the whole run
  ([RiskChecker.kt:99-111](riskchecker/src/main/java/com/bugsmirror/riskchecker/RiskChecker.kt#L99-L111)),
  so nothing can escape on the background thread (which would crash the app). The report is
  always posted to the main thread ([RiskChecker.kt:45-50](riskchecker/src/main/java/com/bugsmirror/riskchecker/RiskChecker.kt#L45-L50)).
- The other public methods follow the same rule: `getAppsByPermissions` returns an empty app list
  if reading the apps fails, and skips any single app that fails
  ([RiskChecker.kt:64-69](riskchecker/src/main/java/com/bugsmirror/riskchecker/RiskChecker.kt#L64-L69),
  [RiskChecker.kt:128-133](riskchecker/src/main/java/com/bugsmirror/riskchecker/RiskChecker.kt#L128-L133)).
  `openAppSettings` returns `false`
  ([RiskChecker.kt:79-94](riskchecker/src/main/java/com/bugsmirror/riskchecker/RiskChecker.kt#L79-L94)).
- `CheckRunnerTest` proves it: a check that throws becomes one CHECK_FAILED, and the next check still runs.

## Video

TODO: link to the screen recording (Google Drive or unlisted YouTube, "anyone with the link can view").
