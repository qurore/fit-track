package com.github.qurore.fittrack;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DashboardActivity extends AppCompatActivity {

    private TextView userInfoTextView;
    private TextView historyContentTextView;
    private TextView settingsContentTextView;
    private TextView headerUsernameTextView;
    private Button logoutButton;
    private String userName;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        
        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        
        // Initialize views
        userInfoTextView = findViewById(R.id.userInfoTextView);
        historyContentTextView = findViewById(R.id.historyContentTextView);
        settingsContentTextView = findViewById(R.id.settingsContentTextView);
        headerUsernameTextView = findViewById(R.id.headerUsernameTextView);
        logoutButton = findViewById(R.id.logoutButton);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        
        // Get user data from intent
        userName = getIntent().getStringExtra("USER_NAME");
        String userEmail = getIntent().getStringExtra("USER_EMAIL");
        
        // Display user information
        if (userName != null && userEmail != null) {
            headerUsernameTextView.setText(userName);
            userInfoTextView.setText(String.format("Welcome, %s!\nEmail: %s", userName, userEmail));
        } else {
            headerUsernameTextView.setText("User");
            userInfoTextView.setText("Welcome to FitTrack!");
        }
        
        // Set up logout button
        logoutButton.setOnClickListener(v -> {
            // Return to MainActivity and trigger logout
            Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
            intent.putExtra("LOGOUT", true);
            startActivity(intent);
            finish();
        });
        
        // Set up bottom navigation
        setupBottomNavigation();
    }
    
    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.navigation_home) {
                // Show home content, hide others
                showHomeContent();
                return true;
            } else if (itemId == R.id.navigation_history) {
                // Show history content, hide others
                showHistoryContent();
                return true;
            } else if (itemId == R.id.navigation_settings) {
                // Show settings content, hide others
                showSettingsContent();
                return true;
            }
            
            return false;
        });
        
        // Set default selection
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
    }
    
    private void showHomeContent() {
        userInfoTextView.setVisibility(View.VISIBLE);
        historyContentTextView.setVisibility(View.GONE);
        settingsContentTextView.setVisibility(View.GONE);
    }
    
    private void showHistoryContent() {
        userInfoTextView.setVisibility(View.GONE);
        historyContentTextView.setVisibility(View.VISIBLE);
        settingsContentTextView.setVisibility(View.GONE);
    }
    
    private void showSettingsContent() {
        userInfoTextView.setVisibility(View.GONE);
        historyContentTextView.setVisibility(View.GONE);
        settingsContentTextView.setVisibility(View.VISIBLE);
    }
} 