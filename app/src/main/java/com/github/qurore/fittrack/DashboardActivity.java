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
import com.github.qurore.fittrack.services.ExerciseService;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Date;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicInteger;

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
    private BarChart activityChart;
    private TextView workoutDaysValue;
    private TextView totalDurationValue;
    private TextView weekRangeText;
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
        activityChart = homeContentLayout.findViewById(R.id.activityChart);
        workoutDaysValue = homeContentLayout.findViewById(R.id.workoutDaysValue);
        totalDurationValue = homeContentLayout.findViewById(R.id.totalDurationValue);
        weekRangeText = homeContentLayout.findViewById(R.id.weekRangeText);
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
            
            // Display username in header
            headerUsernameTextView.setText(userName);
        } catch (Exception e) {
            Log.e("DashboardActivity", "Error processing user data", e);
            Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show();
        }
        
        // Set up workout days and total duration values
        workoutDaysValue.setText("-");
        totalDurationValue.setText("-");
        
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
    
    private void setupActivityChart() {
        ExerciseService exerciseService = new ExerciseService(this);
        
        exerciseService.getAllExercises(new ExerciseService.ExerciseCallback() {
            @Override
            public void onSuccess(List<JSONObject> exercises) {
                // Get current week's data
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                long weekStart = calendar.getTimeInMillis();

                // Calculate week end date
                Calendar endCalendar = (Calendar) calendar.clone();
                endCalendar.add(Calendar.DAY_OF_WEEK, 6);
                endCalendar.set(Calendar.HOUR_OF_DAY, 23);
                endCalendar.set(Calendar.MINUTE, 59);
                endCalendar.set(Calendar.SECOND, 59);
                endCalendar.set(Calendar.MILLISECOND, 999);
                long weekEnd = endCalendar.getTimeInMillis();

                // Format date range
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());
                String startDate = dateFormat.format(calendar.getTime());
                String endDate = dateFormat.format(endCalendar.getTime());
                
                // Add year if the week spans different years
                if (calendar.get(Calendar.YEAR) != endCalendar.get(Calendar.YEAR)) {
                    dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
                    startDate = dateFormat.format(calendar.getTime());
                    endDate = dateFormat.format(endCalendar.getTime());
                } else if (endCalendar.get(Calendar.YEAR) != Calendar.getInstance().get(Calendar.YEAR)) {
                    // Add year if the week is not in the current year
                    startDate += ", " + calendar.get(Calendar.YEAR);
                    endDate += ", " + endCalendar.get(Calendar.YEAR);
                }

                final String dateRange = startDate + " - " + endDate;

                // Count workouts per day and calculate total duration for current week
                int[] dailyWorkouts = new int[7];
                AtomicInteger weeklyWorkouts = new AtomicInteger(0);
                AtomicInteger weeklyDuration = new AtomicInteger(0);

                // Process exercises
                for (JSONObject exercise : exercises) {
                    try {
                        // Process weekly data
                        long startTime = exercise.getLong("start_time");
                        if (startTime >= weekStart && startTime <= weekEnd) {
                            // Increment weekly workout count
                            weeklyWorkouts.incrementAndGet();
                            
                            // Add duration to weekly total
                            weeklyDuration.addAndGet(exercise.getInt("duration"));

                            // Update daily workout count
                            Calendar exerciseDate = Calendar.getInstance();
                            exerciseDate.setTimeInMillis(startTime);
                            int dayOfWeek = exerciseDate.get(Calendar.DAY_OF_WEEK);
                            // Adjust index based on first day of week
                            int index = (dayOfWeek - calendar.getFirstDayOfWeek() + 7) % 7;
                            dailyWorkouts[index]++;
                        }
                    } catch (Exception e) {
                        Log.e("DashboardActivity", "Error processing exercise data", e);
                    }
                }

                // Update statistics on UI thread
                runOnUiThread(() -> {
                    workoutDaysValue.setText(String.valueOf(weeklyWorkouts.get()));
                    totalDurationValue.setText(String.valueOf(weeklyDuration.get()));
                    weekRangeText.setText(dateRange);
                });

                // Create chart entries
                ArrayList<BarEntry> entries = new ArrayList<>();
                for (int i = 0; i < 7; i++) {
                    entries.add(new BarEntry(i, dailyWorkouts[i]));
                }

                // Update chart on UI thread
                runOnUiThread(() -> {
                    BarDataSet dataSet = new BarDataSet(entries, "Workouts");
                    dataSet.setColor(getResources().getColor(R.color.colorPrimary));

                    BarData barData = new BarData(dataSet);
                    barData.setBarWidth(0.5f);

                    activityChart.setData(barData);
                    activityChart.getDescription().setEnabled(false);
                    activityChart.getLegend().setEnabled(false);
                    activityChart.setDrawGridBackground(false);
                    activityChart.setDrawBorders(false);

                    // Customize X axis with day names
                    String[] days = getDayNames();
                    XAxis xAxis = activityChart.getXAxis();
                    xAxis.setValueFormatter(new IndexAxisValueFormatter(days));
                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                    xAxis.setDrawGridLines(false);
                    xAxis.setGranularity(1f);

                    // Customize Y axis
                    activityChart.getAxisLeft().setDrawGridLines(false);
                    activityChart.getAxisRight().setEnabled(false);

                    activityChart.invalidate();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(DashboardActivity.this, 
                        "Error loading activity data: " + error, 
                        Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private String[] getDayNames() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        String[] days = new String[7];
        SimpleDateFormat sdf = new SimpleDateFormat("EEE", Locale.getDefault());
        
        for (int i = 0; i < 7; i++) {
            days[i] = sdf.format(calendar.getTime());
            calendar.add(Calendar.DAY_OF_WEEK, 1);
        }
        
        return days;
    }
    
    private void setupRecentWorkouts() {
        // Set up RecyclerView
        recentWorkoutsList.setLayoutManager(new LinearLayoutManager(this));
        
        // Set up View All button click listener
        View viewAllWorkouts = homeContentLayout.findViewById(R.id.viewAllWorkouts);
        viewAllWorkouts.setOnClickListener(v -> {
            bottomNavigationView.setSelectedItemId(R.id.navigation_history);
        });

        // Initialize ExerciseService
        ExerciseService exerciseService = new ExerciseService(this);
        
        // Fetch recent workouts
        exerciseService.getAllExercises(new ExerciseService.ExerciseCallback() {
            @Override
            public void onSuccess(List<JSONObject> exercises) {
                // Sort exercises by start time in descending order (most recent first)
                exercises.sort((a, b) -> {
                    try {
                        long timeA = a.getLong("start_time");
                        long timeB = b.getLong("start_time");
                        return Long.compare(timeB, timeA); // Descending order
                    } catch (Exception e) {
                        return 0;
                    }
                });

                // Take only the top 3 most recent workouts
                List<RecentWorkout> recentWorkouts = new ArrayList<>();
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                
                for (int i = 0; i < Math.min(3, exercises.size()); i++) {
                    try {
                        JSONObject exercise = exercises.get(i);
                        String id = exercise.getString("_id");
                        String typeSubtype = capitalizeWords(exercise.getString("exercise_type")) + " - " +
                                           capitalizeWords(exercise.getString("exercise_subtype"));
                        String name = capitalizeWords(exercise.getString("exercise_name"));
                        
                        // Format the time string
                        long startTime = exercise.getLong("start_time");
                        int duration = exercise.getInt("duration");
                        String timeStr = dateFormat.format(new Date(startTime)) + " • " + duration + " min";

                        recentWorkouts.add(new RecentWorkout(id, typeSubtype, name, timeStr, exercise));
                    } catch (Exception e) {
                        Log.e("DashboardActivity", "Error processing exercise", e);
                    }
                }

                // Update the RecyclerView on the main thread
                runOnUiThread(() -> {
                    RecentWorkoutsAdapter adapter = new RecentWorkoutsAdapter(recentWorkouts);
                    recentWorkoutsList.setAdapter(adapter);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(DashboardActivity.this, 
                        "Error loading recent workouts: " + error, 
                        Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private String capitalizeWords(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        String[] words = text.toLowerCase().split("\\s+");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (!word.isEmpty()) {
                if (result.length() > 0) {
                    result.append(" ");
                }
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1));
            }
        }
        
        return result.toString();
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
    
    public void showWorkoutContent() {
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
        
        // Initialize WorkoutHistoryFragment if not already done
        if (getSupportFragmentManager().findFragmentById(R.id.workoutHistoryContentLayout) == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.workoutHistoryContentLayout, WorkoutHistoryFragment.newInstance())
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
        
        // Initialize SettingsFragment if not already done
        if (getSupportFragmentManager().findFragmentById(R.id.settingsContentLayout) == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.settingsContentLayout, SettingsFragment.newInstance(userName))
                .commit();
        }
    }
    
    // Data class for recent workouts
    private static class RecentWorkout {
        String id;
        String typeSubtype;
        String name;
        String time;
        JSONObject completeData;
        
        RecentWorkout(String id, String typeSubtype, String name, String time, JSONObject completeData) {
            this.id = id;
            this.typeSubtype = typeSubtype;
            this.name = name;
            this.time = time;
            this.completeData = completeData;
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
            holder.exerciseTypeSubtype.setText(workout.typeSubtype);
            holder.workoutName.setText(workout.name);
            holder.workoutTime.setText(workout.time);
            
            holder.detailsButton.setOnClickListener(v -> {
                // Open RecordExerciseActivity in edit mode with the exercise data
                try {
                    // Create intent to launch RecordExerciseActivity
                    Intent intent = new Intent(DashboardActivity.this, RecordExerciseActivity.class);
                    
                    // Extract exercise type and subtype from the combined string
                    String[] typeParts = workout.typeSubtype.split(" - ");
                    String type = typeParts[0].trim();
                    String subtype = typeParts.length > 1 ? typeParts[1].trim() : "";
                    
                    // Set edit mode and exercise ID
                    intent.putExtra(RecordExerciseActivity.EXTRA_EDIT_MODE, true);
                    intent.putExtra(RecordExerciseActivity.EXTRA_EXERCISE_ID, workout.id);
                    
                    // Put exercise data
                    intent.putExtra(RecordExerciseActivity.EXTRA_EXERCISE_NAME, workout.name);
                    intent.putExtra(RecordExerciseActivity.EXTRA_EXERCISE_TYPE, type);
                    intent.putExtra(RecordExerciseActivity.EXTRA_EXERCISE_SUBTYPE, subtype);
                    
                    // Include full exercise details as a JSON string
                    intent.putExtra(RecordExerciseActivity.EXTRA_EXERCISE_DATA, workout.completeData.toString());
                    
                    // Start activity
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(DashboardActivity.this, 
                        "Error opening exercise: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        @Override
        public int getItemCount() {
            return workouts.size();
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView exerciseTypeSubtype;
            TextView workoutName;
            TextView workoutTime;
            ImageButton detailsButton;
            
            ViewHolder(View itemView) {
                super(itemView);
                exerciseTypeSubtype = itemView.findViewById(R.id.exerciseTypeSubtype);
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
        headerUsernameTextView.setText(newName);

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