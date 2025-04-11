package com.github.qurore.fittrack;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import android.view.ContextThemeWrapper;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.qurore.fittrack.repository.ExerciseRepository;
import com.github.qurore.fittrack.services.ExerciseService;

import org.json.JSONObject;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class HistoryTabContentFragment extends Fragment {
    private TextView titleTextView;
    private TextView descriptionTextView;
    private TextView startDateText;
    private TextView endDateText;
    private CheckBox strengthCheckbox;
    private CheckBox cardioCheckbox;
    private CheckBox flexibilityCheckbox;
    private CheckBox functionalCheckbox;
    private RadioGroup metricRadioGroup;
    private FrameLayout graphContainer;
    private BarChart barChart;
    
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat displayDateFormat;
    private Calendar startDate;
    private Calendar endDate;
    private List<String> selectedTypes;
    private boolean showCount = true; // true for count, false for minutes
    private ExerciseRepository exerciseRepository;
    private ExerciseService exerciseService;
    private SwipeRefreshLayout swipeRefreshLayout;

    public static HistoryTabContentFragment newInstance() {
        return new HistoryTabContentFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history_tab_content, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        displayDateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
        selectedTypes = new ArrayList<>(Arrays.asList("Strength", "Cardio", "Flexibility", "Functional"));
        exerciseRepository = ExerciseRepository.getInstance(requireContext());
        exerciseService = new ExerciseService(requireContext());
        
        // Initialize calendars
        startDate = Calendar.getInstance();
        endDate = Calendar.getInstance();
        startDate.add(Calendar.DAY_OF_MONTH, -30); // Default to last 30 days
        
        // Initialize views
        titleTextView = view.findViewById(R.id.historyTabContentTitle);
        descriptionTextView = view.findViewById(R.id.historyTabContentDescription);
        startDateText = view.findViewById(R.id.startDateText);
        endDateText = view.findViewById(R.id.endDateText);
        graphContainer = view.findViewById(R.id.graphContainer);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        
        // Initialize checkboxes
        strengthCheckbox = view.findViewById(R.id.strengthCheckbox);
        cardioCheckbox = view.findViewById(R.id.cardioCheckbox);
        flexibilityCheckbox = view.findViewById(R.id.flexibilityCheckbox);
        functionalCheckbox = view.findViewById(R.id.functionalCheckbox);
        
        // Initialize radio group
        metricRadioGroup = view.findViewById(R.id.metricRadioGroup);
        
        // Set all checkboxes checked by default
        strengthCheckbox.setChecked(true);
        cardioCheckbox.setChecked(true);
        flexibilityCheckbox.setChecked(true);
        functionalCheckbox.setChecked(true);
        
        // Initialize and set up the chart
        setupChart();
        
        // Set up checkbox listeners
        setupCheckboxListeners();
        
        // Set up radio button listener
        setupRadioGroupListener();
        
        // Set up date selection listeners
        setupDateSelectors();
        
        // Update initial values
        updateDateTexts();
        updateDescription();
        
        // Set up observers
        setupObservers();
        
        // Load data and update graph
        updateGraph();
        
        // Setup SwipeRefreshLayout
        setupSwipeRefreshLayout();
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private void setupChart() {
        // Create and add the chart to the container
        barChart = new BarChart(requireContext());
        graphContainer.removeAllViews();
        graphContainer.addView(barChart);
        
        // Setup chart appearance
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBorders(false);
        barChart.setScaleEnabled(true);
        barChart.setPinchZoom(true);
        barChart.getLegend().setEnabled(false);
        
        // Setup X axis
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        
        // Setup Y axis
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setGranularity(1f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setValueFormatter(new IntegerValueFormatter());
        barChart.getAxisRight().setEnabled(false);
        
        // Add padding
        barChart.setExtraOffsets(10f, 10f, 10f, 10f);
    }
    
    private void setupCheckboxListeners() {
        CompoundButton.OnCheckedChangeListener listener = (buttonView, isChecked) -> {
            String type = buttonView.getText().toString();
            if (isChecked) {
                if (!selectedTypes.contains(type)) {
                    selectedTypes.add(type);
                }
            } else {
                selectedTypes.remove(type);
            }
            updateDescription();
            updateGraph();
        };
        
        strengthCheckbox.setOnCheckedChangeListener(listener);
        cardioCheckbox.setOnCheckedChangeListener(listener);
        flexibilityCheckbox.setOnCheckedChangeListener(listener);
        functionalCheckbox.setOnCheckedChangeListener(listener);
    }
    
    private void setupRadioGroupListener() {
        metricRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            showCount = (checkedId == R.id.countRadioButton);
            updateDescription();
            updateGraph();
        });
    }
    
    private void setupDateSelectors() {
        startDateText.setOnClickListener(v -> showDatePicker(true));
        endDateText.setOnClickListener(v -> showDatePicker(false));
    }
    
    private void showDatePicker(boolean isStartDate) {
        Calendar calendar = isStartDate ? startDate : endDate;
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            new ContextThemeWrapper(requireContext(), R.style.DatePickerDialogTheme),
            (view, year, month, dayOfMonth) -> {
                Calendar selectedDate = Calendar.getInstance();
                selectedDate.set(year, month, dayOfMonth);
                
                // Validate date range
                if (isStartDate) {
                    if (!selectedDate.after(endDate)) {
                        startDate = selectedDate;
                    } else {
                        Toast.makeText(requireContext(), 
                            "Start date cannot be after end date", 
                            Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else {
                    if (!selectedDate.before(startDate)) {
                        endDate = selectedDate;
                    } else {
                        Toast.makeText(requireContext(), 
                            "End date cannot be before start date", 
                            Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                
                updateDateTexts();
                updateDescription();
                updateGraph();
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        
        // Set date bounds
        if (isStartDate) {
            datePickerDialog.getDatePicker().setMaxDate(endDate.getTimeInMillis());
        } else {
            datePickerDialog.getDatePicker().setMinDate(startDate.getTimeInMillis());
        }
        
        datePickerDialog.show();
    }
    
    private void updateDateTexts() {
        startDateText.setText(dateFormat.format(startDate.getTime()));
        endDateText.setText(dateFormat.format(endDate.getTime()));
    }
    
    private void updateDescription() {
        String description;
        if (selectedTypes.isEmpty()) {
            description = "Select exercise types to view statistics";
        } else {
            String metric = showCount ? "count" : "minutes";
            description = String.format("Showing %s for %s", 
                metric,
                String.join(", ", selectedTypes));
        }
        descriptionTextView.setText(description);
    }
    
    private void updateGraph() {
        if (selectedTypes.isEmpty()) {
            // Clear chart if no types selected
            barChart.clear();
            barChart.invalidate();
            return;
        }
        
        // Use the common refresh method
        refreshStatisticsData();
    }
    
    private void refreshStatisticsData() {
        // Show loading indicator in description
        descriptionTextView.setText("Loading data...");
        
        // Trigger a refresh to get the latest data
        exerciseRepository.refreshExercises();
        
        // The actual data processing is handled by the observer
    }
    
    // Helper to capitalize the first letter of a string
    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    // Helper to truncate timestamp to day (for grouping)
    private long getDayKey(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    // Custom ValueFormatter to display integers
    private static class IntegerValueFormatter extends ValueFormatter {
        @Override
        public String getFormattedValue(float value) {
            // Display integer values, hide zero values for cleaner look
            if (value == 0) {
                return "";
            }
            return String.valueOf((int) value);
        }
    }

    private void setupObservers() {
        // Observe exercise data
        exerciseRepository.getExercises().observe(getViewLifecycleOwner(), exercises -> {
            // Process data when it changes
            processExerciseData(exercises);
        });
        
        // Observe loading state
        exerciseRepository.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isAdded() && isLoading) {
                descriptionTextView.setText("Loading data...");
            } else if (isAdded()) {
                updateDescription();
            }
        });
        
        // Observe error messages
        exerciseRepository.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (isAdded() && errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                updateDescription();
            }
        });
    }

    private void processExerciseData(List<JSONObject> exercises) {
        // Get calendar instances for the start and end dates
        Calendar startCal = (Calendar) startDate.clone();
        Calendar endCal = (Calendar) endDate.clone();
        
        // Set time to start of day for start date
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        
        // Set time to end of day for end date
        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 999);
        
        // Calculate time range
        long startTime = startCal.getTimeInMillis();
        long endTime = endCal.getTimeInMillis();
        
        // Map to store the data (day -> value)
        TreeMap<Long, Integer> dayData = new TreeMap<>();
        
        // Track days in the range to ensure we have entries for all days
        List<Long> daysInRange = new ArrayList<>();
        List<String> dayLabels = new ArrayList<>();
        
        // Add all days in the range
        Calendar cal = (Calendar) startDate.clone();
        cal.set(Calendar.HOUR_OF_DAY, 12); // Noon to avoid DST issues
        
        while (!cal.after(endDate)) {
            long dayKey = getDayKey(cal.getTimeInMillis());
            daysInRange.add(dayKey);
            dayLabels.add(displayDateFormat.format(cal.getTime()));
            dayData.put(dayKey, 0); // Initialize with zero
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        // Process each exercise
        for (JSONObject exercise : exercises) {
            try {
                // Get exercise data
                long time = exercise.getLong("start_time");
                String typeLower = exercise.getString("exercise_type");
                
                // Skip if outside date range
                if (time < startTime || time > endTime) {
                    continue;
                }
                
                // Convert fetched type to title case for comparison
                String typeTitleCase = capitalizeFirstLetter(typeLower);
                
                // Skip if exercise type not selected
                if (!selectedTypes.contains(typeTitleCase)) {
                    continue;
                }
                
                // Get the day key (truncate to day)
                long dayKey = getDayKey(time);
                
                // Update the count or minutes based on metric
                int value = 0;
                if (showCount) {
                    // Count exercise
                    value = 1;
                } else {
                    // Sum duration in minutes
                    value = exercise.getInt("duration");
                }
                
                // Update the day's data
                if (dayData.containsKey(dayKey)) {
                    dayData.put(dayKey, dayData.get(dayKey) + value);
                } else {
                    dayData.put(dayKey, value);
                }
                
            } catch (Exception e) {
                Log.e("HistoryFragment", "Error processing exercise data", e);
            }
        }
        
        // Create chart entries
        ArrayList<BarEntry> entries = new ArrayList<>();
        
        // Create entries for each day
        for (int i = 0; i < daysInRange.size(); i++) {
            long dayKey = daysInRange.get(i);
            int value = dayData.get(dayKey);
            entries.add(new BarEntry(i, value));
        }
        
        // Update chart on UI thread
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // Create the data set
                BarDataSet dataSet = new BarDataSet(entries, showCount ? "Exercise Count" : "Exercise Minutes");
                dataSet.setColor(getResources().getColor(R.color.primary));
                dataSet.setDrawValues(true);
                dataSet.setValueFormatter(new IntegerValueFormatter());
                
                // Create and set the data
                BarData barData = new BarData(dataSet);
                barData.setBarWidth(0.5f);
                
                // Set the labels
                barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(dayLabels));
                
                // Update the chart
                barChart.setData(barData);
                barChart.invalidate();
                
                // Update description
                updateDescription();
            });
        }
    }

    private void setupSwipeRefreshLayout() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.primary));
            swipeRefreshLayout.setOnRefreshListener(this::refreshStatisticsData);
            
            // Observe loading state from repository
            exerciseRepository.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
                if (swipeRefreshLayout != null && isLoading != null) {
                    swipeRefreshLayout.setRefreshing(isLoading);
                }
            });
        }
    }
} 