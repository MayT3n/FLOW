# MedFlow Windows Desktop

Windows 11 desktop edition of the existing Android-first FLOW / MatFocus project.

## Current implementation

- Compose Desktop application packaged as a Windows MSI.
- Persian RTL desktop shell with light and dark themes.
- Reuses the Android application's existing serializable data models, JSON store, repository, Pomodoro engine, Jalali date conversion, planner and statistics logic.
- Desktop-specific data is stored under `%APPDATA%\\MedFlow\\flow_data.json`; Android's private app directory is never touched.
- Task creation, completion and deletion are connected to persistent JSON storage.
- Focus timer start, pause, resume, reset, phase switching, skip, session recording and catch-up use the existing Pomodoro engine.
- Today's planner view and statistics are derived from stored user data.
- JSON backup export/import is available from Settings.
- Windows CI compiles Kotlin, runs desktop domain tests and packages the MSI.

## Build locally on Windows

Requirements: JDK 17 and WiX Toolset 3 (the Compose packaging task can download its required WiX distribution).

```powershell
.\\gradlew.bat :desktopApp:compileKotlin :desktopApp:test
.\\gradlew.bat :desktopApp:packageMsi
```

The MSI is written to `desktopApp/build/compose/binaries/main/msi/`.

## Explicitly incomplete

This is an incremental desktop port, not yet a feature-complete replacement for the Android app. The following remain to be implemented and validated:

- Full task editor: due date/time, priority, categories, search, filters and sorting.
- Routine and goal management UI.
- Full month calendar, offline Iranian occasions and calendar editing.
- Desktop audio playback, playlists, ambient audio and download management.
- Wallpaper library and custom wallpaper import.
- Windows notifications, tray integration, global shortcuts and launch-at-login.
- Native Windows Mica/Acrylic integration and a genuinely refractive Liquid Glass renderer.
- Performance profiling across integrated and discrete GPUs, high-DPI/multi-monitor validation, installer upgrade/uninstall tests and clean-machine smoke tests.

No claim of zero bugs or complete Windows integration is made until those items are implemented and tested on real Windows systems.
