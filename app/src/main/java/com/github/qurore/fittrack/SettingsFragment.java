package com.github.qurore.fittrack;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

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

    private View editProfileOption;
    private View aboutOption;
    private View termsOption;
    private Button testGetButton;
    private Button testSetButton;
    private RequestQueue requestQueue;

    private static final String API_BASE_URL = "https://uxghxn9zf4.execute-api.us-west-2.amazonaws.com/Prod";

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
        
        // Initialize Volley RequestQueue
        requestQueue = Volley.newRequestQueue(requireContext());
        
        // Initialize views
        editProfileOption = view.findViewById(R.id.editProfileOption);
        aboutOption = view.findViewById(R.id.aboutOption);
        termsOption = view.findViewById(R.id.termsOption);
        testGetButton = view.findViewById(R.id.testGetButton);
        testSetButton = view.findViewById(R.id.testSetButton);
        
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

        // Test buttons
        testGetButton.setOnClickListener(v -> makeGetRequest());
        testSetButton.setOnClickListener(v -> makeSetRequest());
    }

    private void makeGetRequest() {
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
                    
                    JsonObjectRequest request = new JsonObjectRequest(
                        Request.Method.GET,
                        url,
                        null,
                        response -> {
                            try {
                                Toast.makeText(getContext(), "User data: " + response.toString(), Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                Toast.makeText(getContext(), "Error parsing response", Toast.LENGTH_SHORT).show();
                            }
                        },
                        error -> {
                            Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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
                } else {
                    Toast.makeText(getContext(), "Error getting authentication token", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void makeSetRequest() {
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
                    
                    // Sample user data
                    JSONObject userData = new JSONObject();
                    try {
                        userData.put("height", 175);
                        userData.put("weight", 70);
                        userData.put("birthDate", "1990-01-01");
                        userData.put("gender", "male");
                        userData.put("fitnessLevel", "intermediate");
                        userData.put("goals", new String[]{"weight_loss", "muscle_gain"});
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Error creating request data", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    JsonObjectRequest request = new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        userData,
                        response -> {
                            try {
                                String message = response.getString("message");
                                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                Toast.makeText(getContext(), "Error parsing response", Toast.LENGTH_SHORT).show();
                            }
                        },
                        error -> {
                            Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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
                } else {
                    Toast.makeText(getContext(), "Error getting authentication token", Toast.LENGTH_SHORT).show();
                }
            });
    }
} 