package com.github.qurore.fittrack;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HistoryTabContentFragment extends Fragment {
    private static final String ARG_TITLE = "title";

    private String title;
    private TextView titleTextView;
    private TextView descriptionTextView;
    private RadioGroup timeScaleRadioGroup;
    private RadioGroup graphTypeRadioGroup;

    public static HistoryTabContentFragment newInstance(String title) {
        HistoryTabContentFragment fragment = new HistoryTabContentFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history_tab_content, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        title = getArguments() != null ? getArguments().getString(ARG_TITLE) : "Tab";
        
        // Initialize views
        titleTextView = view.findViewById(R.id.historyTabContentTitle);
        descriptionTextView = view.findViewById(R.id.historyTabContentDescription);
        timeScaleRadioGroup = view.findViewById(R.id.timeScaleRadioGroup);
        graphTypeRadioGroup = view.findViewById(R.id.graphTypeRadioGroup);
        
        // Set title
        titleTextView.setText(title + " History");
        
        // Set initial description
        updateDescription();
        
        // Set up listeners for radio groups
        timeScaleRadioGroup.setOnCheckedChangeListener((group, checkedId) -> updateDescription());
        graphTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> updateDescription());
    }
    
    private void updateDescription() {
        // Get selected time scale
        int timeScaleId = timeScaleRadioGroup.getCheckedRadioButtonId();
        RadioButton timeScaleButton = getView().findViewById(timeScaleId);
        String timeScale = timeScaleButton != null ? timeScaleButton.getText().toString() : "Daily";
        
        // Get selected graph type
        int graphTypeId = graphTypeRadioGroup.getCheckedRadioButtonId();
        RadioButton graphTypeButton = getView().findViewById(graphTypeId);
        String graphType = graphTypeButton != null ? graphTypeButton.getText().toString() : "Time Graph";
        
        // Update description
        String description = String.format("%s %s for %s training will be displayed here", 
                timeScale, graphType, title.toLowerCase());
        descriptionTextView.setText(description);
    }
} 