# Artemis Wallpaper

Artemis Wallpaper is a small native Android app that fetches random imagery from NASA's Image and Video Library for Artemis mission content and sets it as the device wallpaper.

## Features

- Searches the NASA Images API for Artemis mission images.
- Resolves each selected NASA library item to its downloadable asset manifest and prefers original or large image files for wallpaper quality.
- Downsamples large images before decoding and center-crops them to the device wallpaper dimensions to reduce memory pressure.
- Shows the downloaded image and NASA title in the app.
- Uses Android's `WallpaperManager` to set either the home wallpaper or both home and lock wallpapers.

## Build locally

```bash
gradle assembleDebug
```

The app requires Android SDK 35 or newer to compile.

The debug APK is created at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install it on a USB-connected phone with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Build with GitHub Actions

This repository includes a GitHub Actions workflow at `.github/workflows/android.yml` that builds the debug APK and uploads it as a workflow artifact named `artemis-wallpaper-debug-apk`.

### Build on GitHub-hosted runners

1. Push this repository to GitHub.
2. Open the repository on GitHub.
3. Go to **Actions**.
4. Select **Android Debug APK**.
5. Click **Run workflow** and keep the default `ubuntu-latest` runner.
6. When the run finishes, open the run summary and download the `artemis-wallpaper-debug-apk` artifact.
7. Unzip the artifact and install `app-debug.apk` on your phone.

### Build on your remote self-hosted runner

If your remote machine is registered as a GitHub Actions self-hosted runner, it can build this app too. In **Run workflow**, choose `self-hosted` for the runner input. This helps if you want builds to run on your own hardware or avoid GitHub-hosted runner queue time.

Install or verify these prerequisites on the self-hosted runner:

- A supported 64-bit runner operating system such as Ubuntu 20.04 or newer, Debian 10 or newer, Windows 10/11, Windows Server 2016 or newer, or macOS 11 or newer.
- The GitHub Actions runner application registered to this repository or organization, online, idle, and labeled `self-hosted`.
- Outbound HTTPS access on port 443 so the runner can communicate with GitHub and download build dependencies.
- Gradle installed and available on `PATH`, because this repository currently runs `gradle assembleDebug` instead of a checked-in `./gradlew` wrapper.
- Permission for workflow steps to download and install Android SDK command-line tools, platform 35, build tools 35.0.0, and platform-tools. The workflow handles these Android SDK installs automatically.
- Network access to Google Maven, Maven Central, the Gradle Plugin Portal, and Android SDK download endpoints so Gradle and `sdkmanager` can resolve dependencies.
- Enough disk space for Android SDK packages plus Gradle caches; 10 GB free is a practical minimum, and 20 GB or more is safer.

You do **not** need to preinstall JDK 17 or the Android SDK if the runner can download tools during the job: the workflow uses `actions/setup-java` to install Temurin JDK 17 and `android-actions/setup-android` plus `sdkmanager` to install the Android SDK packages before running `gradle assembleDebug`.
