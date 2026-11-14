# MedFlow Desktop

This module is the first Windows desktop foundation for the existing Android application.

## Current status

- Dedicated Compose Multiplatform desktop entry point.
- Desktop-first window layout with RTL Persian navigation.
- Initial dark/light theme switching.
- Windows MSI and EXE packaging configuration.

The dashboard currently uses explicit migration-status copy for features that are not connected yet. It does not pretend that Android-only services are already available on desktop.

## Build on Windows

Requirements: JDK 17.

```powershell
./gradlew.bat :desktopApp:run
./gradlew.bat :desktopApp:packageMsi
./gradlew.bat :desktopApp:packageExe
```

## Next migration steps

1. Extract platform-independent models, Jalali date logic, Pomodoro engine, planner and statistics into shared Kotlin modules.
2. Add desktop persistence with backward-compatible JSON import/export.
3. Implement desktop notifications, audio playback, file dialogs and system tray.
4. Replace the initial visual shell with connected feature screens.
5. Profile and implement a measured glass rendering pipeline.
