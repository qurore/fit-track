package com.github.qurore.fittrack.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.qurore.fittrack.R;
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

public class ExerciseListFragment extends Fragment {
    private static final String TAG = "ExerciseListFragment";
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

            // Process each category type (Strength, Cardio, etc.)
            for (int i = 0; i < categoriesArray.length(); i++) {
                JSONObject category = categoriesArray.getJSONObject(i);
                String type = category.getString("type");
                JSONArray subcategories = category.getJSONArray("subcategories");

                // Process subcategories for each type
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
            }

        } catch (IOException e) {
            Log.e(TAG, "Error reading exercises.json file", e);
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing exercises.json file", e);
        }
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