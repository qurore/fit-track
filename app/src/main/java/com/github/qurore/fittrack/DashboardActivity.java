package com.github.qurore.fittrack;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Implement the listener interface
public class DashboardActivity extends AppCompatActivity implements SettingsFragment.OnNameUpdatedListener {

    private TextView headerUsernameTextView;
    private Button logoutButton;
    private ImageButton settingsButton;
    private String userName;
    private BottomNavigationView bottomNavigationView;
    private RequestQueue requestQueue;
    
    // Home content
    private View homeContentLayout;
    private TextView profileName;
    private TextView profileTitle;
    private TextView profileInitials;
    private TextView levelValue;
    private BarChart activityChart;
    private TextView workoutDaysValue;
    private TextView caloriesValue;
    private RecyclerView recentWorkoutsList;
    
    // Workout content
    private ConstraintLayout workoutContentLayout;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private TabPagerAdapter tabPagerAdapter;
    
    // Workout History content
    private View workoutHistoryContentLayout;
    
    // Statistics content (renamed from History)
    private View historyContentLayout;
    private TabLayout historyTabLayout;
    private ViewPager2 historyViewPager;
    private HistoryTabPagerAdapter historyTabPagerAdapter;
    
    // Settings content
    private View settingsContentLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        
        // Initialize Volley RequestQueue
        requestQueue = Volley.newRequestQueue(this);
        
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
        profileName = homeContentLayout.findViewById(R.id.profileName);
        profileTitle = homeContentLayout.findViewById(R.id.profileTitle);
        profileInitials = homeContentLayout.findViewById(R.id.profileInitials);
        levelValue = homeContentLayout.findViewById(R.id.levelValue);
        activityChart = homeContentLayout.findViewById(R.id.activityChart);
        workoutDaysValue = homeContentLayout.findViewById(R.id.workoutDaysValue);
        caloriesValue = homeContentLayout.findViewById(R.id.caloriesValue);
        recentWorkoutsList = homeContentLayout.findViewById(R.id.recentWorkoutsList);
        
        // Initialize Workout content views
        workoutContentLayout = findViewById(R.id.workoutContentLayout);
        tabLayout = workoutContentLayout.findViewById(R.id.tabLayout);
        viewPager = workoutContentLayout.findViewById(R.id.viewPager);
        
        // Initialize Workout History content views
        workoutHistoryContentLayout = findViewById(R.id.workoutHistoryContentLayout);
        
        // Initialize Statistics content views (renamed from History)
        historyContentLayout = findViewById(R.id.historyContentLayout);
        historyTabLayout = historyContentLayout.findViewById(R.id.historyTabLayout);
        historyViewPager = historyContentLayout.findViewById(R.id.historyViewPager);
        
        // Initialize Settings content view
        settingsContentLayout = findViewById(R.id.settingsContentLayout);
        
        // Get user data and update UI
        try {
            String userDataStr = getIntent().getStringExtra("USER_DATA");
            Log.d("DashboardActivity", "Received user data: " + userDataStr);
            
            JSONObject userData = new JSONObject(userDataStr);
            userName = userData.getString("name");
            Log.d("DashboardActivity", "Parsed user name: " + userName);
            
            // Display username in header and profile
            updateUserDisplay(userName);
        } catch (Exception e) {
            Log.e("DashboardActivity", "Error processing user data", e);
            Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show();
        }
        
        // Set up profile data
        profileTitle.setText("Fitness Enthusiast");
        levelValue.setText("8");
        workoutDaysValue.setText("24");
        caloriesValue.setText("8,540");
        
        // Set up activity chart
        setupActivityChart();
        
        // Set up recent workouts
        setupRecentWorkouts();
        
        // Set up logout button
        logoutButton.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
            intent.putExtra("LOGOUT", true);
            startActivity(intent);
            finish();
        });

        // Set up settings button
        settingsButton.setOnClickListener(v -> {
            showSettingsContent();
        });
        
        // Set up tabs for workout page
        setupTabs();
        setupHistoryTabs();
        
        // Set up bottom navigation
        setupBottomNavigation();
    }
    
    // Method to update username display
    private void updateUserDisplay(String name) {
        if (name != null && !name.isEmpty()) {
            this.userName = name; // Update the instance variable
            headerUsernameTextView.setText(name);
            Log.d("DashboardActivity", "Set headerUsernameTextView text to: " + name);

            profileName.setText(name);
            Log.d("DashboardActivity", "Set profileName text to: " + name);

            // Set initials
            String[] nameParts = name.split(" ");
            String initials = "";
            if (nameParts.length > 0 && !nameParts[0].isEmpty()) {
                initials += nameParts[0].charAt(0);
                if (nameParts.length > 1 && !nameParts[nameParts.length - 1].isEmpty()) {
                    initials += nameParts[nameParts.length - 1].charAt(0);
                }
            }
            profileInitials.setText(initials.toUpperCase());
            Log.d("DashboardActivity", "Set profileInitials text to: " + initials.toUpperCase());
        } else {
            Log.e("DashboardActivity", "Username is null or empty in updateUserDisplay");
            // Optionally set default values or show an error state
            headerUsernameTextView.setText("User");
            profileName.setText("User");
            profileInitials.setText("U");
        }
    }
    
    private void setupActivityChart() {
        // Sample data
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, 2));
        entries.add(new BarEntry(1, 4));
        entries.add(new BarEntry(2, 3));
        entries.add(new BarEntry(3, 5));
        entries.add(new BarEntry(4, 2));
        entries.add(new BarEntry(5, 4));
        entries.add(new BarEntry(6, 3));

        BarDataSet dataSet = new BarDataSet(entries, "Workouts");
        dataSet.setColor(getResources().getColor(R.color.colorPrimary));

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);

        activityChart.setData(barData);
        activityChart.getDescription().setEnabled(false);
        activityChart.getLegend().setEnabled(false);
        activityChart.setDrawGridBackground(false);
        activityChart.setDrawBorders(false);

        // Customize X axis
        String[] days = new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        XAxis xAxis = activityChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(days));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        // Customize Y axis
        activityChart.getAxisLeft().setDrawGridLines(false);
        activityChart.getAxisRight().setEnabled(false);

        activityChart.invalidate();
    }
    
    private void setupRecentWorkouts() {
        // Set up RecyclerView
        recentWorkoutsList.setLayoutManager(new LinearLayoutManager(this));
        
        // Set up View All button click listener
        View viewAllWorkouts = homeContentLayout.findViewById(R.id.viewAllWorkouts);
        viewAllWorkouts.setOnClickListener(v -> {
            bottomNavigationView.setSelectedItemId(R.id.navigation_history);
        });
        
        // Sample data
        List<RecentWorkout> workouts = Arrays.asList(
            new RecentWorkout("Upper Body", "Today • 45 min"),
            new RecentWorkout("Cardio", "Yesterday • 30 min"),
            new RecentWorkout("Leg Day", "2 days ago • 60 min")
        );
        
        RecentWorkoutsAdapter adapter = new RecentWorkoutsAdapter(workouts);
        recentWorkoutsList.setAdapter(adapter);
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
                showHomeContent();
                return true;
            } else if (itemId == R.id.navigation_workout) {
                showWorkoutContent();
                return true;
            } else if (itemId == R.id.navigation_history) {
                showWorkoutHistoryContent();
                return true;
            } else if (itemId == R.id.navigation_statistics) {
                showStatisticsContent();
                return true;
            }
            
            return false;
        });
        
        // Set default selection
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
    }
    
    private void showHomeContent() {
        homeContentLayout.setVisibility(View.VISIBLE);
        workoutContentLayout.setVisibility(View.GONE);
        workoutHistoryContentLayout.setVisibility(View.GONE);
        historyContentLayout.setVisibility(View.GONE);
        settingsContentLayout.setVisibility(View.GONE);
    }
    
    private void showWorkoutContent() {
        homeContentLayout.setVisibility(View.GONE);
        workoutContentLayout.setVisibility(View.VISIBLE);
        workoutHistoryContentLayout.setVisibility(View.GONE);
        historyContentLayout.setVisibility(View.GONE);
        settingsContentLayout.setVisibility(View.GONE);
    }
    
    private void showWorkoutHistoryContent() {
        homeContentLayout.setVisibility(View.GONE);
        workoutContentLayout.setVisibility(View.GONE);
        workoutHistoryContentLayout.setVisibility(View.VISIBLE);
        historyContentLayout.setVisibility(View.GONE);
        settingsContentLayout.setVisibility(View.GONE);
        
        // Load the WorkoutHistoryFragment if it's not already added
        if (getSupportFragmentManager().findFragmentById(R.id.workoutHistoryContentLayout) == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.workoutHistoryContentLayout, WorkoutHistoryFragment.newInstance())
                    .commit();
        }
    }
    
    private void showStatisticsContent() {
        homeContentLayout.setVisibility(View.GONE);
        workoutContentLayout.setVisibility(View.GONE);
        workoutHistoryContentLayout.setVisibility(View.GONE);
        historyContentLayout.setVisibility(View.VISIBLE);
        settingsContentLayout.setVisibility(View.GONE);
    }
    
    private void showSettingsContent() {
        homeContentLayout.setVisibility(View.GONE);
        workoutContentLayout.setVisibility(View.GONE);
        workoutHistoryContentLayout.setVisibility(View.GONE);
        historyContentLayout.setVisibility(View.GONE);
        settingsContentLayout.setVisibility(View.VISIBLE);
        
        // Load the SettingsFragment if it's not already added
        if (getSupportFragmentManager().findFragmentById(R.id.settingsContentLayout) == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.settingsContentLayout, SettingsFragment.newInstance(userName))
                    .commit();
        }
        
        // Unselect all bottom navigation items
        bottomNavigationView.getMenu().setGroupCheckable(0, true, false);
        for (int i = 0; i < bottomNavigationView.getMenu().size(); i++) {
            bottomNavigationView.getMenu().getItem(i).setChecked(false);
        }
        bottomNavigationView.getMenu().setGroupCheckable(0, true, true);
    }
    
    // Data class for recent workouts
    private static class RecentWorkout {
        String name;
        String time;
        
        RecentWorkout(String name, String time) {
            this.name = name;
            this.time = time;
        }
    }
    
    // Adapter for recent workouts
    private class RecentWorkoutsAdapter extends RecyclerView.Adapter<RecentWorkoutsAdapter.ViewHolder> {
        private List<RecentWorkout> workouts;
        
        RecentWorkoutsAdapter(List<RecentWorkout> workouts) {
            this.workouts = workouts;
        }
        
        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_recent_workout, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            RecentWorkout workout = workouts.get(position);
            holder.workoutName.setText(workout.name);
            holder.workoutTime.setText(workout.time);
            
            holder.detailsButton.setOnClickListener(v -> {
                // Handle workout details click
                android.widget.Toast.makeText(DashboardActivity.this, 
                    "Viewing details for " + workout.name, 
                    android.widget.Toast.LENGTH_SHORT).show();
            });
        }
        
        @Override
        public int getItemCount() {
            return workouts.size();
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView workoutName;
            TextView workoutTime;
            Button detailsButton;
            
            ViewHolder(View itemView) {
                super(itemView);
                workoutName = itemView.findViewById(R.id.workoutName);
                workoutTime = itemView.findViewById(R.id.workoutTime);
                detailsButton = itemView.findViewById(R.id.detailsButton);
            }
        }
    }

    // Implementation of the interface method
    @Override
    public void onNameUpdated(String newName) {
        Log.d("DashboardActivity", "onNameUpdated called with new name: " + newName);
        updateUserDisplay(newName);

        // Optionally, you might want to navigate away from the settings screen
        // or provide some other visual feedback here.
        // For now, just updating the display fields.

        // If the settings fragment is still visible, we might want to update its internal state too,
        // although SettingsFragment already updates its 'currentUserName'.
        // If needed, find the fragment and update it:
        /*
        SettingsFragment settingsFragment = (SettingsFragment) getSupportFragmentManager().findFragmentById(R.id.settingsContentLayout);
        if (settingsFragment != null) {
            // Potentially call a method on settingsFragment if it needs to react further
        }
        */
    }
} 