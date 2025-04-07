package com.github.qurore.fittrack;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class RecordExerciseActivity extends AppCompatActivity {
    public static final String EXTRA_EXERCISE_NAME = "exercise_name";
    public static final String EXTRA_EXERCISE_TYPE = "exercise_type";

    private TextView exerciseName;
    private Button datePickerButton;
    private Button timePickerButton;
    private TextInputLayout weightLayout;
    private TextInputLayout repsLayout;
    private TextInputLayout distanceLayout;
    private TextInputEditText durationInput;
    private TextInputEditText weightInput;
    private TextInputEditText repsInput;
    private TextInputEditText distanceInput;
    private TextInputEditText notesInput;
    private Button saveButton;

    private Calendar selectedDateTime;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat timeFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_exercise);

        // Initialize date and time formats
        dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        selectedDateTime = Calendar.getInstance();

        // Initialize views
        exerciseName = findViewById(R.id.exerciseName);
        datePickerButton = findViewById(R.id.datePickerButton);
        timePickerButton = findViewById(R.id.timePickerButton);
        weightLayout = findViewById(R.id.weightLayout);
        repsLayout = findViewById(R.id.repsLayout);
        distanceLayout = findViewById(R.id.distanceLayout);
        durationInput = findViewById(R.id.durationInput);
        weightInput = findViewById(R.id.weightInput);
        repsInput = findViewById(R.id.repsInput);
        distanceInput = findViewById(R.id.distanceInput);
        notesInput = findViewById(R.id.notesInput);
        saveButton = findViewById(R.id.saveButton);

        // Get exercise details from intent
        String name = getIntent().getStringExtra(EXTRA_EXERCISE_NAME);
        String type = getIntent().getStringExtra(EXTRA_EXERCISE_TYPE);

        // Set exercise name
        exerciseName.setText(name);

        // Show/hide fields based on exercise type
        setupFieldsForExerciseType(type);

        // Set up date picker
        datePickerButton.setOnClickListener(v -> showDatePicker());
        timePickerButton.setOnClickListener(v -> showTimePicker());

        // Update button texts with current date and time
        updateDateTimeButtons();

        // Set up save button
        saveButton.setOnClickListener(v -> saveExercise(type));
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select exercise date")
                .setSelection(selectedDateTime.getTimeInMillis())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            // Update selected date while preserving time
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.setTimeInMillis(selection);
            
            selectedDateTime.set(Calendar.YEAR, calendar.get(Calendar.YEAR));
            selectedDateTime.set(Calendar.MONTH, calendar.get(Calendar.MONTH));
            selectedDateTime.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH));
            
            updateDateTimeButtons();
        });

        datePicker.show(getSupportFragmentManager(), "date_picker");
    }

    private void showTimePicker() {
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(selectedDateTime.get(Calendar.HOUR_OF_DAY))
                .setMinute(selectedDateTime.get(Calendar.MINUTE))
                .setTitleText("Select exercise time")
                .build();

        timePicker.addOnPositiveButtonClickListener(v -> {
            selectedDateTime.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
            selectedDateTime.set(Calendar.MINUTE, timePicker.getMinute());
            updateDateTimeButtons();
        });

        timePicker.show(getSupportFragmentManager(), "time_picker");
    }

    private void updateDateTimeButtons() {
        datePickerButton.setText(dateFormat.format(selectedDateTime.getTime()));
        timePickerButton.setText(timeFormat.format(selectedDateTime.getTime()));
    }

    private void setupFieldsForExerciseType(String type) {
        // Reset all fields to GONE
        weightLayout.setVisibility(View.GONE);
        repsLayout.setVisibility(View.GONE);
        distanceLayout.setVisibility(View.GONE);

        // Show relevant fields based on type
        switch (type) {
            case "Strength":
                weightLayout.setVisibility(View.VISIBLE);
                repsLayout.setVisibility(View.VISIBLE);
                break;
            case "Cardio":
                distanceLayout.setVisibility(View.VISIBLE);
                break;
            case "Flexibility":
            case "Functional":
                // Only duration and notes are visible by default
                break;
        }
    }

    private void saveExercise(String type) {
        // Validate required fields
        if (!validateCommonFields()) {
            return;
        }

        // Get common field values
        long startTime = selectedDateTime.getTimeInMillis();
        int duration = Integer.parseInt(durationInput.getText().toString());
        String notes = notesInput.getText().toString();

        try {
            switch (type) {
                case "Strength":
                    if (!validateStrengthFields()) {
                        return;
                    }
                    float weight = Float.parseFloat(weightInput.getText().toString());
                    int reps = Integer.parseInt(repsInput.getText().toString());
                    saveStrengthExercise(exerciseName.getText().toString(), startTime, duration, weight, reps, notes);
                    break;

                case "Cardio":
                    Float distance = null;
                    if (!distanceInput.getText().toString().isEmpty()) {
                        distance = Float.parseFloat(distanceInput.getText().toString());
                    }
                    saveCardioExercise(exerciseName.getText().toString(), startTime, duration, distance, notes);
                    break;

                case "Flexibility":
                    saveFlexibilityExercise(exerciseName.getText().toString(), startTime, duration, notes);
                    break;

                case "Functional":
                    saveFunctionalExercise(exerciseName.getText().toString(), startTime, duration, notes);
                    break;
            }

            // Show success message and finish activity
            Toast.makeText(this, "Exercise saved successfully", Toast.LENGTH_SHORT).show();
            finish();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateCommonFields() {
        if (durationInput.getText().toString().isEmpty()) {
            durationInput.setError("Duration is required");
            return false;
        }
        return true;
    }

    private boolean validateStrengthFields() {
        if (weightInput.getText().toString().isEmpty()) {
            weightInput.setError("Weight is required");
            return false;
        }
        if (repsInput.getText().toString().isEmpty()) {
            repsInput.setError("Repetitions are required");
            return false;
        }
        return true;
    }

    private void saveStrengthExercise(String name, long startTime, int duration, float weight, int reps, String notes) {
        // TODO: Save to database
    }

    private void saveCardioExercise(String name, long startTime, int duration, Float distance, String notes) {
        // TODO: Save to database
    }

    private void saveFlexibilityExercise(String name, long startTime, int duration, String notes) {
        // TODO: Save to database
    }

    private void saveFunctionalExercise(String name, long startTime, int duration, String notes) {
        // TODO: Save to database
    }
} 