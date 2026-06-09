package com.example.artemiswallpaper;

final class NasaImage {
    private final String title;
    private final String assetsUrl;

    NasaImage(String title, String assetsUrl) {
        this.title = title;
        this.assetsUrl = assetsUrl;
    }

    String title() {
        return title;
    }

    String assetsUrl() {
        return assetsUrl;
    }
}
