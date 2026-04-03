# TLapz Video Journal

TLapz is a high-performance video journaling application built for Android. It focuses on privacy, efficient storage management, and a seamless capture experience on modern hardware.

---

## Core Features

### Storage Optimization
TLapz includes a built-in compression engine powered by the Media3 Transformer API. This allows for:
- **Automatic Compression**: Content can be optimized immediately after recording based on user-defined profiles.
- **Batch Processing**: Tools to recompress or merge videos by month or year to manage long-term storage.
- **Hardware Compatibility**: Specific architectural fixes for MediaTek and Mali GPUs to ensure stable SDR encoding and color accuracy.

### Video Capture
The camera system is built on CameraX with manual lifecycle management for stability.
- **Dynamic Resolution**: Supports multiple recording qualities including SD (480p) and HD (720p).
- **Forced SDR Pipeline**: Ensures videos are recorded in standard 8-bit dynamic range to avoid color-crushing and exposure issues on high-end HDR sensors.

### Privacy and Local Storage
TLapz is designed to keep your data private.
- **Local Storage**: All recordings and metadata remain strictly on the device. The app requires no internet access.
- **Folder Selection**: Utilizes the Storage Access Framework (SAF) to give users full control over where their data is stored, including external SD cards.

### Design
- **Material You Integration**: Dynamic color support based on the user's system theme.
- **Timeline View**: Simple, chronological grouping of entries for easy navigation.

---

## Technical Stack

- **UI**: Jetpack Compose
- **Camera**: CameraX (Manual Lifecycle)
- **Processing**: Media3 Transformer and ExoPlayer
- **Architecture**: MVI / MVVM with Clean Architecture
- **Dependency Injection**: Hilt
- **Database**: Room
- **Storage**: Scoped Storage / Storage Access Framework (SAF)

---

## Installation and Build

### Prerequisites
- Android Studio Ladybug or later
- Android 9.0 (API 28) or higher

### Build Instructions
1. Clone the repository: `git clone https://github.com/arpan-khan/TLapz.git`
2. Open the project in Android Studio.
3. Build using the Gradle wrapper: `./gradlew assembleDebug`

---

## License

This project is licensed under the MIT License.

---

