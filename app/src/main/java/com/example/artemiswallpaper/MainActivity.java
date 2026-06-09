package com.example.artemiswallpaper;

import android.app.Activity;
import android.app.WallpaperManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
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

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int CONNECT_TIMEOUT_MILLIS = 15000;
    private static final int READ_TIMEOUT_MILLIS = 30000;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainThread = new Handler(Looper.getMainLooper());
    private final NasaImageClient nasaImageClient = new NasaImageClient();
    private final Random random = new Random();

    private Button homeButton;
    private Button homeAndLockButton;
    private ImageView previewImage;
    private ProgressBar progressBar;
    private TextView statusText;
    private TextView imageTitleText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildLayout());
        homeButton.setOnClickListener(view -> loadAndSetRandomArtemisWallpaper(WallpaperManager.FLAG_SYSTEM));
        homeAndLockButton.setOnClickListener(view -> loadAndSetRandomArtemisWallpaper(
                WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK));
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
        title.setText(getString(R.string.app_title));
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(0xFF0B3D91);
        root.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView description = new TextView(this);
        description.setText(getString(R.string.app_description));
        description.setGravity(Gravity.CENTER);
        description.setTextSize(16);
        LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        descriptionParams.setMargins(0, dp(12), 0, dp(16));
        root.addView(description, descriptionParams);

        previewImage = new ImageView(this);
        previewImage.setContentDescription(getString(R.string.preview_content_description));
        previewImage.setAdjustViewBounds(true);
        previewImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        previewImage.setBackgroundColor(0xFFE7ECF2);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f);
        root.addView(previewImage, imageParams);

        imageTitleText = new TextView(this);
        imageTitleText.setGravity(Gravity.CENTER);
        imageTitleText.setText(getString(R.string.no_image_selected));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.setMargins(0, dp(12), 0, 0);
        root.addView(imageTitleText, titleParams);

        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        progressParams.setMargins(0, dp(12), 0, 0);
        root.addView(progressBar, progressParams);

        statusText = new TextView(this);
        statusText.setText(getString(R.string.ready_status));
        statusText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        statusParams.setMargins(0, dp(12), 0, dp(12));
        root.addView(statusText, statusParams);

        homeButton = new Button(this);
        homeButton.setText(getString(R.string.set_home_wallpaper));
        root.addView(homeButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        homeAndLockButton = new Button(this);
        homeAndLockButton.setText(getString(R.string.set_home_lock_wallpaper));
        LinearLayout.LayoutParams lockParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lockParams.setMargins(0, dp(8), 0, 0);
        root.addView(homeAndLockButton, lockParams);

        return root;
    }

    private void loadAndSetRandomArtemisWallpaper(int wallpaperFlags) {
        setLoading(true, getString(R.string.finding_images_status));
        executor.execute(() -> {
            try {
                List<NasaImage> images = nasaImageClient.searchArtemisImages();
                if (images.isEmpty()) {
                    throw new IllegalStateException(getString(R.string.no_images_error));
                }

                NasaImage image = images.get(random.nextInt(images.size()));
                updateStatus(getString(R.string.finding_asset_status));
                String imageUrl = nasaImageClient.bestImageUrl(image);

                updateStatus(getString(R.string.downloading_status));
                Bitmap bitmap = downloadScaledBitmap(imageUrl);
                Bitmap wallpaperBitmap = centerCropForWallpaper(bitmap);

                updateStatus(getString(R.string.setting_wallpaper_status));
                setWallpaper(wallpaperBitmap, wallpaperFlags);

                mainThread.post(() -> {
                    previewImage.setImageBitmap(wallpaperBitmap);
                    imageTitleText.setText(image.title());
                    setLoading(false, getString(R.string.wallpaper_updated_status));
                });
            } catch (Exception exception) {
                mainThread.post(() -> setLoading(false,
                        getString(R.string.wallpaper_error_status, exception.getMessage())));
            }
        });
    }

    private Bitmap downloadScaledBitmap(String imageUrl) throws Exception {
        BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
        boundsOptions.inJustDecodeBounds = true;
        downloadBitmap(imageUrl, boundsOptions);

        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inSampleSize = sampleSizeForWallpaper(boundsOptions);
        Bitmap bitmap = downloadBitmap(imageUrl, decodeOptions);
        if (bitmap == null) {
            throw new IllegalStateException(getString(R.string.decode_error));
        }
        return bitmap;
    }

    private Bitmap downloadBitmap(String imageUrl, BitmapFactory.Options options) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(imageUrl).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        try {
            int statusCode = connection.getResponseCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new IllegalStateException("Image download failed with HTTP " + statusCode + ".");
            }

            try (InputStream inputStream = new BufferedInputStream(connection.getInputStream())) {
                return BitmapFactory.decodeStream(inputStream, null, options);
            }
        } finally {
            connection.disconnect();
        }
    }

    private int sampleSizeForWallpaper(BitmapFactory.Options options) {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
        int targetWidth = Math.max(wallpaperManager.getDesiredMinimumWidth(), getResources().getDisplayMetrics().widthPixels);
        int targetHeight = Math.max(wallpaperManager.getDesiredMinimumHeight(), getResources().getDisplayMetrics().heightPixels);
        int sampleSize = 1;

        while ((options.outWidth / (sampleSize * 2)) >= targetWidth
                && (options.outHeight / (sampleSize * 2)) >= targetHeight) {
            sampleSize *= 2;
        }

        return sampleSize;
    }

    private Bitmap centerCropForWallpaper(Bitmap bitmap) {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
        int targetWidth = Math.max(wallpaperManager.getDesiredMinimumWidth(), getResources().getDisplayMetrics().widthPixels);
        int targetHeight = Math.max(wallpaperManager.getDesiredMinimumHeight(), getResources().getDisplayMetrics().heightPixels);
        Bitmap output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        float scale = Math.max(targetWidth / (float) bitmap.getWidth(), targetHeight / (float) bitmap.getHeight());
        int scaledWidth = Math.round(bitmap.getWidth() * scale);
        int scaledHeight = Math.round(bitmap.getHeight() * scale);
        int left = (targetWidth - scaledWidth) / 2;
        int top = (targetHeight - scaledHeight) / 2;
        Rect destination = new Rect(left, top, left + scaledWidth, top + scaledHeight);
        canvas.drawBitmap(bitmap, null, destination, null);

        return output;
    }

    private void setWallpaper(Bitmap bitmap, int wallpaperFlags) throws Exception {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            wallpaperManager.setBitmap(bitmap, null, true, wallpaperFlags);
        } else {
            wallpaperManager.setBitmap(bitmap);
        }
    }

    private void setLoading(boolean loading, String message) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        homeButton.setEnabled(!loading);
        homeAndLockButton.setEnabled(!loading);
        statusText.setText(message);
    }

    private void updateStatus(String message) {
        mainThread.post(() -> statusText.setText(message));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
