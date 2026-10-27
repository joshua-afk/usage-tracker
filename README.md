# Usage Tracker

A personal Android app that records which apps you use and when (similar to Digital Wellbeing) and syncs the data to a server on your local network. It is sideloaded and not meant for the Play Store.

## What it does

- Reads app usage from Android's usage access and stores app sessions and screen unlocks locally (Room).
- Collects hourly in the background (WorkManager), with a checkpoint so no events are missed or duplicated.
- Shows today's screen time, unlock count and a per-app breakdown, plus a history view for any past day.
- Syncs unsynced records to a configurable URL in batches of at most 500, only on unmetered networks. Records are marked synced only after an HTTP 2xx, so nothing is lost if the server is unreachable.
- Exports all data, or a date range, as JSON through the share sheet.

## Requirements

- JDK 17 or newer (21 is used in CI)
- Android SDK with `platforms;android-37.0` and `build-tools;36.0.0`
- An Android 9 (API 28) or newer device

No Android Studio is needed. Point Gradle at the SDK by setting `ANDROID_HOME`, or by creating `local.properties` (gitignored):

```properties
sdk.dir=/path/to/Android/Sdk
```

## Build

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Run the unit tests with:

```bash
./gradlew testDebugUnitTest
```

CI (`.github/workflows/build.yml`) builds the debug APK on every push to `main`, on pull requests and on manual dispatch, and uploads it as the `usage-tracker-debug-apk` workflow artifact. You can download it from the run page instead of building locally.

## Install (sideload)

With USB debugging enabled on the phone:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or copy the APK to the phone (for example from the CI artifact) and open it. Android will ask you to allow installs from that source the first time.

## Grant usage access

The app cannot read usage data until you allow it:

1. Open the app. If access is missing, it shows an explanation and an **Open usage access settings** button.
2. In the system list, choose **Usage Tracker** and turn on **Permit usage access**.
3. Go back to the app. It re-checks automatically and opens the Today screen.

You can also find it under Settings > Apps > Special app access > Usage access (the exact path varies by manufacturer).

## Configure sync

Open the **Settings** tab and set:

- **Server URL**: the full endpoint, for example `http://192.168.1.20:3000/api/usage`. Plain HTTP to a LAN address is allowed.
- **Token**: sent as `Authorization: Bearer <token>`.

**Sync now** collects fresh events and uploads immediately. Automatic syncing runs hourly on unmetered networks (home Wi-Fi). Failed attempts retry with exponential backoff.

### Request body

The sync request and the exported file share one shape. The server should upsert by `id`, so re-sent records do not create duplicates, and return a 2xx only after the whole batch is stored.

```json
{
  "device_id": "<stable per-install UUID>",
  "sessions": [
    {
      "id": "uuid",
      "package": "com.example",
      "app_name": "Example",
      "started_at": "2026-10-27T09:15:02.000+08:00",
      "ended_at": "2026-10-27T09:21:47.000+08:00",
      "duration_seconds": 405
    }
  ],
  "unlocks": [{ "id": "uuid", "occurred_at": "2026-10-27T09:15:01.000+08:00" }]
}
```

A request carries at most 500 records in total (sessions plus unlocks).

## Export

In **Settings**, use **Export all data** or pick a From and To day and choose **Export date range**, then pick a target in the share sheet. Both bounds are inclusive local days.

## Release signing (later)

Release builds are not signed yet. To set it up:

1. Create a keystore (keep it out of git; `*.jks`, `*.keystore` and `keystore.properties` are already ignored):

   ```bash
   keytool -genkeypair -v -keystore usage-tracker.jks -keyalg RSA -keysize 2048 -validity 10000 -alias usage-tracker
   ```

2. Create `keystore.properties` next to the root `build.gradle.kts`:

   ```properties
   storeFile=/absolute/path/to/usage-tracker.jks
   storePassword=...
   keyAlias=usage-tracker
   keyPassword=...
   ```

3. In `app/build.gradle.kts`, read it and attach a `signingConfig` to the `release` build type:

   ```kotlin
   val keystoreProps = java.util.Properties().apply {
       val file = rootProject.file("keystore.properties")
       if (file.exists()) file.inputStream().use { load(it) }
   }

   android {
       signingConfigs {
           create("release") {
               storeFile = file(keystoreProps.getProperty("storeFile", "release.jks"))
               storePassword = keystoreProps.getProperty("storePassword")
               keyAlias = keystoreProps.getProperty("keyAlias")
               keyPassword = keystoreProps.getProperty("keyPassword")
           }
       }
       buildTypes {
           release { signingConfig = signingConfigs.getByName("release") }
       }
   }
   ```

4. Build with `./gradlew assembleRelease`.

To sign in CI, store the keystore as a base64 repository secret (`base64 -w0 usage-tracker.jks`), the passwords as secrets, and have the workflow decode the keystore and write `keystore.properties` before running `assembleRelease`. Do not commit the keystore or passwords.

Keep a backup of the keystore: an app signed with a different key cannot be installed over an existing install.
