package com.github.qurore.fittrack;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    private View editProfileOption;
    private View aboutOption;
    private View termsOption;

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_settings_content, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        editProfileOption = view.findViewById(R.id.editProfileOption);
        aboutOption = view.findViewById(R.id.aboutOption);
        termsOption = view.findViewById(R.id.termsOption);
        
        // Set up click listeners
        setupClickListeners();
    }
    
    private void setupClickListeners() {
        // Account settings
        editProfileOption.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Edit Profile - Coming soon", Toast.LENGTH_SHORT).show();
        });
        
        // About section
        aboutOption.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AboutActivity.class);
            startActivity(intent);
        });
        
        termsOption.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), TermsPrivacyActivity.class);
            startActivity(intent);
        });
    }
} 