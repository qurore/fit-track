package com.github.qurore.fittrack;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.qurore.fittrack.adapters.ExerciseListAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TabContentFragment extends Fragment {
    private static final String TAG = "TabContentFragment";
    private static final String ARG_TITLE = "title";
    
    private ExpandableListView exerciseListView;
    private ExerciseListAdapter adapter;
    private List<String> categories;
    private Map<String, List<String>> exercises;

    public static TabContentFragment newInstance(String title) {
        TabContentFragment fragment = new TabContentFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tab_content, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        String title = getArguments() != null ? getArguments().getString(ARG_TITLE) : "Tab";
        
        TextView titleTextView = view.findViewById(R.id.selectExerciseTitle);
        titleTextView.setText("Select Exercise");
        
        TextView subtitleTextView = view.findViewById(R.id.selectExerciseSubtitle);
        subtitleTextView.setText("Choose a category, then select an exercise");
        
        exerciseListView = view.findViewById(R.id.exerciseListView);
        
        initializeDataFromJson(title);
        setupListView();
    }
    
    private void initializeDataFromJson(String tabTitle) {
        categories = new ArrayList<>();
        exercises = new HashMap<>();

        try {
            // Read JSON file from raw resources
            InputStream inputStream = requireContext().getResources().openRawResource(R.raw.exercises);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            
            JSONObject jsonObject = new JSONObject(stringBuilder.toString());
            JSONArray categoriesArray = jsonObject.getJSONArray("categories");

            // Find the matching category type for the current tab
            for (int i = 0; i < categoriesArray.length(); i++) {
                JSONObject category = categoriesArray.getJSONObject(i);
                String type = category.getString("type");
                
                // Only process the category that matches the current tab
                if (type.equals(tabTitle)) {
                    JSONArray subcategories = category.getJSONArray("subcategories");

                    // Process subcategories
                    for (int j = 0; j < subcategories.length(); j++) {
                        JSONObject subcategory = subcategories.getJSONObject(j);
                        String subcategoryName = subcategory.getString("name");
                        JSONArray exercisesArray = subcategory.getJSONArray("exercises");

                        // Add subcategory to categories list
                        categories.add(subcategoryName);

                        // Create list for exercises in this subcategory
                        List<String> exerciseList = new ArrayList<>();
                        for (int k = 0; k < exercisesArray.length(); k++) {
                            JSONObject exercise = exercisesArray.getJSONObject(k);
                            exerciseList.add(exercise.getString("name"));
                        }
                        exercises.put(subcategoryName, exerciseList);
                    }
                    break; // Exit loop once we've found and processed the matching category
                }
            }

            // If no exercises were loaded (category not found), use generic data
            if (exercises.isEmpty()) {
                Log.w(TAG, "No exercises found for category: " + tabTitle + ". Using generic data.");
                initializeGenericData(tabTitle);
            }

        } catch (IOException e) {
            Log.e(TAG, "Error reading exercises.json file", e);
            initializeGenericData(tabTitle);
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing exercises.json file", e);
            initializeGenericData(tabTitle);
        }
    }
    
    private void initializeGenericData(String title) {
        categories = new ArrayList<>();
        exercises = new HashMap<>();
        
        // Add generic categories for other tab types
        for (int i = 1; i <= 6; i++) {
            String category = "Training " + i;
            categories.add(category);
            
            // Add generic exercises
            List<String> genericExercises = new ArrayList<>();
            for (int j = 1; j <= 3; j++) {
                genericExercises.add(title + " Exercise " + j);
            }
            exercises.put(category, genericExercises);
        }
    }
    
    private void setupListView() {
        adapter = new ExerciseListAdapter(requireContext(), categories, exercises);
        exerciseListView.setAdapter(adapter);

        // Handle exercise selection
        exerciseListView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            String category = categories.get(groupPosition);
            String exercise = exercises.get(category).get(childPosition);
            
            // Launch RecordExerciseActivity
            Intent intent = new Intent(getContext(), RecordExerciseActivity.class);
            intent.putExtra(RecordExerciseActivity.EXTRA_EXERCISE_NAME, exercise);
            intent.putExtra(RecordExerciseActivity.EXTRA_EXERCISE_TYPE, getArguments().getString(ARG_TITLE));
            intent.putExtra(RecordExerciseActivity.EXTRA_EXERCISE_SUBTYPE, category);
            startActivity(intent);
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