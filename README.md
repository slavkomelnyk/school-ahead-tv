# School TV

School TV brings the School website to Android TV, with remote friendly video playback controls for supported web players.

## Getting started

1. Install School TV on an Android TV device or emulator.
2. Open **School TV** from the TV home screen.
3. Browse the School website with the remote. When a supported video is detected, playback controls become available.

The website address is set for each app build. See the [development guide](DEVELOPMENT.md#configure-the-start-website) if you maintain the app and need to change it.

## Remote controls

| Button | Action |
| --- | --- |
| MENU | Show or hide video controls |
| OK / D-pad center | Select the focused control. In fullscreen, show hidden controls. |
| Play/Pause | Toggle video playback |
| Rewind / Fast-forward | Seek backward or forward by 10 seconds |
| Left / Right | Seek by 10 seconds when controls are hidden; move between focused controls when visible |
| BACK | Hide visible controls. Press BACK again within 2 seconds to minimize the player. Otherwise, BACK exits fullscreen or returns to the previous website page when possible. |

The player controls supported HTML5 video and YouTube. Playback features may vary with the website’s embedded player.

## More information

- [Development guide](DEVELOPMENT.md): project setup, configuration, build, and release steps.
- [Version history](VERSIONS.md): app versions and release notes.
