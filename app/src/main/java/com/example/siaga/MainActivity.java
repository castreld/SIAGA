package com.example.siaga;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
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
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;


import com.example.siaga.NotificationService;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

public class MainActivity extends AppCompatActivity {
    private TextView gasOutTextView;
    private TextView lowestGas;
    private TextView highestGas;
    private OkHttpClient client;
    private Handler handler;
    LineChart gasChart;
    private Button fanButton;
    private Button alarmButton;
    private boolean isFanOn = false;
    private boolean isAlarmOn = false;
    final String TAG = "DEMO";
    private String produkId;
    private NotificationCompat.Builder builder;
    private NotificationManager notificationManager;
    private int dataCount = 0;
    private ArrayList<Entry> gasEntries = new ArrayList<>();
    private int highestThreshold = 0;

    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean o) {
            if (o) {
                Toast.makeText(MainActivity.this, "Post notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Post notification permission not granted", Toast.LENGTH_SHORT).show();
            }
        }
    });

    private int lowestGasValue = Integer.MAX_VALUE;
    private int highestGasValue = Integer.MIN_VALUE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        gasOutTextView = findViewById(R.id.gasOut);
        lowestGas = findViewById(R.id.lowestGasOut);
        highestGas = findViewById(R.id.highestGasOut);

        if(highestThreshold == 0) {
            highestThreshold = 300;
        }else {
            highestThreshold = Integer.parseInt(getIntent().getStringExtra("highestThreshold"));
        }

        produkId = getIntent().getStringExtra("produkId");

        if (produkId == null || produkId.isEmpty()) {
            Toast.makeText(MainActivity.this, "Product ID is missing or invalid.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Chart Section
        gasChart = findViewById(R.id.gasChartOut);

        LineDataSet gasLineChart = new LineDataSet(new ArrayList<>(), "Gas Levels (PPM)");

        gasLineChart.setLineWidth(4);
        gasLineChart.setColor(Color.parseColor("#79B7FF"));
        gasLineChart.setCircleColor(Color.parseColor("#79B7FF"));
        gasLineChart.setCircleRadius(6);
        gasLineChart.setDrawCircleHole(false);

        LineData data = new LineData(gasLineChart);
        gasChart.setData(data);

        XAxis xAxis = gasChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(10);
        xAxis.setGranularityEnabled(true);

        YAxis yAxis = gasChart.getAxisLeft();
        yAxis.setAxisMinimum(0f);

        YAxis leftAxis = gasChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);

        gasChart.getAxisRight().setEnabled(false);
        gasChart.setDragEnabled(true);
        gasChart.setScaleEnabled(false);
        gasChart.setAutoScaleMinMaxEnabled(false);
        gasChart.getAxisLeft().setAxisMinimum(0f);

        gasChart.invalidate();

        // Client Section
        client = new OkHttpClient();
        handler = new Handler(Looper.getMainLooper());
        // Handle edge-to-edge insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // NOTIFIKASI
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "Alert",
                    "Air Quality Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Notification for Air Quality Alerts");
            notificationManager.createNotificationChannel(channel);
        }
        builder = new NotificationCompat.Builder(this, "Alert")
                .setSmallIcon(R.drawable.siaga)
                .setContentTitle("DANGEROUS AIR QUALITY ALERT")
                .setContentText("GAS LEAKS! TAKE IMMEDIATE PRECAUTIONS FOR YOUR SAFETY!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setColor(ContextCompat.getColor(this, R.color.alertColor))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Dangerous gas detected! Gas levels reach dangerous levels. Take immediate precautions to ensure safety."));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            activityResultLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
        Request request = new Request.Builder()
                .url("https://siaga.site/api/apps/" + produkId).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

                if (response.isSuccessful()) {
                    ResponseBody responseBody = response.body();
                    Log.d(TAG, "onResponse: " + responseBody.string());
                }
            }
        });

        fanButton = findViewById(R.id.fanButton);
        alarmButton = findViewById(R.id.alarmButton);

        fanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isFanOn = !isFanOn;
                updateFanButton();
            }
        });

        alarmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isAlarmOn = !isAlarmOn;
                updateAlarmButton();
            }
        });

        Intent serviceIntent = new Intent(this, NotificationService.class);
        ContextCompat.startForegroundService(this, serviceIntent);

        updateFanButton();
        updateAlarmButton();
        settingButtonConfig();
        fetchGasValue();
    }

    private void checkForDangerousGasLevel(int airQuality) {
        final int DANGEROUS_GAS_THRESHOLD = highestThreshold;

        if (airQuality >= DANGEROUS_GAS_THRESHOLD) {
            if (notificationManager != null) {
                if (!isNotificationShowing) {
                    isNotificationShowing = true;
                    notificationManager.notify(1, builder.build());
                }
            }
        } else {
            isNotificationShowing = false;
        }
    }

    private boolean isNotificationShowing = false;

    private void fetchGasValue() {
        String url = "https://siaga.site/api/apps/" + produkId;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("MainActivity", "Failed to fetch data", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        JSONObject jsonObject = new JSONObject(responseData);

                        if (jsonObject.getInt("status") == 200) {
                            JSONArray messageArray = jsonObject.getJSONArray("message");
                            JSONObject data = messageArray.getJSONObject(0);

                            final int airQuality = data.getInt("suhu");

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
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

                                    Entry newEntry = new Entry(dataCount++, airQuality);
                                    gasEntries.add(newEntry);

                                    if (gasEntries.size() > 7) {
                                        gasEntries.remove(0);
                                    }

                                    LineDataSet gasLineChart = new LineDataSet(gasEntries, "Gas Levels");
                                    gasLineChart.setLineWidth(4);
                                    gasLineChart.setColor(Color.parseColor("#2C6CBC"));
                                    gasLineChart.setCircleColor(Color.parseColor("#2C6CBC"));
                                    gasLineChart.setCircleRadius(6);
                                    gasLineChart.setDrawCircleHole(false);

                                    LineData data = new LineData(gasLineChart);
                                    gasChart.setData(data);

                                    gasChart.notifyDataSetChanged();
                                    gasChart.invalidate();
                                }
                            });
                        } else {
                            Log.e("MainActivity", "Error: " + jsonObject.getString("message"));
                        }

                    } catch (JSONException e) {
                        Log.e("MainActivity", "JSON parsing error.", e);
                    }
                } else {
                    Log.e("MainActivity", "Unexpected response code: " + response.code());
                }
            }
        });

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fetchGasValue();
            }
        }, 1000);
    }



    private void settingButtonConfig() {
        ImageButton settingButton = (ImageButton) findViewById(R.id.settingButton);

        settingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, Settings.class);
                intent.putExtra("highestThreshold", highestThreshold);
                intent.putExtra("produkId", produkId);
                startActivity(intent);
            }
        });
    }

    private void updateFanButton() {
        if (isFanOn) {
            fanButton.setEnabled(true);
            fanButton.setBackground(ContextCompat.getDrawable(this, R.drawable.button_rounded_enabled));
            fanButton.setText("FAN\nON");
        } else {
            fanButton.setEnabled(true);
            fanButton.setBackground(ContextCompat.getDrawable(this, R.drawable.button_rounded_disabled));
            fanButton.setText("FAN\nOFF");
        }
    }

    private void updateAlarmButton() {
        if (isAlarmOn) {
            alarmButton.setEnabled(true);
            alarmButton.setBackground(ContextCompat.getDrawable(this, R.drawable.button_rounded_enabled));
            alarmButton.setText("ALARM\nON");
        } else {
            alarmButton.setEnabled(true);
            alarmButton.setBackground(ContextCompat.getDrawable(this, R.drawable.button_rounded_disabled));
            alarmButton.setText("ALARM\nOFF");
        }
    }
}
