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
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.qurore.fittrack.repository.ExerciseRepository;
import com.github.qurore.fittrack.services.ExerciseService;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.TreeMap;

public class WorkoutHistoryFragment extends Fragment {
    private static final String TAG = "WorkoutHistoryFragment";
    private static final int VIEW_TYPE_MONTH_HEADER = 0;
    private static final int VIEW_TYPE_EXERCISE = 1;
    
    private RecyclerView workoutHistoryList;
    private ExerciseRepository exerciseRepository;
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
        
        exerciseRepository = ExerciseRepository.getInstance(requireContext());
        
        // Initialize views
        workoutHistoryList = view.findViewById(R.id.workoutHistoryList);
        noExercisesText = view.findViewById(R.id.noExercisesText);
        addExerciseLink = view.findViewById(R.id.addExerciseLink);
        
        // Set up RecyclerView
        workoutHistoryList.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Initialize adapter with empty list
        adapter = new WorkoutHistoryAdapter(new ArrayList<>());
        workoutHistoryList.setAdapter(adapter);
        
        // Set up add exercise link
        addExerciseLink.setOnClickListener(v -> {
            if (getActivity() instanceof DashboardActivity) {
                DashboardActivity activity = (DashboardActivity) getActivity();
                activity.showWorkoutContent();
                activity.findViewById(R.id.bottomNavigation).findViewById(R.id.navigation_workout).performClick();
            }
        });
        
        // Set up observers
        setupObservers();
        
        // Load exercises
        loadExercises();
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    private void setupObservers() {
        // Observe exercise data changes
        exerciseRepository.getExercises().observe(getViewLifecycleOwner(), exercises -> {
            if (isAdded()) {  // Check if fragment is still attached to activity
                List<HistoryListItem> historyItems = convertToHistoryItems(exercises);
                adapter.updateData(historyItems);
                
                // Update visibility based on data
                if (historyItems.isEmpty()) {
                    noExercisesText.setVisibility(View.VISIBLE);
                    workoutHistoryList.setVisibility(View.GONE);
                } else {
                    noExercisesText.setVisibility(View.GONE);
                    workoutHistoryList.setVisibility(View.VISIBLE);
                }
            }
        });
        
        // Observe error messages
        exerciseRepository.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (isAdded() && errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                
                // Show no exercises text on error if no data available
                if (adapter.getItemCount() == 0) {
                    noExercisesText.setVisibility(View.VISIBLE);
                    workoutHistoryList.setVisibility(View.GONE);
                }
            }
        });
    }
    
    private void loadExercises() {
        // Just trigger a refresh - actual data loading is handled by the repository
        exerciseRepository.refreshExercises();
    }
    
    private List<HistoryListItem> convertToHistoryItems(List<JSONObject> exercises) {
        List<HistoryListItem> items = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault());
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getDefault());
        monthFormat.setTimeZone(TimeZone.getDefault());
        
        // Sort exercises by start time in descending order and group by month
        TreeMap<Long, String> monthKeySorting = new TreeMap<>((a, b) -> b.compareTo(a));
        TreeMap<String, List<WorkoutHistoryItem>> monthlyExercises = new TreeMap<>();
        
        for (JSONObject exercise : exercises) {
            try {
                String id = exercise.getString("_id");
                String name = capitalizeWords(exercise.getString("exercise_name"));
                String type = capitalizeWords(exercise.getString("exercise_type"));
                String subtype = capitalizeWords(exercise.getString("exercise_subtype"));
                long startTime = exercise.getLong("start_time");
                int duration = exercise.getInt("duration");
                
                // Format the date
                String formattedDate = dateFormat.format(new Date(startTime));
                String monthKey = monthFormat.format(new Date(startTime));
                
                // Store the month's timestamp for sorting
                Calendar cal = Calendar.getInstance();
                cal.setTime(new Date(startTime));
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                monthKeySorting.put(cal.getTimeInMillis(), monthKey);
                
                // Format duration
                String formattedDuration = duration + " min";
                
                WorkoutHistoryItem item = new WorkoutHistoryItem(id, name, type, subtype, formattedDate + " • " + formattedDuration);
                item.startTime = startTime; // Add startTime for sorting
                item.completeData = exercise; // Store the complete exercise data
                
                // Add to monthly group
                monthlyExercises.computeIfAbsent(monthKey, k -> new ArrayList<>()).add(item);
            } catch (Exception e) {
                // Skip invalid entries
                continue;
            }
        }
        
        // Convert grouped data to flat list with headers in descending order
        for (Long monthTimestamp : monthKeySorting.keySet()) {
            String monthKey = monthKeySorting.get(monthTimestamp);
            items.add(new MonthHeaderItem(monthKey));
            
            // Sort exercises within the month by start time in descending order
            List<WorkoutHistoryItem> monthExercises = monthlyExercises.get(monthKey);
            monthExercises.sort((a, b) -> Long.compare(b.startTime, a.startTime));
            items.addAll(monthExercises);
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
    
    // Base class for list items
    private static abstract class HistoryListItem {
        abstract int getType();
    }
    
    // Month header item
    private static class MonthHeaderItem extends HistoryListItem {
        String month;
        
        MonthHeaderItem(String month) {
            this.month = month;
        }
        
        @Override
        int getType() {
            return VIEW_TYPE_MONTH_HEADER;
        }
    }
    
    // Exercise item
    private static class WorkoutHistoryItem extends HistoryListItem {
        String id;
        String name;
        String type;
        String subtype;
        String time;
        long startTime; // Add startTime field for sorting
        JSONObject completeData; // Store complete exercise data
        
        WorkoutHistoryItem(String id, String name, String type, String subtype, String time) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.subtype = subtype;
            this.time = time;
        }
        
        @Override
        int getType() {
            return VIEW_TYPE_EXERCISE;
        }
    }
    
    // Adapter for workout history items
    private class WorkoutHistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private List<HistoryListItem> items;
        
        WorkoutHistoryAdapter(List<HistoryListItem> items) {
            this.items = items;
        }
        
        void updateData(List<HistoryListItem> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
        }

        void removeItem(int position) {
            items.remove(position);
            notifyItemRemoved(position);
            
            // If the next item is a header and it's the last item or the next item is also a header,
            // remove the header too
            if (position < items.size() && items.get(position).getType() == VIEW_TYPE_MONTH_HEADER) {
                if (position == items.size() - 1 || 
                    (position + 1 < items.size() && items.get(position + 1).getType() == VIEW_TYPE_MONTH_HEADER)) {
                    items.remove(position);
                    notifyItemRemoved(position);
                }
            }
        }
        
        @Override
        public int getItemViewType(int position) {
            return items.get(position).getType();
        }
        
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == VIEW_TYPE_MONTH_HEADER) {
                View view = inflater.inflate(R.layout.item_month_header, parent, false);
                return new MonthHeaderViewHolder(view);
            } else {
                View view = inflater.inflate(R.layout.item_workout_history, parent, false);
                return new ExerciseViewHolder(view);
            }
        }
        
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof MonthHeaderViewHolder) {
                MonthHeaderItem header = (MonthHeaderItem) items.get(position);
                ((MonthHeaderViewHolder) holder).monthText.setText(header.month);
            } else if (holder instanceof ExerciseViewHolder) {
                WorkoutHistoryItem workout = (WorkoutHistoryItem) items.get(position);
                ExerciseViewHolder exerciseHolder = (ExerciseViewHolder) holder;
                exerciseHolder.exerciseTypeSubtype.setText(String.format("%s - %s", workout.type, workout.subtype));
                exerciseHolder.workoutName.setText(workout.name);
                exerciseHolder.workoutTime.setText(workout.time);
                
                exerciseHolder.deleteButton.setOnClickListener(v -> {
                    new AlertDialog.Builder(requireContext())
                        .setTitle("Delete Exercise")
                        .setMessage("Are you sure you want to delete this exercise?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            exerciseRepository.deleteExercise(workout.id, new ExerciseService.DeleteCallback() {
                                @Override
                                public void onSuccess() {
                                    // No need to manually update the UI
                                    // The repository will refresh data and the observer will update the UI
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
                
                exerciseHolder.editButton.setOnClickListener(v -> {
                    // Open RecordExerciseActivity in edit mode with the exercise data
                    try {
                        // Create intent to launch RecordExerciseActivity
                        Intent intent = new Intent(requireContext(), RecordExerciseActivity.class);
                        
                        // Set edit mode and exercise ID
                        intent.putExtra(RecordExerciseActivity.EXTRA_EDIT_MODE, true);
                        intent.putExtra(RecordExerciseActivity.EXTRA_EXERCISE_ID, workout.id);
                        
                        // Put exercise data
                        intent.putExtra(RecordExerciseActivity.EXTRA_EXERCISE_NAME, workout.name);
                        intent.putExtra(RecordExerciseActivity.EXTRA_EXERCISE_TYPE, workout.type);
                        intent.putExtra(RecordExerciseActivity.EXTRA_EXERCISE_SUBTYPE, workout.subtype);
                        
                        // Include full exercise details as a JSON string 
                        intent.putExtra(RecordExerciseActivity.EXTRA_EXERCISE_DATA, workout.completeData.toString());
                        
                        // Start activity
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "Error opening exercise: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
        
        @Override
        public int getItemCount() {
            return items.size();
        }
        
        class MonthHeaderViewHolder extends RecyclerView.ViewHolder {
            TextView monthText;
            
            MonthHeaderViewHolder(View itemView) {
                super(itemView);
                monthText = itemView.findViewById(R.id.monthText);
            }
        }
        
        class ExerciseViewHolder extends RecyclerView.ViewHolder {
            TextView exerciseTypeSubtype;
            TextView workoutName;
            TextView workoutTime;
            ImageButton deleteButton;
            ImageButton editButton;
            
            ExerciseViewHolder(View itemView) {
                super(itemView);
                exerciseTypeSubtype = itemView.findViewById(R.id.exerciseTypeSubtype);
                workoutName = itemView.findViewById(R.id.workoutName);
                workoutTime = itemView.findViewById(R.id.workoutTime);
                deleteButton = itemView.findViewById(R.id.deleteButton);
                editButton = itemView.findViewById(R.id.editButton);
            }
        }
    }
} 