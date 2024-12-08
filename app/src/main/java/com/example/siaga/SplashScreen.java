package com.example.siaga;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SplashScreen extends AppCompatActivity {

    private static final int SPLASH_SCREEN_DELAY = 2500;
    private static final String SHARED_PREF_NAME = "productPrefs";
    private static final String KEY_PRODUCT_ID = "productId";
    private static final String BASE_URL = "https://siaga.site/api/";

    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        ImageView logoImage = findViewById(R.id.imageView3);
        ImageView smknImage = findViewById(R.id.imageView);
        ImageView siagaImage = findViewById(R.id.imageView2);
        TextView titleText = findViewById(R.id.textView);
        TextView subTitleText = findViewById(R.id.textView2);

        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        logoImage.startAnimation(fadeIn);
        smknImage.startAnimation(fadeIn);
        siagaImage.startAnimation(fadeIn);
        titleText.startAnimation(fadeIn);
        subTitleText.startAnimation(fadeIn);

        client = new OkHttpClient();

        new Handler(Looper.getMainLooper()).postDelayed(this::checkSavedProductId, SPLASH_SCREEN_DELAY);
    }

    private void checkSavedProductId() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        String savedProductId = sharedPreferences.getString(KEY_PRODUCT_ID, "");

        if (!savedProductId.isEmpty()) {
            validateProductId(savedProductId, sharedPreferences);
        } else {
            navigateToProductIdInput();
        }
    }

    private void validateProductId(String productId, SharedPreferences sharedPreferences) {
        String url = BASE_URL + "apps/" + productId;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(SplashScreen.this, "Gagal memvalidasi ID Produk: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    navigateToProductIdInput();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                    boolean success = jsonResponse.get("success").getAsBoolean();

                    runOnUiThread(() -> {
                        if (success) {
                            navigateToMainActivity(productId);
                        } else {
                            Toast.makeText(SplashScreen.this, "ID Produk tidak valid. Harap masukkan ulang.", Toast.LENGTH_SHORT).show();
                            sharedPreferences.edit().remove(KEY_PRODUCT_ID).apply();
                            navigateToProductIdInput();
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(SplashScreen.this, "Terjadi kesalahan saat validasi ID Produk.", Toast.LENGTH_SHORT).show();
                        navigateToProductIdInput();
                    });
                }
            }
        });
    }

    private void navigateToProductIdInput() {
        Intent intent = new Intent(SplashScreen.this, ProductIdInput.class);
        startActivity(intent);
        finish();
    }

    private void navigateToMainActivity(String productId) {
        Intent intent = new Intent(SplashScreen.this, MainActivity.class);
        intent.putExtra("produkId", productId);
        startActivity(intent);
        finish();
    }
}
