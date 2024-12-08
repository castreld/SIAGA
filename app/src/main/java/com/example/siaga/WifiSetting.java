package com.example.siaga;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WifiSetting extends AppCompatActivity {

    private static final String TAG = "WifiSetting";
    private static final String NODEMCU_URL = "http://192.168.4.1";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final int REQUEST_PERMISSIONS = 123;
    private static final int SCAN_INTERVAL = 5000;

    private WifiManager wifiManager;
    private LinearLayout wifiContainer;
    private Handler handler;
    private Runnable fetchTask;

    private EditText ssidInput;
    private EditText passwordInput;
    private Button submitButton;

    private OkHttpClient client;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_setting);

        wifiContainer = findViewById(R.id.wifiContainer);
        ssidInput = findViewById(R.id.ssidInput);
        passwordInput = findViewById(R.id.passInput);
        submitButton = findViewById(R.id.submitButton);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        client = new OkHttpClient();

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ssid = ssidInput.getText().toString().trim();
                String password = passwordInput.getText().toString().trim();

                if (!ssid.isEmpty() && !password.isEmpty()) {
                    sendWifiConfigToNodeMCU(ssid, password);
                } else {
                    Toast.makeText(WifiSetting.this, "Harap isi SSID dan Password Wi-Fi", Toast.LENGTH_SHORT).show();
                }
            }
        });

        if (arePermissionsGranted()) {
            startRealTimeFetching();
        } else {
            requestPermissions();
        }
    }

    private void sendWifiConfigToNodeMCU(String ssid, String password) {
        String jsonPayload = String.format("{\"ssid\":\"%s\",\"password\":\"%s\"}", ssid, password);

        RequestBody body = RequestBody.create(JSON, jsonPayload);
        Request request = new Request.Builder()
                .url(NODEMCU_URL+"/save-wifi")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Gagal mengirim konfigurasi Wi-Fi", e);
                runOnUiThread(() -> Toast.makeText(WifiSetting.this, "Gagal terhubung ke server NodeMCU", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(WifiSetting.this, "Konfigurasi Wi-Fi berhasil disimpan!", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(WifiSetting.this, "Gagal menyimpan konfigurasi Wi-Fi", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private boolean arePermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            Toast.makeText(this, "Aplikasi memerlukan izin lokasi untuk memindai jaringan Wi-Fi.", Toast.LENGTH_LONG).show();
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS);
    }

    private void fetchWiFiNetworksFromNodeMCU() {
        Request request = new Request.Builder()
                .url(NODEMCU_URL+"/scan")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Error fetching WiFi networks", e);
                runOnUiThread(() -> Toast.makeText(WifiSetting.this, "Gagal terhubung ke server NodeMCU", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        JSONArray networks = jsonResponse.getJSONArray("networks");

                        runOnUiThread(() -> updateWiFiList(networks));
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing JSON response", e);
                        runOnUiThread(() -> Toast.makeText(WifiSetting.this, "Gagal memproses data WiFi", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(WifiSetting.this, "Gagal mendapatkan data WiFi", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void updateWiFiList(JSONArray networks) {
        wifiContainer.removeAllViews();

        try {
            for (int i = 0; i < networks.length(); i++) {
                JSONObject network = networks.getJSONObject(i);
                String ssid = network.getString("ssid");

                TextView wifiTextView = new TextView(this);
                wifiTextView.setText(ssid);
                wifiTextView.setTextSize(20);
                wifiTextView.setTextColor(getResources().getColor(R.color.secColorTxt));
                wifiTextView.setPadding(10, 10, 10, 10);
                wifiTextView.setBackground(getResources().getDrawable(R.drawable.select_wifi_bg));

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 0, 10);
                wifiTextView.setLayoutParams(params);

                wifiContainer.addView(wifiTextView);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error updating WiFi list", e);
        }
    }

    private String getCurrentSSID() {
        if (wifiManager != null && wifiManager.getConnectionInfo() != null) {
            String ssid = wifiManager.getConnectionInfo().getSSID();
            if (ssid != null) {
                return ssid.replace("\"", "");
            }
        }
        return null;
    }

    private void startRealTimeFetching() {
        fetchTask = new Runnable() {
            @Override
            public void run() {
                String currentSSID = getCurrentSSID();
                if (currentSSID != null && currentSSID.equals("Siaga Device")) {
                    fetchWiFiNetworksFromNodeMCU();
                    handler.postDelayed(this, SCAN_INTERVAL);
                } else {
                    runOnUiThread(() -> Toast.makeText(WifiSetting.this, "Pastikan terhubung ke jaringan NodeMCU terlebih dahulu.", Toast.LENGTH_SHORT).show());
                }
            }
        };
        handler = new Handler(Looper.getMainLooper());
        handler.post(fetchTask);
    }

    private void stopRealTimeFetching() {
        if (fetchTask != null && handler != null) {
            handler.removeCallbacks(fetchTask);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (arePermissionsGranted()) {
            startRealTimeFetching();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRealTimeFetching();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRealTimeFetching();
            } else {
                Toast.makeText(this, "Izin diperlukan untuk memindai jaringan Wi-Fi.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
