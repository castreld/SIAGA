package com.example.siaga;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MainActivity extends AppCompatActivity implements MqttManager.MqttCallbackHandler {

    private static final String TAG = "MainActivity";
    private static final int REFRESH_INTERVAL_MS = 3000;

    private TextView gasOutTextView, lowestGas, highestGas;
    private LineChart gasChart;
    private Button fanButton, alarmButton;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    private OkHttpClient httpClient;
    private Handler handler;
    private ArrayList<Entry> gasEntries;
    private int dataCount = 0;
    private int lowestGasValue = Integer.MAX_VALUE;
    private int highestGasValue = Integer.MIN_VALUE;
    private boolean isFanOn = false;
    private boolean isAlarmOn = false;
    private boolean isNotificationShowing = false;
    private int highestThreshold;
    private String productId;

    private MqttManager mqttManager;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        initViews();
        initNotificationChannel();
        setupGasChart();
        fetchConfigData();
        // fetchGasValuePeriodically();
        setupButtonListeners();
        startNotificationService();
        initMqtt();
    }

    private void initMqtt() {
        String mqttBrokerUri = "mqtts://aa114d69a648467da8a56e7ba1b2bd56.s1.eu.hivemq.cloud:8883";
        String clientId = "android_app_" + System.currentTimeMillis();
        mqttManager = new MqttManager(getApplicationContext(), mqttBrokerUri, clientId, this);
    }

    @Override
    public void onConnectSuccess() {
        Log.i(TAG, "MQTT Connection Successful. Subscribing to topics...");
        mqttManager.subscribe("gas_level");
        mqttManager.subscribe("device_status");
        Toast.makeText(this, "Connected to MQTT", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectFailure(Throwable exception) {
        Log.e(TAG, "MQTT Connection Failed: " + exception.getMessage());
        Toast.makeText(this, "Failed to connect to MQTT: " + exception.getMessage(), Toast.LENGTH_LONG).show();
        // Consider retrying the connection
    }

    @Override
    public void onConnectionLost(Throwable cause) {
        Log.w(TAG, "MQTT Connection Lost: " + cause.getMessage());
        Toast.makeText(this, "MQTT Connection Lost: " + cause.getMessage(), Toast.LENGTH_SHORT).show();
        // Consider attempting to reconnect
    }

    @Override
    public void onMessageReceived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());
        Log.d(TAG, "Received MQTT message on topic '" + topic + "': " + payload);

        if (topic.equals("gas_level")) {
            try {
                int airQuality = Integer.parseInt(payload);
                runOnUiThread(() -> updateGasData(airQuality));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing gas_level payload: " + payload, e);
            }
        } else if (topic.equals("device_status")) {
            runOnUiThread(() -> {
                // Handle device status updates here
                Log.i(TAG, "Device Status: " + payload);
                // You might want to update UI or app state based on device status
                Toast.makeText(MainActivity.this, "Device Status: " + payload, Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public void onDeliveryComplete(IMqttDeliveryToken token) {
        Log.i(TAG, "MQTT Message Delivery Complete (if publishing)");
    }

    private void initViews() {
        gasOutTextView = findViewById(R.id.gasOut);
        lowestGas = findViewById(R.id.lowestGasOut);
        highestGas = findViewById(R.id.highestGasOut);
        gasChart = findViewById(R.id.gasChartOut);
        fanButton = findViewById(R.id.fanButton);
        alarmButton = findViewById(R.id.alarmButton);

        handler = new Handler(Looper.getMainLooper());
        httpClient = new OkHttpClient();
        gasEntries = new ArrayList<>();

        SharedPreferences sharedPreferences = getSharedPreferences("productPrefs", Context.MODE_PRIVATE);
        productId = sharedPreferences.getString("productId", null);

        if (productId == null || productId.isEmpty()) {
            Toast.makeText(this, "Product ID is missing or invalid.", Toast.LENGTH_SHORT).show();
            redirectToProductIdInput();

            return;
        }

        validateProductId(sharedPreferences);
        highestThreshold = getIntent().getIntExtra("highestThreshold", 300);
    }

    private void validateProductId(SharedPreferences sharedPreferences) {
        String url = "https://siaga.site/api/apps/" + productId;

        Request request = new Request.Builder().url(url).build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Kesalahan jaringan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    redirectToProductIdInput();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        boolean success = jsonObject.getBoolean("success");

                        runOnUiThread(() -> {
                            if (!success) {
                                Toast.makeText(MainActivity.this, "Product ID tidak valid. Harap masukkan ulang.", Toast.LENGTH_SHORT).show();
                                sharedPreferences.edit().remove("productId").apply();
                                redirectToProductIdInput();
                            }
                        });
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing JSON", e);
                        runOnUiThread(() -> redirectToProductIdInput());
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Gagal memvalidasi Product ID.", Toast.LENGTH_SHORT).show();
                        redirectToProductIdInput();
                    });
                }
            }
        });
    }

    private void redirectToProductIdInput() {
        Intent intent = new Intent(MainActivity.this, ProductIdInput.class);
        startActivity(intent);
        finish();
    }

    private void initNotificationChannel() {
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "Alert", "Air Quality Alerts", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications for dangerous gas levels.");
            notificationManager.createNotificationChannel(channel);
        }

        notificationBuilder = new NotificationCompat.Builder(this, "Alert")
                .setSmallIcon(R.drawable.siaga)
                .setContentTitle("Dangerous Gas Alert!")
                .setContentText("Gas levels are dangerously high. Take immediate precautions.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(ContextCompat.getColor(this, R.color.alertColor))
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                        "Dangerous gas detected! Ensure safety immediately."));
    }

    private void setupGasChart() {
        gasChart.getDescription().setEnabled(false);
        gasChart.getLegend().setEnabled(false);

        XAxis xAxis = gasChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(5);

        YAxis leftAxis = gasChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);

        gasChart.getAxisRight().setEnabled(false);
        gasChart.setDragEnabled(true);
        gasChart.setScaleEnabled(false);
    }

    private void fetchConfigData() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void fetchGasValuePeriodically() {
        String url = "https://siaga.site/api/apps/" + productId;
        Request request = new Request.Builder().url(url).build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Failed to fetch gas values", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        if (jsonObject.getBoolean("success")) {
                            JSONArray messageArray = jsonObject.getJSONArray("message");
                            JSONObject data = messageArray.getJSONObject(0);
                            int airQuality = data.getInt("suhu");

                            handler.post(() -> updateGasData(airQuality));
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing JSON", e);
                    }
                } else {
                    Log.e(TAG, "Unexpected response: " + response.code());
                }
            }
        });

        handler.postDelayed(this::fetchGasValuePeriodically, REFRESH_INTERVAL_MS);
    }

    private void updateGasData(int airQuality) {
        gasOutTextView.setText(String.valueOf(airQuality));

        if (airQuality > 0 && airQuality < lowestGasValue) {
            lowestGasValue = airQuality;
            lowestGas.setText(String.valueOf(lowestGasValue));
        }

        if (airQuality > highestGasValue) {
            highestGasValue = airQuality;
            highestGas.setText(String.valueOf(highestGasValue));
        }

        checkForDangerousGasLevel(airQuality);

        gasEntries.add(new Entry(dataCount++, airQuality));
        if (gasEntries.size() > 7) gasEntries.remove(0);

        LineDataSet lineDataSet = new LineDataSet(gasEntries, "Gas Levels");
        lineDataSet.setLineWidth(4);
        lineDataSet.setColor(Color.parseColor("#2C6CBC"));
        lineDataSet.setCircleColor(Color.parseColor("#2C6CBC"));

        gasChart.setData(new LineData(lineDataSet));
        gasChart.invalidate();
    }

    private void checkForDangerousGasLevel(int airQuality) {
        if (airQuality >= highestThreshold) {
            if (!isNotificationShowing) {
                isNotificationShowing = true;
                notificationManager.notify(1, notificationBuilder.build());
            }
        } else {
            isNotificationShowing = false;
        }
    }

    private void setupButtonListeners() {
        fanButton.setOnClickListener(v -> {
            isFanOn = !isFanOn;
            updateFanButton();
        });

        alarmButton.setOnClickListener(v -> {
            isAlarmOn = !isAlarmOn;
            updateAlarmButton();
        });

        ImageButton settingButton = findViewById(R.id.settingButton);
        settingButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, Settings.class);
            intent.putExtra("highestThreshold", highestThreshold);
            intent.putExtra("productId", productId);
            startActivity(intent);
        });

        updateFanButton();
        updateAlarmButton();
    }

    private void updateFanButton() {
        fanButton.setText(isFanOn ? "FAN\nON" : "FAN\nOFF");
        fanButton.setBackground(ContextCompat.getDrawable(this, isFanOn ?
                R.drawable.button_rounded_enabled : R.drawable.button_rounded_disabled));
    }

    private void updateAlarmButton() {
        alarmButton.setText(isAlarmOn ? "ALARM\nON" : "ALARM\nOFF");
        alarmButton.setBackground(ContextCompat.getDrawable(this, isAlarmOn ?
                R.drawable.button_rounded_enabled : R.drawable.button_rounded_disabled));
    }

    private void startNotificationService() {
        Intent serviceIntent = new Intent(this, NotificationService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }
}
