package com.github.qurore.fittrack;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class DashboardActivity extends AppCompatActivity {

    private TextView userInfoTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        
        // Initialize views
        userInfoTextView = findViewById(R.id.userInfoTextView);
        
        // Get user data from intent
        String userName = getIntent().getStringExtra("USER_NAME");
        String userEmail = getIntent().getStringExtra("USER_EMAIL");
        
        // Display user information
        if (userName != null && userEmail != null) {
            userInfoTextView.setText(String.format("Welcome, %s!\nEmail: %s", userName, userEmail));
        } else {
            userInfoTextView.setText("Welcome to FitTrack!");
        }
    }
} 