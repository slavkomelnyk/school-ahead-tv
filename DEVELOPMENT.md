# Development guide

This guide covers setup, configuration, builds, and releases for School TV. For end user instructions, see the [README](README.md).

## Requirements

- Android Studio with Android SDK Platform 35 installed.
- JDK 17.
- Android TV device or emulator running Android 8.0 (API 26) or newer.

The project includes a Gradle wrapper; a separate Gradle installation is not required.

## Project layout

| Path | Purpose |
| --- | --- |
| `app/src/main/java/com/ahead/school/MainActivity.kt` | App entry point and app-wide remote key routing |
| `app/src/main/java/com/ahead/school/ui/screens/web/TvWebViewScreen.kt` | WebView, navigation, and screen state |
| `app/src/main/java/com/ahead/school/bridge/TvMediaBridge.kt` | JavaScript bridge for HTML5 and YouTube playback |
| `app/src/main/java/com/ahead/school/ui/screens/media/MediaOverlay.kt` | TV playback controls and progress display |
| `app/src/main/res/values/web_config.xml` | Start website URL |

## Configure the start website

Edit `start_website_url` in `app/src/main/res/values/web_config.xml`, then rebuild the app. Use a complete HTTPS URL.

## Build and install

Build and check the project:

```sh
./gradlew build
```

Build only the debug APK:

```sh
./gradlew :app:assembleDebug
```

Install it on a connected Android TV device or emulator:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Remote key behavior

Remote key events are routed at the activity level so playback keys work whether focus is on the WebView or a Compose control. The WebView bridge performs playback and seek commands in the page.

- MENU toggles video controls.
- OK reveals hidden controls in fullscreen; when controls are visible, OK activates the focused control.
- Play/Pause toggles playback. Rewind and fast-forward seek by 10 seconds.
- Left and Right seek when controls are hidden and navigate the control focus when visible.
- BACK hides visible controls. A second BACK within 2 seconds minimizes the player; otherwise, BACK exits fullscreen or navigates backward in the website when possible.

## Manual builds and releases

This repository does not use GitHub Actions. Build APKs locally, then create beta or stable releases manually.

### Create a beta prerelease

1. Build the debug APK:

   ```sh
   ./gradlew :app:assembleDebug
   ```

2. The APK is at `app/build/outputs/apk/debug/app-debug.apk`. Optionally copy it to a versioned filename such as `School-TV-beta-v1.0.0-beta.1.apk`.
3. Create and push a beta tag. Use the `beta-` prefix to distinguish it from stable release tags:

   ```sh
   git tag -a beta-v1.0.0-beta.1 -m "Beta v1.0.0-beta.1"
   git push origin beta-v1.0.0-beta.1
   ```

4. In GitHub, create a new release from that tag, mark it as a prerelease, attach the debug APK, and publish it.

### Configure release signing

Create a release keystore and keep it outside the repository. For example:

```sh
keytool -genkeypair -v -keystore school-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias school
```

Do not commit the keystore or passwords. Before building a stable release, provide these Gradle properties through `ORG_GRADLE_PROJECT_` environment variables or a secure, untracked local Gradle properties file:

- `RELEASE_STORE_FILE`: absolute path to the keystore.
- `RELEASE_STORE_PASSWORD`: keystore password.
- `RELEASE_KEY_ALIAS`: key alias.
- `RELEASE_KEY_PASSWORD`: key password.

For example, set the values in your local shell as `ORG_GRADLE_PROJECT_RELEASE_STORE_FILE`, `ORG_GRADLE_PROJECT_RELEASE_STORE_PASSWORD`, `ORG_GRADLE_PROJECT_RELEASE_KEY_ALIAS`, and `ORG_GRADLE_PROJECT_RELEASE_KEY_PASSWORD`. Without all four properties, Gradle creates an unsigned release APK.

### Create a stable draft release

1. Update `versionCode` and `versionName` in `app/build.gradle.kts`. Increase `versionCode` for every distributed package.
2. Add release notes to `VERSIONS.md`.
3. Build the signed release APK:

   ```sh
   ./gradlew :app:assembleRelease
   ```

   The APK is at `app/build/outputs/apk/release/app-release.apk`.

4. Commit the version and release notes, create a `v`-prefixed tag, and push it. For example:

   ```sh
   git add app/build.gradle.kts VERSIONS.md
   git commit -m "Prepare release v1.0.1"
   git tag -a v1.0.1 -m "Release v1.0.1"
   git push origin main
   git push origin v1.0.1
   ```

5. In GitHub, create a new release from tag `v1.0.1`, attach the signed APK, save it as a draft for review, and publish it when ready.

See the [version history](VERSIONS.md) for the current project version and past release notes.
