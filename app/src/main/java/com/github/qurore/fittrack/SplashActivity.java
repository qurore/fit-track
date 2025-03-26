package com.github.qurore.fittrack;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private static final long SPLASH_DISPLAY_LENGTH = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize views
        ImageView splashLogo = findViewById(R.id.splashLogo);
        TextView splashText = findViewById(R.id.splashText);

        // Create fade in animation
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(1000);
        fadeIn.setFillAfter(true);

        // Apply animations
        splashLogo.startAnimation(fadeIn);
        splashText.startAnimation(fadeIn);

        // Delayed transition to MainActivity
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(mainIntent);
            
            // Add transition animation
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            
            // Finish splash activity
            finish();
        }, SPLASH_DISPLAY_LENGTH);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Ensure activity finishes when paused (prevents getting stuck)
        finish();
    }
} 