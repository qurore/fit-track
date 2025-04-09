package com.github.qurore.fittrack;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.ContextThemeWrapper;

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
    private SimpleDateFormat dateFormat;
    private Calendar startDate;
    private Calendar endDate;
    private List<String> selectedTypes;
    private boolean showCount = true; // true for count, false for minutes

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
        selectedTypes = new ArrayList<>(Arrays.asList("Strength", "Cardio", "Flexibility", "Functional"));
        
        // Initialize calendars
        startDate = Calendar.getInstance();
        endDate = Calendar.getInstance();
        startDate.add(Calendar.DAY_OF_MONTH, -30); // Default to last 30 days
        
        // Initialize views
        titleTextView = view.findViewById(R.id.historyTabContentTitle);
        descriptionTextView = view.findViewById(R.id.historyTabContentDescription);
        startDateText = view.findViewById(R.id.startDateText);
        endDateText = view.findViewById(R.id.endDateText);
        
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
        
        // Set up checkbox listeners
        setupCheckboxListeners();
        
        // Set up radio button listener
        setupRadioGroupListener();
        
        // Set up date selection listeners
        setupDateSelectors();
        
        // Update initial values
        updateDateTexts();
        updateDescription();
    }
    
    private void setupCheckboxListeners() {
        CompoundButton.OnCheckedChangeListener listener = (buttonView, isChecked) -> {
            String type = buttonView.getText().toString();
            if (isChecked) {
                selectedTypes.add(type);
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
        // TODO: Implement graph update based on:
        // - selectedTypes (List<String>)
        // - showCount (boolean) - true for count, false for minutes
        // - startDate (Calendar)
        // - endDate (Calendar)
    }
} 