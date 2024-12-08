package com.example.siaga;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ProductIdInput extends AppCompatActivity {

    Button submitButton;
    EditText productIdInput;

    private static final String SHARED_PREF_NAME = "productPrefs";
    private static final String KEY_PRODUCT_ID = "productId";

    private static final String BASE_URL = "https://siaga.site/api/";

    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_product_id_input);

        client = new OkHttpClient();

        submitButton = findViewById(R.id.submitButton);
        productIdInput = findViewById(R.id.productIdInput);

        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String productId = productIdInput.getText().toString().trim();
                if (!productId.isEmpty()) {
                    validasiLaluSimpan(productId, sharedPreferences);
                } else {
                    Toast.makeText(ProductIdInput.this, "Harap masukkan ID Produk yang valid.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Validasi dan simpan produk ID baru
    private void validasiLaluSimpan(String productId, SharedPreferences sharedPreferences) {
        String url = BASE_URL + "apps/" + productId;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ProductIdInput.this, "Kesalahan jaringan: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d("ServerResponse", responseBody);
                    JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                    boolean success = jsonResponse.get("success").getAsBoolean();
                    JsonElement messageElement = jsonResponse.get("message");

                    runOnUiThread(() -> {
                        if (success) {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString(KEY_PRODUCT_ID, productId);
                            editor.apply();

                            Intent intent = new Intent(ProductIdInput.this, MainActivity.class);
                            intent.putExtra("produkId", productId);
                            startActivity(intent);
                            finish();
                        } else {
                            handleValidationError(messageElement);
                        }
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(ProductIdInput.this, "Terjadi kesalahan saat memvalidasi ID Produk.", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void handleValidationError(JsonElement messageElement) {
        if (messageElement.isJsonObject()) {
            JsonObject messageObject = messageElement.getAsJsonObject();
            String errorText = messageObject.has("errorText") ? messageObject.get("errorText").getAsString() : "Kesalahan tidak diketahui.";
            Toast.makeText(ProductIdInput.this, errorText, Toast.LENGTH_SHORT).show();
        } else if (messageElement.isJsonArray()) {
            JsonArray messageArray = messageElement.getAsJsonArray();
            StringBuilder errorMessages = new StringBuilder();
            for (JsonElement element : messageArray) {
                errorMessages.append(element.getAsString()).append("\n");
            }
            Toast.makeText(ProductIdInput.this, errorMessages.toString(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(ProductIdInput.this, messageElement.getAsString(), Toast.LENGTH_SHORT).show();
        }
    }
}
