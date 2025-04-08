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
            if (getActivity() instanceof DashboardActivity) {
                DashboardActivity activity = (DashboardActivity) getActivity();
                activity.showWorkoutContent();
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
    
    private List<HistoryListItem> convertToHistoryItems(List<JSONObject> exercises) {
        List<HistoryListItem> items = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault());
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getDefault());
        monthFormat.setTimeZone(TimeZone.getDefault());
        
        // Sort exercises by start time in descending order and group by month
        TreeMap<String, List<WorkoutHistoryItem>> monthlyExercises = new TreeMap<>((a, b) -> b.compareTo(a));
        
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
                
                // Format duration
                String formattedDuration = duration + " min";
                
                // Build additional info for cardio exercises
                if (type.equalsIgnoreCase("Cardio") && exercise.has("distance")) {
                    float distance = (float) exercise.getDouble("distance");
                    formattedDuration += " • " + distance + " m";
                }
                
                WorkoutHistoryItem item = new WorkoutHistoryItem(id, name, type, subtype, formattedDate + " • " + formattedDuration);
                
                // Add to monthly group
                monthlyExercises.computeIfAbsent(monthKey, k -> new ArrayList<>()).add(item);
            } catch (Exception e) {
                // Skip invalid entries
                continue;
            }
        }
        
        // Convert grouped data to flat list with headers
        for (String month : monthlyExercises.keySet()) {
            items.add(new MonthHeaderItem(month));
            items.addAll(monthlyExercises.get(month));
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
                            exerciseService.deleteExercise(workout.id, new ExerciseService.DeleteCallback() {
                                @Override
                                public void onSuccess() {
                                    removeItem(holder.getAdapterPosition());
                                    if (items.isEmpty()) {
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
            
            ExerciseViewHolder(View itemView) {
                super(itemView);
                exerciseTypeSubtype = itemView.findViewById(R.id.exerciseTypeSubtype);
                workoutName = itemView.findViewById(R.id.workoutName);
                workoutTime = itemView.findViewById(R.id.workoutTime);
                deleteButton = itemView.findViewById(R.id.deleteButton);
            }
        }
    }
} 