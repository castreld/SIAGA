package com.example.siaga;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

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
    int airQuality;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        gasOutTextView = findViewById(R.id.gasOut);
        client = new OkHttpClient();
        handler = new Handler(Looper.getMainLooper());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fanButton = findViewById(R.id.fanButton);
        alarmButton = findViewById(R.id.alarmButton);

        fanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFan();
            }
        });

        alarmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleAlarm();
            }
        });

        updateFanButton();
        updateAlarmButton();
        settingButtonConfig();
        fetchGasValue();
        checkAndMakeNotification();
        makeNotification();
    }

    // INI JUGA BELUM JADI EY

    public void makeNotification() {
        if (airQuality >= 300) {
            String channelID = "CHANNEL_ID_NOTIFICATION";

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(getApplicationContext(), channelID);
            builder.setSmallIcon(R.drawable.siaga)
                    .setContentTitle("VIBRASI BERHASIL DIAKTIFKAN!")
                    .setContentText("Vibrasi Notifikasi berhasil diaktifkan! sekarang Gadget anda akan bergetar ketika mendapat \n notifikasi dari kami!")
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("data", "value");

            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                    0, intent, PendingIntent.FLAG_MUTABLE);
            builder.setContentIntent(pendingIntent);
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel notificationChannel =
                        notificationManager.getNotificationChannel(channelID);
                if (notificationChannel == null) {
                    int importance = NotificationManager.IMPORTANCE_HIGH;
                    notificationChannel = new NotificationChannel(channelID,
                            "Some Description", importance);
                    notificationChannel.setLightColor(Color.GREEN);
                    notificationChannel.enableVibration(true);
                    notificationManager.createNotificationChannel(notificationChannel);
                }
            }

            notificationManager.notify(0, builder.build());
        }
    }


    private void toggleFan() {
        isFanOn = !isFanOn;
        updateFanButton();
        sendFanStateToServer();
    }

    private void sendFanStateToServer() {
        String url = "http://5.9.117.55:5026/api/apps/" + produkId + "/toggle-fan";
        int fanStateToSend = isFanOn ? 1 : 0;

        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json"),
                "{\"fanState\": " + fanStateToSend + "}"
        );

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("MainActivity", "Failed to update fan state on server.", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d("MainActivity", "Fan state updated successfully on the server.");
                } else {
                    Log.e("MainActivity", "Unexpected response code: " + response.code());
                }
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

    private void fetchGasValue() {
        String url = "http://5.9.117.55:5026/api/apps/" + produkId;

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

                        if (jsonObject.getInt("status") == 200) {
                            JSONArray messageArray = jsonObject.getJSONArray("message");
                            JSONObject data = messageArray.getJSONObject(0);

                            airQuality = data.getInt("suhu");
                            final int buttonState = data.getInt("buttonState");
                            final int fanState = data.getInt("fanState");

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    gasOutTextView.setText(String.valueOf(airQuality));

                                    isFanOn = (fanState == 1);
                                    isAlarmOn = (buttonState == 1);

                                    updateFanButton();
                                    updateAlarmButton();
                                    checkAndMakeNotification();
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


    private boolean notificationSent = false;

    private void checkAndMakeNotification() {
        if (airQuality >= 300 && !notificationSent) {
            makeNotification();
            notificationSent = true;
        }
    }



    private void toggleAlarm() {
        isAlarmOn = !isAlarmOn;
        updateAlarmButton();
        sendAlarmStateToServer();
    }

    private void sendAlarmStateToServer() {
        String url = "http://5.9.117.55:5026/api/apps/" + produkId + "/toggle-buzzer";
        int alarmStateToSend = isAlarmOn ? 1 : 0;

        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json"),
                "{\"alarmState\": " + alarmStateToSend + "}"
        );

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("MainActivity", "Failed to update alarm state on server.", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d("MainActivity", "Alarm state updated successfully on the server.");
                } else {
                    Log.e("MainActivity", "Unexpected response code: " + response.code());
                }
            }
        });
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

    private void settingButtonConfig() {
        ImageButton settingButton = findViewById(R.id.settingButton);

        settingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, Settings.class));
            }
        });
    }
}