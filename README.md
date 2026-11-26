# School

An Android TV app for browsing and watching [Stabrise School](https://school.stabrise.com/) video lessons with a TV remote. 

## Features

- Subjects in a side navigation drawer
- Topic filters and search within the selected subject
- Lesson list that loads more as you scroll (20 per page)
- Saved subjects, topics, and first lesson pages shown at once when available, then refreshed from the server
- Full-screen YouTube playback with TV-friendly controls
- Screen stays awake while the app is open

## Requirements

- Android Studio, or the Android SDK with platform 37.1
- JDK 17 or newer
- An Android TV device or emulator running Android 8.0 (API 26) or newer
- A network connection

The Gradle wrapper is included, so you don't need to install Gradle.

## Build and run

Open the project in Android Studio and run the `app` configuration, or use the command line:

```sh
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

For command-line builds, the SDK location must be set in `local.properties` (`sdk.dir=...`) or in the `ANDROID_HOME` environment variable. Android Studio creates `local.properties` for you.

`./gradlew :app:assembleRelease` produces an unsigned APK, because no release signing is configured.

## Controls

### Browsing

- Open the side drawer to switch subjects.
- Select a topic chip to filter lessons, or **All Topics** to remove the filter.
- Select the search field to type, then press the keyboard's search key. **Clear search** resets it.
- Select a lesson card to play its video.

### Player

| Remote key | Controls hidden | Controls shown |
| --- | --- | --- |
| OK, Up, Down | Show controls | Use or move between controls |
| Left, Right | Seek and show controls | Move between buttons; seek when the progress line is focused |
| Rewind, Fast-forward | Seek | Seek |
| Play, Pause, Play/Pause | Control playback | Control playback |
| Back | Close the player | Hide controls |

A single press seeks 5 seconds. Holding the key seeks continuously and speeds up over time. Controls hide after 4 seconds during playback.

With touch or a mouse, click the video to show or hide the controls, and click the progress line to seek.

## Backend

The app reads from `https://api-school.stabrise.com/`:

| Endpoint | Returns |
| --- | --- |
| `GET api/public/subjects` | Subjects |
| `GET api/public/subjects/{subject_id}/lesson-topics` | Topics of a subject |
| `GET api/public/subjects/{subject_id}/lessons-page` | A page of lessons (`topic_id`, `q`, `limit`, `offset`) |
| `GET api/public/lessons/{lesson_id}` | Lesson details |

The base URL is set in `SchoolApi.kt`.

The API has no dedicated video field. The app takes the first YouTube link it finds in the lesson content, then in the materials, then in the task content. A lesson without such a link shows "This lesson has no video".

## Project structure

Sources are in `app/src/main/java/com/stabrise/school/`:

| Path | Purpose |
| --- | --- |
| `MainActivity.kt` | Entry point and browsing UI |
| `data/SchoolApi.kt` | Retrofit service and HTTP cache |
| `data/SchoolModels.kt` | API models and YouTube link extraction |
| `ui/SchoolTvViewModel.kt` | Screen state and loading |
| `ui/VideoPlayer.kt` | YouTube player and playback controls |
| `ui/theme/` | Colors, typography, and theme |

## Tech stack

Kotlin, Jetpack Compose, Compose for TV (Material 3), Retrofit with Gson, Coil, and a WebView running the YouTube IFrame API. Library versions are listed in `gradle/libs.versions.toml`.
