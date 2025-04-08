package com.github.qurore.fittrack;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.qurore.fittrack.services.ExerciseService;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class WorkoutHistoryFragment extends Fragment {
    private static final String TAG = "WorkoutHistoryFragment";
    
    private RecyclerView workoutHistoryList;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ExerciseService exerciseService;
    private WorkoutHistoryAdapter adapter;
    private TextView noExercisesText;
    private TextView addExerciseLink;

    public static WorkoutHistoryFragment newInstance() {
        return new WorkoutHistoryFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_workout_history_content, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        exerciseService = new ExerciseService(requireContext());
        
        // Initialize views
        workoutHistoryList = view.findViewById(R.id.workoutHistoryList);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        noExercisesText = view.findViewById(R.id.noExercisesText);
        addExerciseLink = view.findViewById(R.id.addExerciseLink);
        
        // Set up RecyclerView
        workoutHistoryList.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Initialize SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::loadExercises);
        
        // Initialize adapter with empty list
        adapter = new WorkoutHistoryAdapter(new ArrayList<>());
        workoutHistoryList.setAdapter(adapter);
        
        // Set up add exercise link
        addExerciseLink.setOnClickListener(v -> {
            // Navigate to workout selection screen
            if (getActivity() instanceof DashboardActivity) {
                DashboardActivity activity = (DashboardActivity) getActivity();
                activity.showWorkoutContent();
                // Update bottom navigation to Workout tab
                activity.findViewById(R.id.bottomNavigation).findViewById(R.id.navigation_workout).performClick();
            }
        });
        
        // Load exercises
        loadExercises();
    }
    
    private void loadExercises() {
        swipeRefreshLayout.setRefreshing(true);
        
        exerciseService.getAllExercises(new ExerciseService.ExerciseCallback() {
            @Override
            public void onSuccess(List<JSONObject> exercises) {
                if (isAdded()) {  // Check if fragment is still attached to activity
                    swipeRefreshLayout.setRefreshing(false);
                    List<WorkoutHistoryItem> workoutItems = convertToWorkoutItems(exercises);
                    adapter.updateData(workoutItems);
                    
                    // Update visibility based on data
                    if (workoutItems.isEmpty()) {
                        noExercisesText.setVisibility(View.VISIBLE);
                        workoutHistoryList.setVisibility(View.GONE);
                    } else {
                        noExercisesText.setVisibility(View.GONE);
                        workoutHistoryList.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {  // Check if fragment is still attached to activity
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                    
                    // Show no exercises text on error
                    noExercisesText.setVisibility(View.VISIBLE);
                    workoutHistoryList.setVisibility(View.GONE);
                }
            }
        });
    }
    
    private List<WorkoutHistoryItem> convertToWorkoutItems(List<JSONObject> exercises) {
        List<WorkoutHistoryItem> items = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getDefault());
        
        for (JSONObject exercise : exercises) {
            try {
                String id = exercise.getString("_id");
                String name = capitalizeWords(exercise.getString("exercise_name"));
                String type = capitalizeWords(exercise.getString("exercise_type"));
                String subtype = capitalizeWords(exercise.getString("exercise_subtype"));
                long startTime = exercise.getLong("start_time");
                int duration = exercise.getInt("duration"); // Now in minutes
                
                // Format the date
                String formattedDate = dateFormat.format(new Date(startTime));
                
                // Format duration (now directly in minutes)
                String formattedDuration = duration + " min";
                
                // Build additional info for cardio exercises
                if (type.equalsIgnoreCase("Cardio") && exercise.has("distance")) {
                    float distance = (float) exercise.getDouble("distance"); // Already in meters
                    formattedDuration += " • " + distance + " m";
                }
                
                items.add(new WorkoutHistoryItem(id, name, type, subtype, formattedDate + " • " + formattedDuration));
            } catch (Exception e) {
                // Skip invalid entries
                continue;
            }
        }
        
        return items;
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
    
    // Data class for workout history items
    private static class WorkoutHistoryItem {
        String id;
        String name;
        String type;
        String subtype;
        String time;
        
        WorkoutHistoryItem(String id, String name, String type, String subtype, String time) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.subtype = subtype;
            this.time = time;
        }
    }
    
    // Adapter for workout history items
    private class WorkoutHistoryAdapter extends RecyclerView.Adapter<WorkoutHistoryAdapter.ViewHolder> {
        private List<WorkoutHistoryItem> workouts;
        
        WorkoutHistoryAdapter(List<WorkoutHistoryItem> workouts) {
            this.workouts = workouts;
        }
        
        void updateData(List<WorkoutHistoryItem> newWorkouts) {
            this.workouts = newWorkouts;
            notifyDataSetChanged();
        }

        void removeItem(int position) {
            workouts.remove(position);
            notifyItemRemoved(position);
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_workout_history, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            WorkoutHistoryItem workout = workouts.get(position);
            holder.exerciseTypeSubtype.setText(String.format("%s - %s", workout.type, workout.subtype));
            holder.workoutName.setText(workout.name);
            holder.workoutTime.setText(workout.time);
            
            holder.deleteButton.setOnClickListener(v -> {
                new AlertDialog.Builder(requireContext())
                    .setTitle("Delete Exercise")
                    .setMessage("Are you sure you want to delete this exercise?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        exerciseService.deleteExercise(workout.id, new ExerciseService.DeleteCallback() {
                            @Override
                            public void onSuccess() {
                                removeItem(holder.getAdapterPosition());
                                if (workouts.isEmpty()) {
                                    noExercisesText.setVisibility(View.VISIBLE);
                                    workoutHistoryList.setVisibility(View.GONE);
                                }
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
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
            ImageButton deleteButton;
            
            ViewHolder(View itemView) {
                super(itemView);
                exerciseTypeSubtype = itemView.findViewById(R.id.exerciseTypeSubtype);
                workoutName = itemView.findViewById(R.id.workoutName);
                workoutTime = itemView.findViewById(R.id.workoutTime);
                deleteButton = itemView.findViewById(R.id.deleteButton);
            }
        }
    }
} 