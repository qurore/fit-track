package com.github.qurore.fittrack;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class DashboardActivity extends AppCompatActivity {

    private TextView headerUsernameTextView;
    private Button logoutButton;
    private ImageButton settingsButton;
    private String userName;
    private BottomNavigationView bottomNavigationView;
    
    // Home content
    private ConstraintLayout homeContentLayout;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private TabPagerAdapter tabPagerAdapter;
    
    // History content
    private View historyContentLayout;
    private TabLayout historyTabLayout;
    private ViewPager2 historyViewPager;
    private HistoryTabPagerAdapter historyTabPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        
        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        
        // Initialize views
        headerUsernameTextView = findViewById(R.id.headerUsernameTextView);
        logoutButton = findViewById(R.id.logoutButton);
        settingsButton = findViewById(R.id.settingsButton);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        
        // Initialize Home content views
        homeContentLayout = findViewById(R.id.homeContentLayout);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        
        // Initialize History content views
        historyContentLayout = findViewById(R.id.historyContentLayout);
        historyTabLayout = historyContentLayout.findViewById(R.id.historyTabLayout);
        historyViewPager = historyContentLayout.findViewById(R.id.historyViewPager);
        
        // Get user data from intent
        userName = getIntent().getStringExtra("USER_NAME");
        
        // Display username in header
        if (userName != null) {
            headerUsernameTextView.setText(userName);
        } else {
            headerUsernameTextView.setText("User");
        }
        
        // Set up logout button
        logoutButton.setOnClickListener(v -> {
            // Return to MainActivity and trigger logout
            Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
            intent.putExtra("LOGOUT", true);
            startActivity(intent);
            finish();
        });

        // Set up settings button
        settingsButton.setOnClickListener(v -> {
            // Show settings content
            showSettingsContent();
        });
        
        // Set up tabs
        setupTabs();
        setupHistoryTabs();
        
        // Set up bottom navigation
        setupBottomNavigation();
    }
    
    private void setupTabs() {
        // Initialize the ViewPager adapter
        tabPagerAdapter = new TabPagerAdapter(this);
        viewPager.setAdapter(tabPagerAdapter);
        
        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(tabPagerAdapter.getTabTitle(position));
        }).attach();
    }
    
    private void setupHistoryTabs() {
        // Initialize the ViewPager adapter for History
        historyTabPagerAdapter = new HistoryTabPagerAdapter(this);
        historyViewPager.setAdapter(historyTabPagerAdapter);
        
        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(historyTabLayout, historyViewPager, (tab, position) -> {
            tab.setText(historyTabPagerAdapter.getTabTitle(position));
        }).attach();
    }
    
    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.navigation_home) {
                // Show home content, hide others
                showHomeContent();
                return true;
            } else if (itemId == R.id.navigation_workout) {
                // Show workout content (to be implemented)
                // For now, show home content
                showHomeContent();
                return true;
            } else if (itemId == R.id.navigation_history) {
                // Show history content, hide others
                showHistoryContent();
                return true;
            }
            
            return false;
        });
        
        // Set default selection
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
    }
    
    private void showHomeContent() {
        homeContentLayout.setVisibility(View.VISIBLE);
        historyContentLayout.setVisibility(View.GONE);
    }
    
    private void showHistoryContent() {
        homeContentLayout.setVisibility(View.GONE);
        historyContentLayout.setVisibility(View.VISIBLE);
    }
    
    private void showSettingsContent() {
        // To be implemented
        // For now, just show a toast message
        android.widget.Toast.makeText(this, "Settings clicked", android.widget.Toast.LENGTH_SHORT).show();
    }
} 