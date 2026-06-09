# Artemis Wallpaper

Artemis Wallpaper is a small Android app that fetches a random image from NASA's Image and Video Library for Artemis mission imagery and sets it as the device wallpaper.

## Features

- Searches the NASA Images API for Artemis mission images.
- Downloads a random preview image from the result set.
- Shows the downloaded image in the app.
- Uses Android's `WallpaperManager` to set the phone background.

## Build

```bash
gradle assembleDebug
```

The app requires Android SDK 35 or newer to compile.
