# VirtualSpace - Android App Virtualization Framework

A complete, self-built app virtualization framework for Android supporting Android 9-16 (SDK 28-35).

## Features

- **Complete Virtualization Engine**: Self-built framework, no third-party virtualization libraries required
- **Multi-Instance Support**: Clone apps and run multiple accounts simultaneously
- **Device Spoofing**: Hide real device identity from cloned apps
- **PackageManager Hook**: Intercept and spoof package information
- **ActivityManager Hook**: Manage virtual activity lifecycle
- **Native Hook (C/C++)**: PLT/GOT hooking for system service interception
- **Service Proxy**: Intercept and redirect system services

## Architecture

```
VirtualSpace/
├── app/                      # Host application
│   ├── src/main/
│   │   ├── java/com/virtual/
│   │   │   ├── app/          # Application UI components
│   │   │   ├── core/         # Core virtualization engine
│   │   │   │   ├── am/       # ActivityManager proxy
│   │   │   │   ├── pm/       # PackageManager proxy
│   │   │   │   ├── dispatch/ # Intent dispatch system
│   │   │   │   ├── service/  # Service broker
│   │   │   │   └── context/  # Virtual context
│   │   │   └── hook/         # Hook management
│   │   └── res/              # Resources
│   └── build.gradle
│
└── hook/                     # Native hook module
    ├── src/main/
    │   ├── cpp/              # C/C++ native code
    │   └── java/             # Native hook Java interface
    └── build.gradle
```

## Supported Android Versions

- Android 9 (Pie) - SDK 28
- Android 10 (Q) - SDK 29
- Android 11 (R) - SDK 30
- Android 12 (S) - SDK 31/32
- Android 13 (T) - SDK 33
- Android 14 (U) - SDK 34
- Android 15 (V) - SDK 35

## Build Instructions

### Prerequisites

- Android Studio Hedgehog or later
- Android SDK with NDK
- JDK 17 or later

### Build Steps

1. Open the project in Android Studio:
   ```
   File → Open → Select VirtualSpace folder
   ```

2. Wait for Gradle sync to complete

3. Build the debug APK:
   ```
   Build → Build Bundle(s) / APK(s) → Build APK(s)
   ```

4. The APK will be generated at:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

### Command Line Build

```bash
cd VirtualSpace
./gradlew assembleDebug
```

## Key Components

### VirtualCore
Main engine that manages virtual apps and packages.

### VirtualDispatch
Intercepts and redirects Intents to virtual apps.

### ServiceBroker
Hooks system services (AMS, PMS, etc.) and provides virtual responses.

### Native Hook (C/C++)
Provides low-level PLT/GOT hooking for intercepting system library functions.

### HookManager
Java-side hook management and method proxying.

## Usage

1. Install the APK on your Android device
2. Launch VirtualSpace
3. Browse installed apps
4. Select an app to clone
5. The cloned app will appear in the "Cloned Apps" tab
6. Open cloned apps directly from VirtualSpace

## Limitations

- Requires QUERY_ALL_PACKAGES permission on Android 11+
- Some system apps may have additional protection
- Root access not required (limited functionality)

## License

MIT License

## Credits

Built with Android SDK, Material Components, and native Linux hooking techniques.
