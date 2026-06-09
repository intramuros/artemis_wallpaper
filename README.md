# Artemis Wallpaper

Artemis Wallpaper is a small native Android app that fetches random imagery from NASA's Image and Video Library for Artemis mission content and sets it as the device wallpaper.

## Features

- Searches the NASA Images API for Artemis mission images.
- Resolves each selected NASA library item to its downloadable asset manifest and prefers original or large image files for wallpaper quality.
- Downsamples large images before decoding and center-crops them to the device wallpaper dimensions to reduce memory pressure.
- Shows the downloaded image and NASA title in the app.
- Uses Android's `WallpaperManager` to set either the home wallpaper or both home and lock wallpapers.

## Build

```bash
gradle assembleDebug
```

The app requires Android SDK 35 or newer to compile.
