package com.github.qurore.fittrack;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    private View editProfileOption;
    private View changePasswordOption;
    private View notificationsOption;
    private View darkModeOption;
    private View aboutOption;
    private View termsOption;
    private SwitchCompat notificationsSwitch;
    private SwitchCompat darkModeSwitch;

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
        changePasswordOption = view.findViewById(R.id.changePasswordOption);
        notificationsOption = view.findViewById(R.id.notificationsOption);
        darkModeOption = view.findViewById(R.id.darkModeOption);
        aboutOption = view.findViewById(R.id.aboutOption);
        termsOption = view.findViewById(R.id.termsOption);
        notificationsSwitch = view.findViewById(R.id.notificationsSwitch);
        darkModeSwitch = view.findViewById(R.id.darkModeSwitch);
        
        // Set up click listeners
        setupClickListeners();
    }
    
    private void setupClickListeners() {
        // Account settings
        editProfileOption.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Edit Profile - Coming soon", Toast.LENGTH_SHORT).show();
        });
        
        changePasswordOption.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Change Password - Coming soon", Toast.LENGTH_SHORT).show();
        });
        
        // Application settings
        notificationsOption.setOnClickListener(v -> {
            notificationsSwitch.setChecked(!notificationsSwitch.isChecked());
            updateNotificationSettings(notificationsSwitch.isChecked());
        });
        
        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateNotificationSettings(isChecked);
        });
        
        darkModeOption.setOnClickListener(v -> {
            darkModeSwitch.setChecked(!darkModeSwitch.isChecked());
            updateDarkModeSettings(darkModeSwitch.isChecked());
        });
        
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateDarkModeSettings(isChecked);
        });
        
        // About section
        aboutOption.setOnClickListener(v -> {
            Toast.makeText(getContext(), "About FitTrack - Coming soon", Toast.LENGTH_SHORT).show();
        });
        
        termsOption.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Terms and Privacy Policy - Coming soon", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void updateNotificationSettings(boolean enabled) {
        // In a real app, this would save the setting to SharedPreferences
        // and update the notification settings
        String message = enabled ? "Notifications enabled" : "Notifications disabled";
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
    
    private void updateDarkModeSettings(boolean enabled) {
        // In a real app, this would save the setting to SharedPreferences
        // and update the app theme
        String message = enabled ? "Dark mode enabled" : "Dark mode disabled";
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        
        // Note: Actually changing the theme would require restarting the activity
        // or using a theme overlay, which is beyond the scope of this implementation
    }
} 