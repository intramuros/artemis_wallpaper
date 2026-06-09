package com.example.artemiswallpaper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class NasaImageClient {
    private static final String ARTEMIS_SEARCH_URL =
            "https://images-api.nasa.gov/search?q=Artemis%20mission&media_type=image&page_size=100";
    private static final int CONNECT_TIMEOUT_MILLIS = 15000;
    private static final int READ_TIMEOUT_MILLIS = 20000;

    List<NasaImage> searchArtemisImages() throws Exception {
        JSONObject response = new JSONObject(readText(ARTEMIS_SEARCH_URL));
        JSONArray items = response.getJSONObject("collection").getJSONArray("items");
        List<NasaImage> images = new ArrayList<>();

        for (int itemIndex = 0; itemIndex < items.length(); itemIndex++) {
            JSONObject item = items.getJSONObject(itemIndex);
            String assetsUrl = item.optString("href", "");
            String title = titleFor(item);
            if (!assetsUrl.isEmpty()) {
                images.add(new NasaImage(title, assetsUrl));
            }
        }

        return images;
    }

    String bestImageUrl(NasaImage image) throws Exception {
        JSONArray assets = new JSONArray(readText(image.assetsUrl()));
        String fallback = "";

        for (int assetIndex = 0; assetIndex < assets.length(); assetIndex++) {
            String assetUrl = assets.getString(assetIndex);
            if (!isSupportedImage(assetUrl)) {
                continue;
            }
            String lowerUrl = assetUrl.toLowerCase(Locale.US);
            if (fallback.isEmpty()) {
                fallback = assetUrl;
            }
            if (lowerUrl.contains("~orig") || lowerUrl.contains("~large")) {
                return assetUrl;
            }
        }

        if (fallback.isEmpty()) {
            throw new IllegalStateException("NASA did not provide a downloadable image asset.");
        }
        return fallback;
    }

    private String titleFor(JSONObject item) {
        JSONArray data = item.optJSONArray("data");
        if (data == null || data.length() == 0) {
            return "NASA Artemis image";
        }
        JSONObject firstDataItem = data.optJSONObject(0);
        if (firstDataItem == null) {
            return "NASA Artemis image";
        }
        return firstDataItem.optString("title", "NASA Artemis image");
    }

    private boolean isSupportedImage(String assetUrl) {
        String lowerUrl = assetUrl.toLowerCase(Locale.US);
        return lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg") || lowerUrl.endsWith(".png");
    }

    private String readText(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        try {
            int statusCode = connection.getResponseCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new IllegalStateException("NASA request failed with HTTP " + statusCode + ".");
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                return builder.toString();
            }
        } finally {
            connection.disconnect();
        }
    }
}
