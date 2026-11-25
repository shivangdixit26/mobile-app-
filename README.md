# Device Risk Checker (Android)

An Android library (`riskchecker`, built as an AAR) that checks a phone for security risks and finds
installed apps by permission, and a Jetpack Compose sample app (`samplehost`) that uses it.

This repository holds two Android Studio projects. Open **one folder at a time**, not the repository root.

| Folder | What it is |
|---|---|
| [`Android_Sanjeet_Kumar/`](Android_Sanjeet_Kumar/) | The project as submitted for the assessment. Its README answers the task's questions |
| [`Android_Sanjeet_Kumar_AppleUI/`](Android_Sanjeet_Kumar_AppleUI/) | The same project with extra design effects in the sample app only (score ring, scroll-edge header, motion, haptics). The library is unchanged. See the "Design effects" section of its README |

Both use the same app id, so installing one replaces the other on a phone.

## Run it on a Mac with Android Studio

1. Get the code:
   ```
   git clone https://github.com/shivangdixit26/mobile-app-.git
   ```
2. In Android Studio: **File > Open**, choose the `Android_Sanjeet_Kumar` folder (or the AppleUI one), then **Open**.
   Trust the project when asked.
3. Wait for the first **Gradle sync**. It downloads Gradle 8.14.3 and the libraries, so it needs internet
   the first time and can take a few minutes.
   - If Android Studio asks to install **Android SDK Platform 36**, accept.
   - If it offers to **upgrade the Android Gradle Plugin**, choose *Don't remind me for this project*.
     The project uses AGP 8.13.2 on purpose: newer AndroidX versions need compileSdk 37, and the task
     requires compileSdk 36.
4. Pick a device (a phone with USB debugging on, or an emulator from **Device Manager**) and click
   **Run** with the `samplehost` configuration.

From a terminal instead, inside one of the two folders:

```
./gradlew assembleDebug                    # builds both modules (AAR + APK)
./gradlew :riskchecker:testDebugUnitTest   # runs the 27 unit tests
./gradlew :samplehost:installDebug         # installs on a connected phone or running emulator
```

Requirements: Android Studio with its bundled JDK (or any JDK 17+), and the Android SDK with platform 36.

## If the first Gradle sync fails

- **"Could not install Gradle distribution … Connection refused" from services.gradle.org:** some networks
  block gradle.org. The wrapper therefore downloads Gradle from Gradle's own GitHub mirror
  (`gradle/wrapper/gradle-wrapper.properties`, `distributionUrl`). The `distributionSha256Sum` line
  checks that it is the official file. Don't edit these lines by hand; one wrong character gives a 404.
- **"Connect timed out" (often on IPv6-only Wi-Fi):** in Android Studio, open **Help > Edit Custom VM
  Options…**, add the line `-Djava.net.preferIPv6Addresses=true`, restart Android Studio and sync
  again. Or switch to another network, such as a phone hotspot.
- **A library download fails** (`dl.google.com`, `repo1.maven.org`): switch network and click **Try Again**.
