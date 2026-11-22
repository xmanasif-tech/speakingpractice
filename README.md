# Shadowing — OnePlus Nord CE5 Android APK Project

Native Android 15 (API 35 / OxygenOS 15) Kotlin WebView wrapper configured for **https://shadowing.tech**.

## Device & Build Profile
- **Target Device**: OnePlus Nord CE5 (OxygenOS 15 / Android 15, 6.77" 1080×2392 120Hz)
- **Package ID**: `tech.shadowing.nordce5`
- **Target / Compile SDK**: 35 (Min SDK 26)
- **ABI Architecture**: arm64-v8a (64-bit optimized)
- **Voice Recording (WebRTC Mic)**: Enabled (RECORD_AUDIO + WebChromeClient.onPermissionRequest)
- **Edge-to-Edge Inset Safety**: Enabled (punch-hole & OxygenOS gesture bar safe)

## Fastest Way to Build Your .apk (No PC Software Needed — 2 Minutes)
1. Create a free repository on [github.com/new](https://github.com/new).
2. Upload all extracted files from this ZIP (make sure the `.github/workflows/build-apk.yml` folder is included).
3. Open the **Actions** tab on GitHub and click **Build Nord CE5 APK** (it triggers automatically on push).
4. Once finished (~90 seconds), download **`shadowing-nord-ce5-apk`** from the Artifacts section, unzip it, and transfer the `.apk` to your OnePlus Nord CE5.

## Local Build (Android Studio or Gradle CLI)
```bash
# Requires JDK 17 + Android SDK Platform 35
gradle assembleDebug

# Install directly to OnePlus Nord CE5 via USB Debugging:
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Installing on OnePlus Nord CE5 (OxygenOS 15)
1. Open the `.apk` file from **My Files** or **Chrome Downloads**.
2. Tap **Settings** when prompted and toggle **Allow from this source** (Settings → Apps → Special app access → Install unknown apps).
3. Tap **Install**. If Google Play Protect asks, choose **Install anyway** (unsigned debug builds show this prompt).
4. Open **Shadowing** and tap **While using the app** when prompted for Microphone access so voice shadowing works.
