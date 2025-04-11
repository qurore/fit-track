package com.github.qurore.fittrack;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.github.qurore.fittrack.repository.ExerciseRepository;
import com.github.qurore.fittrack.services.ExerciseService;

import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class RecordExerciseActivity extends AppCompatActivity {
    private static final String TAG = "RecordExerciseActivity";
    private static final String API_URL = "https://xg95njnqd7.execute-api.us-west-2.amazonaws.com/Prod/exercises";
    
    public static final String EXTRA_EXERCISE_NAME = "exercise_name";
    public static final String EXTRA_EXERCISE_TYPE = "exercise_type";
    public static final String EXTRA_EXERCISE_SUBTYPE = "exercise_subtype";
    public static final String EXTRA_EDIT_MODE = "edit_mode";
    public static final String EXTRA_EXERCISE_ID = "exercise_id";
    public static final String EXTRA_EXERCISE_DATA = "exercise_data";

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

    private ExerciseRepository exerciseRepository;
    private FirebaseAuth mAuth;
    
    private boolean isEditMode = false;
    private String exerciseId = null;
    private JSONObject originalExerciseData = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_exercise);

        // Initialize Firebase Auth and ExerciseRepository
        mAuth = FirebaseAuth.getInstance();
        exerciseRepository = ExerciseRepository.getInstance(this);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

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
        String subtype = getIntent().getStringExtra(EXTRA_EXERCISE_SUBTYPE);
        
        // Check if in edit mode
        isEditMode = getIntent().getBooleanExtra(EXTRA_EDIT_MODE, false);
        if (isEditMode) {
            exerciseId = getIntent().getStringExtra(EXTRA_EXERCISE_ID);
            String exerciseDataString = getIntent().getStringExtra(EXTRA_EXERCISE_DATA);
            
            // Set window title to reflect edit mode
            setTitle("Edit Exercise");
            saveButton.setText("Update Exercise");
            
            // Load exercise data
            if (exerciseDataString != null) {
                try {
                    originalExerciseData = new JSONObject(exerciseDataString);
                    populateFieldsFromExerciseData(originalExerciseData, type);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing exercise data", e);
                    Toast.makeText(this, "Error loading exercise data", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            }
        } else {
            setTitle("Record Exercise");
        }

        // Set exercise name and update title with type and subtype
        exerciseName.setText(name);
        TextView exerciseNameTitle = findViewById(R.id.exerciseNameTitle);
        String titleText = type + " - " + subtype;
        exerciseNameTitle.setText(titleText);

        // Show/hide fields based on exercise type
        setupFieldsForExerciseType(type);

        // Set up date picker
        datePickerButton.setOnClickListener(v -> showDatePicker());
        timePickerButton.setOnClickListener(v -> showTimePicker());

        // Update button texts with current date and time
        updateDateTimeButtons();

        // Set up save button
        saveButton.setOnClickListener(v -> {
            if (isEditMode) {
                updateExercise(type, subtype);
            } else {
                saveExercise(type, subtype);
            }
        });
    }
    
    private void populateFieldsFromExerciseData(JSONObject exerciseData, String type) {
        try {
            // Set start time
            long startTime = exerciseData.getLong("start_time");
            selectedDateTime.setTimeInMillis(startTime);
            updateDateTimeButtons();
            
            // Set duration
            if (exerciseData.has("duration")) {
                durationInput.setText(String.valueOf(exerciseData.getInt("duration")));
            }
            
            // Set notes if available
            if (exerciseData.has("notes") && !exerciseData.isNull("notes")) {
                notesInput.setText(exerciseData.getString("notes"));
            }
            
            // Set type-specific fields
            switch (type) {
                case "Strength":
                    if (exerciseData.has("weight")) {
                        weightInput.setText(String.valueOf(exerciseData.getDouble("weight")));
                    }
                    if (exerciseData.has("reps")) {
                        repsInput.setText(String.valueOf(exerciseData.getInt("reps")));
                    }
                    break;
                    
                case "Cardio":
                    if (exerciseData.has("distance") && !exerciseData.isNull("distance")) {
                        distanceInput.setText(String.valueOf(exerciseData.getDouble("distance")));
                    }
                    break;
                    
                case "Flexibility":
                case "Functional":
                    // No additional fields needed
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error populating fields from exercise data", e);
            Toast.makeText(this, "Error loading exercise data", Toast.LENGTH_SHORT).show();
        }
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

    private float getDistanceInMeters() {
        if (distanceInput.getText().toString().isEmpty()) {
            return 0;
        }
        return Float.parseFloat(distanceInput.getText().toString());
    }
    
    private void updateExercise(String type, String subtype) {
        // Validate required fields
        if (!validateCommonFields()) {
            return;
        }

        // Get common field values
        final long startTime = selectedDateTime.getTimeInMillis();
        final int duration = Integer.parseInt(durationInput.getText().toString());
        final String notes = notesInput.getText().toString();
        final String name = exerciseName.getText().toString();

        try {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "Please sign in to update exercises", Toast.LENGTH_SHORT).show();
                return;
            }

            // Prepare type-specific data
            final JSONObject exerciseData = new JSONObject();
            try {
                exerciseData.put("exercise_type", type.toLowerCase());
                exerciseData.put("exercise_name", name);
                exerciseData.put("start_time", startTime);
                exerciseData.put("duration", duration);
                exerciseData.put("notes", notes);
                exerciseData.put("exercise_subtype", subtype.toLowerCase());

                // Add type-specific fields
                switch (type) {
                    case "Strength":
                        if (!validateStrengthFields()) {
                            return;
                        }
                        float weight = Float.parseFloat(weightInput.getText().toString());
                        int reps = Integer.parseInt(repsInput.getText().toString());
                        exerciseData.put("weight", weight);
                        exerciseData.put("reps", reps);
                        break;

                    case "Cardio":
                        if (!distanceInput.getText().toString().isEmpty()) {
                            float distance = getDistanceInMeters();
                            exerciseData.put("distance", distance);
                        }
                        break;

                    case "Flexibility":
                    case "Functional":
                        // No additional fields needed
                        break;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
                return;
            } catch (Exception e) {
                Log.e(TAG, "Error creating exercise data", e);
                Toast.makeText(this, "Error preparing exercise data", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get the token and send the data
            currentUser.getIdToken(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String idToken = task.getResult().getToken();
                        exerciseRepository.updateExercise(exerciseId, exerciseData, idToken, new ExerciseService.SaveCallback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(RecordExerciseActivity.this, "Exercise updated successfully", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                            
                            @Override
                            public void onError(String error) {
                                Toast.makeText(RecordExerciseActivity.this, error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(this, "Authentication error", Toast.LENGTH_SHORT).show();
                    }
                });

        } catch (Exception e) {
            Toast.makeText(this, "Error updating exercise: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveExercise(String type, String subtype) {
        // Validate required fields
        if (!validateCommonFields()) {
            return;
        }

        // Get common field values
        final long startTime = selectedDateTime.getTimeInMillis();
        final int duration = Integer.parseInt(durationInput.getText().toString()); // Now storing in minutes
        final String notes = notesInput.getText().toString();
        final String name = exerciseName.getText().toString();

        try {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "Please sign in to save exercises", Toast.LENGTH_SHORT).show();
                return;
            }

            // Prepare type-specific data before the async call
            final JSONObject exerciseData = new JSONObject();
            try {
                exerciseData.put("exercise_type", type.toLowerCase());
                exerciseData.put("exercise_name", name);
                exerciseData.put("start_time", startTime);
                exerciseData.put("duration", duration); // Now in minutes
                exerciseData.put("notes", notes);
                exerciseData.put("exercise_subtype", subtype.toLowerCase());

                // Add type-specific fields
                switch (type) {
                    case "Strength":
                        if (!validateStrengthFields()) {
                            return;
                        }
                        float weight = Float.parseFloat(weightInput.getText().toString());
                        int reps = Integer.parseInt(repsInput.getText().toString());
                        exerciseData.put("weight", weight);
                        exerciseData.put("reps", reps);
                        break;

                    case "Cardio":
                        if (!distanceInput.getText().toString().isEmpty()) {
                            float distance = getDistanceInMeters(); // Now storing in meters directly
                            exerciseData.put("distance", distance);
                        }
                        break;

                    case "Flexibility":
                    case "Functional":
                        // No additional fields needed
                        break;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
                return;
            } catch (Exception e) {
                Log.e(TAG, "Error creating exercise data", e);
                Toast.makeText(this, "Error preparing exercise data", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get the token and send the data
            currentUser.getIdToken(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String idToken = task.getResult().getToken();
                        exerciseRepository.saveExercise(exerciseData, idToken, new ExerciseService.SaveCallback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(RecordExerciseActivity.this, "Exercise saved successfully", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                            
                            @Override
                            public void onError(String error) {
                                Toast.makeText(RecordExerciseActivity.this, error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(this, "Authentication error", Toast.LENGTH_SHORT).show();
                    }
                });

        } catch (Exception e) {
            Toast.makeText(this, "Error saving exercise: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
} 