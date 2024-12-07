package com.example.siaga;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashScreen extends AppCompatActivity {

    private static final int SPLASH_SCREEN_DELAY = 2500;

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

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashScreen.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }, SPLASH_SCREEN_DELAY);
    }
}
