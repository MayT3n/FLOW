# MatFocus

A Persian-first Pomodoro and productivity app built with Kotlin and Jetpack Compose. MatFocus 1.2 continues the existing FLOW application without changing its Android package identity or moving its existing user-data file.

## Compatibility and data preservation

- Android application ID: `com.flow.focus` (intentionally unchanged).
- Existing user data remains at `files/flow_data.json` in app-private storage.
- Existing timer, tasks, routines, calendar, statistics, wallpaper and theme models are retained.
- New settings fields have serialization defaults so older JSON data remains readable.
- The release version name is `1.2`. CI supplies monotonically increasing version codes as `1000 + GitHub Actions run number`.
- The launcher artwork is a custom Android VectorDrawable and the app label is MatFocus.

## Build

Requirements: JDK 17 and Android SDK 35.

```bash
./gradlew testDebugUnitTest
./gradlew assembleRelease -PversionName=1.2 -PversionCode=1027
```

For a local release-signed build, provide these environment variables (never commit the keystore):

- `FLOW_KEYSTORE_PATH`
- `FLOW_KEYSTORE_PASSWORD`
- `FLOW_KEY_ALIAS`
- `FLOW_KEY_PASSWORD`

Without a release keystore Gradle can create a debug-signed test APK. A debug-signed APK is **not** suitable for updating a production install.

## Audio catalogue and licences

### Music

The instrumental catalogue is sourced from [AERIS MUSIC's CC0 repository](https://github.com/farmmer938-lgtm/aeris-cc0-music). Its README and LICENSE declare the 13 original tracks to be dedicated to the public domain under CC0 1.0. Tracks are streamed over HTTPS and can be downloaded individually into app-private storage for offline playback. Media3 provides playback, MediaSession, background controls, focus handling and headset/noisy-route behaviour.

The third-party catalogue's licence statement is retained as the source-of-record; the app does not extract audio from Spotify, Apple Music or YouTube.

### Ambient field recordings

The catalogue includes source and licence metadata for each file:

| Item | Creator | Source | Licence |
|---|---|---|---|
| Rain (1) | ezwa | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Rain_(1).ogg) | Public Domain |
| Forest ambience | nille | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:20090610_0_ambience.ogg) | Public Domain |
| Howling wind | Tvabutzku1234 | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Howling_wind.ogg) | CC0 1.0 |
| Dry grass burning in open fireplace | ezwa | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Dry_grass_burning_in_open_fireplace.ogg) | Public Domain |
| Waves | Dsw4 | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Waves.ogg) | Public Domain |
| Ocean waves crushing | Luftrum | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Oceanwavescrushing.ogg) | CC BY 3.0 (attribution required) |

Ambient files are streamed or downloaded on demand to app-private storage. The forest source page describes noticeable background noise; the fireplace clip is about 25 seconds long and has not yet been verified for seamless looping. These are disclosed limitations, not claims of studio-mastered or already-tested audio.

## GitHub-hosted announcements

Edit `notifications/news.json` on the default branch to publish messages without changing Kotlin code.

Schema version 1:

```json
{
  "schemaVersion": 1,
  "updatedAt": "2026-11-11T00:00:00Z",
  "items": [
    {
      "id": "stable-unique-id",
      "title": "عنوان اطلاعیه",
      "body": "متن کامل پیام",
      "category": "News",
      "publishedAt": 1794355200000,
      "expiresAt": null,
      "enabled": true,
      "url": "https://example.com"
    }
  ]
}
```

Supported categories: `News`, `Update`, `Feature`, `Maintenance`, `Important`. `publishedAt` and `expiresAt` are Unix timestamps in milliseconds. Keep each `id` stable. Set `enabled` to false or add an expired timestamp to hide a message. The client validates schema version 1, caches the feed in DataStore and retains cached messages offline. It checks on app open, when the bell is opened and periodically through a network-constrained WorkManager job (12-hour interval; Android controls the actual execution time). This is pull-based polling, not instant push delivery.

## In-app update publishing

The single source of truth for the app update is `updates/latest.json`. The release workflow creates and publishes it when a version tag is pushed.

Example schema:

```json
{
  "schemaVersion": 1,
  "versionName": "1.2",
  "versionCode": 1027,
  "releaseDate": "2026-11-11",
  "changelog": ["MatFocus 1.2"],
  "downloadUrl": "https://github.com/MayT3n/FLOW/releases/download/v1.2/MatFocus-1.2-1027.apk",
  "fileSize": 12345678,
  "sha256": "64-character-hex-digest",
  "minimumSupportedVersion": 1,
  "mandatory": false,
  "updateAvailable": true
}
```

To publish a new update:

1. Update `version.txt` and the changelog in the commit.
2. Ensure the four GitHub Actions secrets below contain the **same production signing key and alias used by the installed app**.
3. Push a version tag such as `v1.3` after merging the release-ready commit to `main`.
4. The workflow runs tests, builds the APK, verifies its signature, computes byte size and SHA-256, creates a GitHub Release and updates `updates/latest.json` on `main`.

Required GitHub Actions secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

A tag build fails closed if the release keystore secrets are missing. A GitHub Actions success with a debug-signed test artifact is not a production update. Android's official package installer asks the user to confirm installation; silent installation is not attempted. Before installation, the app checks HTTPS, exact file size, SHA-256, package name, version code and signing certificate. The same package ID and certificate are required for an in-place update that preserves app-private data.

## Release validation

Automated CI tests/builds are necessary but do not replace device validation. In particular, audio quality and seamless-loop listening, lock-screen controls, real installation over an older production-signed APK, and preservation of data after the actual package replacement require a compatible Android device and the original signing keystore.
