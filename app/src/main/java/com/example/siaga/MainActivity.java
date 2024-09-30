package com.example.siaga;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import com.example.siaga.NotificationService;

public class MainActivity extends AppCompatActivity {
    private TextView gasOutTextView;
    private OkHttpClient client;
    private Handler handler;
    private Button fanButton;
    private Button alarmButton;
    private boolean isFanOn = false;
    private boolean isAlarmOn = false;
    final String TAG = "DEMO";
    private String produkId = "78A3EE";
    private NotificationCompat.Builder builder;
    private NotificationManager notificationManager;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        gasOutTextView = findViewById(R.id.gasOut);
        client = new OkHttpClient();
        handler = new Handler(Looper.getMainLooper());
        // Handle edge-to-edge insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

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
                .setContentTitle("PERINGATAN KUALITAS UDARA")
                .setContentText("GAS BOCOR! SEGERA LAKUKAN TINDAKAN PENCEGAHAN DEMI KESELAMATAN ANDA!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setColor(ContextCompat.getColor(this, R.color.alertColor))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Adanya gas bahaya terdeteksi! Kadar gas mencapai tingkat berbahaya. Segera lakukan tindakan pencegahan untuk memastikan keselamatan.")); // Expanded text for more detail

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            activityResultLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
        }
        Request request = new Request.Builder()
                .url("https://siaga.site/api/apps/78A3EE").build();

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

        // Initialize buttons
        fanButton = findViewById(R.id.fanButton);
        alarmButton = findViewById(R.id.alarmButton);

        // Set click listeners to toggle button states
        fanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isFanOn = !isFanOn; // Toggle the state
                updateFanButton();
            }
        });

        alarmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isAlarmOn = !isAlarmOn; // Toggle the state
                updateAlarmButton();
            }
        });

        Intent serviceIntent = new Intent(this, NotificationService.class);
        ContextCompat.startForegroundService(this, serviceIntent);

        // Update buttons' initial states
        updateFanButton();
        updateAlarmButton();
        settingButtonConfig();
        fetchGasValue();
    }

    private void fetchGasValue() {
        String url = "https://siaga.site/api/apps/" + produkId;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("MainActivity", "Failed to fetch data from the server.", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        JSONObject jsonObject = new JSONObject(responseData);

                        // Check the status code
                        if (jsonObject.getInt("status") == 200) {
                            JSONArray messageArray = jsonObject.getJSONArray("message");
                            JSONObject data = messageArray.getJSONObject(0);

                            final int airQuality = data.getInt("suhu");

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    gasOutTextView.setText(String.valueOf(airQuality));
                                }
                            });

                            if (airQuality >= 300) {
                                notificationManager.notify(10, builder.build());
                            }
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
                startActivity(new Intent(MainActivity.this, Settings.class));
            }
        });
    }

    private void updateFanButton() {
        if (isFanOn) {
            fanButton.setEnabled(true);
            fanButton.setBackground(ContextCompat.getDrawable(this, R.drawable.button_rounded_enabled));
            fanButton.setText("FAN \n ON");
        } else {
            fanButton.setEnabled(true);
            fanButton.setBackground(ContextCompat.getDrawable(this, R.drawable.button_rounded_disabled));
            fanButton.setText("FAN \n OFF");
        }
    }

    private void updateAlarmButton() {
        if (isAlarmOn) {
            alarmButton.setEnabled(true);
            alarmButton.setBackground(ContextCompat.getDrawable(this, R.drawable.button_rounded_enabled));
            alarmButton.setText("ALARM \n ON");
        } else {
            alarmButton.setEnabled(true);
            alarmButton.setBackground(ContextCompat.getDrawable(this, R.drawable.button_rounded_disabled));
            alarmButton.setText("ALARM \n OFF");
        }
    }


}
