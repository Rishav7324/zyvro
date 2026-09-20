# Installation & Setup Guide 📲

This guide walks you through downloading, installing, and configuring **Zyvro** on your Android device or building it from source.

---

## 📥 Option 1: Install Pre-Built APK (Recommended)

### Step 1: Download the APK
Navigate to the [GitHub Releases Page](https://github.com/Rishav7324/zyvro/releases) and download the latest version:
* **`Zyvro-vX.X.X-release.apk`**: Production-ready, optimized, and recommended for daily use.
* **`Zyvro-vX.X.X-debug.apk`**: Developer build containing extended debugging logs.

### Step 2: Allow Unknown Sources
If this is your first time installing an APK outside Google Play:
1. Tap on the downloaded APK notification or find it in your **Files** / **Downloads** app.
2. If prompted with *"For your security, your phone is not allowed to install unknown apps from this source"*:
   - Tap **Settings**.
   - Enable **Allow from this source**.
3. Tap **Install**.

---

## 🔐 Permissions Overview

When launching Zyvro for the first time, you will see a permission onboarding screen. Here is why each permission is needed:

| Permission | Android Version | Purpose |
| :--- | :--- | :--- |
| **Notifications** (`POST_NOTIFICATIONS`) | Android 13+ (API 33+) | Displays active download speed, progress percentage, and background lockscreen media controls. |
| **Media Images** (`READ_MEDIA_IMAGES`) | Android 13+ (API 33+) | Allows accessing downloaded Instagram/Pinterest photos and saving thumbnails. |
| **Media Video** (`READ_MEDIA_VIDEO`) | Android 13+ (API 33+) | Allows scanning and playing downloaded videos in the Library. |
| **Media Audio** (`READ_MEDIA_AUDIO`) | Android 13+ (API 33+) | Allows playing extracted audio tracks, creating playlists, and trimming ringtones. |
| **External Storage** (`WRITE_EXTERNAL_STORAGE`) | Android 9 and below | Required on older Android versions for saving files to `/sdcard/Download`. |
| **Modify System Settings** (`WRITE_SETTINGS`) | All | **Optional** permission: Only requested when you choose "Set as Default Phone Ringtone" in the Audio Cutter dialog. |

---

## 🛠️ Option 2: Building from Source

### Prerequisites
* **Android Studio Ladybug (2024.2+)** or later
* **JDK 21** (Temurin or OpenJDK recommended)
* **Android SDK Platform 35**
* **Git** installed on your command line

### Step-by-Step Compilation

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/Rishav7324/zyvro.git
   cd zyvro
   ```

2. **Verify Environment:**
   Ensure `JAVA_HOME` points to your JDK 21 installation:
   ```bash
   java -version
   # Expected: openjdk version "21.x.x"
   ```

3. **Compile Debug APK:**
   ```bash
   ./gradlew clean assembleDebug
   ```
   The generated APK will be at:
   ```text
   app/build/outputs/apk/debug/app-debug.apk
   ```

4. **Compile Release APK:**
   ```bash
   ./gradlew assembleRelease
   ```
   *(Note: Local release builds automatically fall back to the debug signing key if custom keystore secrets are not set in `key.properties`).*

---

## ⚡ Post-Installation Recommended Configuration

1. **Battery Optimization**: For large downloads (4K movies or full albums), go to **Android Settings > Apps > Zyvro > Battery** and select **Unrestricted**. This prevents Android's Doze mode from pausing downloads when the screen is turned off.
2. **Aria2 Turbo**: Open **Zyvro Settings** and ensure **Aria2 Acceleration** is toggled ON to get up to 8 simultaneous download chunks.
3. **Wi-Fi Auto-Resume**: Enable this option in Settings if you want paused downloads to automatically resume once connected to Wi-Fi.
