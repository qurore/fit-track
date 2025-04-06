package com.github.qurore.fittrack.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.qurore.fittrack.R;
import com.github.qurore.fittrack.adapters.ExerciseListAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExerciseListFragment extends Fragment {
    private ExpandableListView exerciseListView;
    private ExerciseListAdapter adapter;
    private List<String> categories;
    private Map<String, List<String>> exercises;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab_content, container, false);
        exerciseListView = view.findViewById(R.id.exerciseListView);
        
        initializeData();
        setupListView();
        
        return view;
    }

    private void initializeData() {
        categories = new ArrayList<>();
        exercises = new HashMap<>();

        // Add categories
        categories.add("Chest");
        categories.add("Back");
        categories.add("Legs");
        categories.add("Shoulders");
        categories.add("Arms");
        categories.add("Core");

        // Add exercises for each category
        List<String> chestExercises = new ArrayList<>();
        chestExercises.add("Bench Press");
        chestExercises.add("Incline Press");
        chestExercises.add("Decline Press");
        chestExercises.add("Chest Fly");
        chestExercises.add("Push-Up");
        chestExercises.add("Cable Crossover");
        exercises.put("Chest", chestExercises);

        List<String> backExercises = new ArrayList<>();
        backExercises.add("Pull-Up");
        backExercises.add("Lat Pulldown");
        backExercises.add("Barbell Row");
        backExercises.add("Deadlift");
        backExercises.add("Face Pull");
        exercises.put("Back", backExercises);

        List<String> legExercises = new ArrayList<>();
        legExercises.add("Squat");
        legExercises.add("Leg Press");
        legExercises.add("Lunges");
        legExercises.add("Leg Extension");
        legExercises.add("Leg Curl");
        legExercises.add("Calf Raise");
        exercises.put("Legs", legExercises);

        List<String> shoulderExercises = new ArrayList<>();
        shoulderExercises.add("Overhead Press");
        shoulderExercises.add("Lateral Raise");
        shoulderExercises.add("Front Raise");
        shoulderExercises.add("Reverse Fly");
        shoulderExercises.add("Shrugs");
        exercises.put("Shoulders", shoulderExercises);

        List<String> armExercises = new ArrayList<>();
        armExercises.add("Bicep Curl");
        armExercises.add("Tricep Extension");
        armExercises.add("Hammer Curl");
        armExercises.add("Skull Crusher");
        armExercises.add("Preacher Curl");
        exercises.put("Arms", armExercises);

        List<String> coreExercises = new ArrayList<>();
        coreExercises.add("Plank");
        coreExercises.add("Crunch");
        coreExercises.add("Russian Twist");
        coreExercises.add("Leg Raise");
        coreExercises.add("Side Plank");
        exercises.put("Core", coreExercises);
    }

    private void setupListView() {
        adapter = new ExerciseListAdapter(requireContext(), categories, exercises);
        exerciseListView.setAdapter(adapter);

        // Handle exercise selection
        exerciseListView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            String category = categories.get(groupPosition);
            String exercise = exercises.get(category).get(childPosition);
            // TODO: Handle exercise selection
            return true;
        });

        // Handle category expansion
        exerciseListView.setOnGroupExpandListener(groupPosition -> {
            // Close other expanded groups
            for (int i = 0; i < categories.size(); i++) {
                if (i != groupPosition && exerciseListView.isGroupExpanded(i)) {
                    exerciseListView.collapseGroup(i);
                }
            }
        });
    }
} 