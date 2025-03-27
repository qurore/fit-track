package com.github.qurore.fittrack;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

public class WorkoutHistoryFragment extends Fragment {

    private RecyclerView workoutHistoryList;

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
        
        // Initialize RecyclerView
        workoutHistoryList = view.findViewById(R.id.workoutHistoryList);
        workoutHistoryList.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Set up with sample data (in a real app, this would come from a database)
        setupWorkoutHistoryList();
    }
    
    private void setupWorkoutHistoryList() {
        // Sample data - in a real app, this would load from a database
        List<WorkoutHistoryItem> workouts = Arrays.asList(
            new WorkoutHistoryItem("Upper Body", "Today • 45 min"),
            new WorkoutHistoryItem("Cardio", "Yesterday • 30 min"),
            new WorkoutHistoryItem("Leg Day", "2 days ago • 60 min"),
            new WorkoutHistoryItem("Core Workout", "3 days ago • 25 min"),
            new WorkoutHistoryItem("Full Body", "5 days ago • 50 min"),
            new WorkoutHistoryItem("Upper Body", "1 week ago • 45 min"),
            new WorkoutHistoryItem("Cardio", "1 week ago • 30 min"),
            new WorkoutHistoryItem("Leg Day", "2 weeks ago • 60 min"),
            new WorkoutHistoryItem("Core Workout", "2 weeks ago • 25 min"),
            new WorkoutHistoryItem("Full Body", "3 weeks ago • 50 min")
        );
        
        WorkoutHistoryAdapter adapter = new WorkoutHistoryAdapter(workouts);
        workoutHistoryList.setAdapter(adapter);
    }
    
    // Data class for workout history items
    private static class WorkoutHistoryItem {
        String name;
        String time;
        
        WorkoutHistoryItem(String name, String time) {
            this.name = name;
            this.time = time;
        }
    }
    
    // Adapter for workout history items
    private class WorkoutHistoryAdapter extends RecyclerView.Adapter<WorkoutHistoryAdapter.ViewHolder> {
        private List<WorkoutHistoryItem> workouts;
        
        WorkoutHistoryAdapter(List<WorkoutHistoryItem> workouts) {
            this.workouts = workouts;
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_recent_workout, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            WorkoutHistoryItem workout = workouts.get(position);
            
            holder.workoutName.setText(workout.name);
            holder.workoutTime.setText(workout.time);
            
            holder.detailsButton.setOnClickListener(v -> {
                // Handle details button click - navigate to workout details
                // For now, just showing a placeholder implementation
                if (getActivity() != null) {
                    // In a real app, you would navigate to a workout details activity/fragment
                }
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
} 