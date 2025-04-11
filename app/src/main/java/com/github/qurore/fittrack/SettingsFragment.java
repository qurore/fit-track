package com.github.qurore.fittrack;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import android.widget.EditText;
import android.util.Log;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

public class SettingsFragment extends Fragment {
    private static final String TAG = "SettingsFragment";

    // Define the interface
    public interface OnNameUpdatedListener {
        void onNameUpdated(String newName);
    }
    private OnNameUpdatedListener listener;

    private View editProfileOption;
    private View aboutOption;
    private View termsOption;
    private Button testGetButton;
    private Button testSetButton;
    private RequestQueue requestQueue;
    private String currentUserName;

    private static final String API_BASE_URL = "https://xg95njnqd7.execute-api.us-west-2.amazonaws.com/Prod";
    private static final String ARG_USERNAME = "username";

    public static SettingsFragment newInstance(String username) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USERNAME, username);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnNameUpdatedListener) {
            listener = (OnNameUpdatedListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnNameUpdatedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null; // Prevent memory leaks
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentUserName = getArguments().getString(ARG_USERNAME);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_settings_content, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize Volley RequestQueue
        requestQueue = Volley.newRequestQueue(requireContext());
        
        // Initialize views
        editProfileOption = view.findViewById(R.id.editProfileOption);
        aboutOption = view.findViewById(R.id.aboutOption);
        termsOption = view.findViewById(R.id.termsOption);
        
        // Make content visible
        view.setVisibility(View.VISIBLE);
        
        // Set up click listeners
        setupClickListeners();
    }
    
    private void setupClickListeners() {
        // Account settings
        editProfileOption.setOnClickListener(v -> showEditNameDialog());
        
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

    private void showEditNameDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_name, null);
        EditText nameEditText = dialogView.findViewById(R.id.nameEditText);
        
        // Use the name passed from DashboardActivity
        if (currentUserName != null) {
            nameEditText.setText(currentUserName);
        } else {
            // Fallback to Firebase user name if needed (shouldn't usually happen)
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                nameEditText.setText(currentUser.getDisplayName());
            }
        }

        new AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Save", (dialog, which) -> {
                String newName = nameEditText.getText().toString().trim();
                if (!newName.isEmpty()) {
                    updateUserName(newName);
                } else {
                    Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void updateUserName(String newName) {
        Log.d(TAG, "Updating user name to: " + newName);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        currentUser.getIdToken(true)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String idToken = task.getResult().getToken();
                    String url = API_BASE_URL + "/user";
                    
                    try {
                        JSONObject requestBody = new JSONObject();
                        requestBody.put("name", newName);
                        
                        JsonObjectRequest request = new JsonObjectRequest(
                            Request.Method.POST,
                            url,
                            requestBody,
                            response -> {
                                Toast.makeText(getContext(), "Name updated successfully", Toast.LENGTH_SHORT).show();
                                // Update the local username variable
                                currentUserName = newName;
                                // Notify the activity
                                if (listener != null) {
                                    listener.onNameUpdated(newName);
                                }
                            },
                            error -> {
                                Toast.makeText(getContext(), "Error updating name: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        ) {
                            @Override
                            public java.util.Map<String, String> getHeaders() {
                                java.util.Map<String, String> headers = new java.util.HashMap<>();
                                headers.put("Authorization", "Bearer " + idToken);
                                return headers;
                            }
                        };
                        
                        requestQueue.add(request);
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Error creating request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Error getting authentication token", Toast.LENGTH_SHORT).show();
                }
            });
    }
} 