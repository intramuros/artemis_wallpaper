package com.example.artemiswallpaper;

import android.app.Activity;
import android.app.WallpaperManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String ARTEMIS_SEARCH_URL =
            "https://images-api.nasa.gov/search?q=Artemis%20mission&media_type=image&page_size=100";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainThread = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private Button wallpaperButton;
    private ImageView previewImage;
    private ProgressBar progressBar;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildLayout());
        wallpaperButton.setOnClickListener(view -> loadAndSetRandomArtemisWallpaper());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private View buildLayout() {
        int padding = dp(20);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(padding, padding, padding, padding);

        TextView title = new TextView(this);
        title.setText("Artemis Wallpaper");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(0xFF0B3D91);
        root.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView description = new TextView(this);
        description.setText("Fetch a random NASA Artemis mission image and set it as your phone background.");
        description.setGravity(Gravity.CENTER);
        description.setTextSize(16);
        LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        descriptionParams.setMargins(0, dp(12), 0, dp(16));
        root.addView(description, descriptionParams);

        previewImage = new ImageView(this);
        previewImage.setAdjustViewBounds(true);
        previewImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        previewImage.setBackgroundColor(0xFFE7ECF2);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f);
        root.addView(previewImage, imageParams);

        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        progressParams.setMargins(0, dp(16), 0, 0);
        root.addView(progressBar, progressParams);

        statusText = new TextView(this);
        statusText.setText("Tap the button to download an Artemis image.");
        statusText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        statusParams.setMargins(0, dp(12), 0, dp(12));
        root.addView(statusText, statusParams);

        wallpaperButton = new Button(this);
        wallpaperButton.setText("Set random Artemis wallpaper");
        root.addView(wallpaperButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        return root;
    }

    private void loadAndSetRandomArtemisWallpaper() {
        setLoading(true, "Finding Artemis mission images...");
        executor.execute(() -> {
            try {
                List<String> images = fetchImageUrls();
                if (images.isEmpty()) {
                    throw new IllegalStateException("NASA did not return any Artemis images.");
                }

                String imageUrl = images.get(random.nextInt(images.size()));
                updateStatus("Downloading image...");
                Bitmap bitmap = downloadBitmap(imageUrl);

                updateStatus("Setting wallpaper...");
                WallpaperManager.getInstance(this).setBitmap(bitmap);

                mainThread.post(() -> {
                    previewImage.setImageBitmap(bitmap);
                    setLoading(false, "Wallpaper updated from NASA Artemis imagery.");
                });
            } catch (Exception exception) {
                mainThread.post(() -> setLoading(false, "Unable to update wallpaper: " + exception.getMessage()));
            }
        });
    }

    private List<String> fetchImageUrls() throws Exception {
        JSONObject response = new JSONObject(readText(ARTEMIS_SEARCH_URL));
        JSONArray items = response.getJSONObject("collection").getJSONArray("items");
        List<String> imageUrls = new ArrayList<>();

        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            JSONArray links = item.optJSONArray("links");
            if (links == null) {
                continue;
            }
            for (int linkIndex = 0; linkIndex < links.length(); linkIndex++) {
                JSONObject link = links.getJSONObject(linkIndex);
                if ("preview".equals(link.optString("rel"))) {
                    imageUrls.add(link.getString("href"));
                    break;
                }
            }
        }

        return imageUrls;
    }

    private Bitmap downloadBitmap(String imageUrl) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(imageUrl).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        try (InputStream inputStream = new BufferedInputStream(connection.getInputStream())) {
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) {
                throw new IllegalStateException("Downloaded image could not be decoded.");
            }
            return bitmap;
        } finally {
            connection.disconnect();
        }
    }

    private String readText(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        } finally {
            connection.disconnect();
        }
    }

    private void setLoading(boolean loading, String message) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        wallpaperButton.setEnabled(!loading);
        statusText.setText(message);
    }

    private void updateStatus(String message) {
        mainThread.post(() -> statusText.setText(message));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
