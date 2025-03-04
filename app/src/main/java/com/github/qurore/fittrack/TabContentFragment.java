package com.github.qurore.fittrack;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class TabContentFragment extends Fragment {
    private static final String ARG_TITLE = "title";
    
    private Button[] trainingButtons = new Button[6];
    private TextView[] trainingLabels = new TextView[6];

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
        
        TextView titleTextView = view.findViewById(R.id.tabContentTitle);
        titleTextView.setText(title + " Training");
        
        // Initialize training buttons and labels
        initializeTrainingButtons(view);
        
        // Set up click listeners for training buttons
        setupButtonClickListeners();
    }
    
    private void initializeTrainingButtons(View view) {
        // Initialize buttons
        trainingButtons[0] = view.findViewById(R.id.trainingButton1);
        trainingButtons[1] = view.findViewById(R.id.trainingButton2);
        trainingButtons[2] = view.findViewById(R.id.trainingButton3);
        trainingButtons[3] = view.findViewById(R.id.trainingButton4);
        trainingButtons[4] = view.findViewById(R.id.trainingButton5);
        trainingButtons[5] = view.findViewById(R.id.trainingButton6);
        
        // Initialize labels
        trainingLabels[0] = view.findViewById(R.id.trainingLabel1);
        trainingLabels[1] = view.findViewById(R.id.trainingLabel2);
        trainingLabels[2] = view.findViewById(R.id.trainingLabel3);
        trainingLabels[3] = view.findViewById(R.id.trainingLabel4);
        trainingLabels[4] = view.findViewById(R.id.trainingLabel5);
        trainingLabels[5] = view.findViewById(R.id.trainingLabel6);
        
        // Set labels based on tab type
        String tabTitle = getArguments().getString(ARG_TITLE, "");
        if (tabTitle.equals("Strength")) {
            trainingLabels[0].setText("Chest");
            trainingLabels[1].setText("Back");
            trainingLabels[2].setText("Legs");
            trainingLabels[3].setText("Shoulders");
            trainingLabels[4].setText("Arms");
            trainingLabels[5].setText("Core");
        } else {
            // For other tabs, keep generic labels for now
            for (int i = 0; i < trainingLabels.length; i++) {
                trainingLabels[i].setText("Training " + (i + 1));
            }
        }
    }
    
    private void setupButtonClickListeners() {
        for (int i = 0; i < trainingButtons.length; i++) {
            final int buttonIndex = i;
            trainingButtons[i].setOnClickListener(v -> {
                String tabTitle = getArguments().getString(ARG_TITLE, "");
                String buttonLabel = trainingLabels[buttonIndex].getText().toString();
                Toast.makeText(getContext(), 
                        tabTitle + " - " + buttonLabel + " selected", 
                        Toast.LENGTH_SHORT).show();
            });
        }
    }
} 