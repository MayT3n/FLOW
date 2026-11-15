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

## Desktop UI implementation status

The Windows desktop branch now includes:

- Responsive Persian RTL workspace shell, expandable/collapsible right navigation and native Windows title bar.
- Separate light and dark palettes with shared typography, spacing, shapes and surface tokens.
- Bundled Vazirmatn Regular/Bold Persian typography and its OFL license; the installed app does not need a network connection to render Persian text.
- A floating, layered glass navigation rail, refined line-icon set, calmer toolbar, and circular focus progress dial.
- Four distinct material profiles. Performance and balanced use Mica; quality and ultra request the Windows DWM Acrylic backdrop and add stronger surface tint, edge reflection and elevation.
- Responsive dashboard, focus timer with phase/status/progress feedback, persistent tasks, routines, goals, Jalali calendar, analytics, music library, playlists and settings.
- A contextual mini-player while a track is selected.
- Wallpaper selection/import, Windows startup integration, tray integration and JSON backup/restore.

The material treatment combines translucent, layered Compose surfaces with the native *window-level* DWM backdrop. This is a deliberate performance trade-off: it avoids capturing and blurring the desktop every frame. It is not a physically refractive, independently blurred panel renderer. Acrylic support depends on the Windows build and can fall back to Mica; no claim is made that every GPU produces identical results.

## Validation boundaries

GitHub Actions compiles the desktop module, runs desktop domain tests, packages the MSI, installs it, launches the installed executable for a smoke test and uninstalls it. This is not a substitute for human visual review at 1366×768, 1920×1080, 2560×1440 and 4K on real Windows hardware.

Still requiring device-level validation or further work:

- Visual comparison and performance profiling across integrated and discrete GPUs, high-DPI and multi-monitor setups.
- MSI upgrade/uninstall behavior with an existing user-data directory.
- A dedicated ambient-sound mixer and a physically refractive, per-surface glass renderer.
- Visual comparison and interaction review on real hardware after the refreshed UI is installed.
