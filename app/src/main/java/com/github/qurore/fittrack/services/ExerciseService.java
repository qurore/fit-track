package com.github.qurore.fittrack.services;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.AuthFailureError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExerciseService {
    private static final String TAG = "ExerciseService";
    private static final String API_URL = "https://xg95njnqd7.execute-api.us-west-2.amazonaws.com/Prod/exercises";
    
    private final RequestQueue requestQueue;
    private final FirebaseAuth mAuth;
    
    public interface ExerciseCallback {
        void onSuccess(List<JSONObject> exercises);
        void onError(String error);
    }

    public interface DeleteCallback {
        void onSuccess();
        void onError(String error);
    }
    
    public ExerciseService(Context context) {
        requestQueue = Volley.newRequestQueue(context);
        mAuth = FirebaseAuth.getInstance();
    }
    
    public void getAllExercises(ExerciseCallback callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("User not authenticated");
            return;
        }
        
        currentUser.getIdToken(true)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String idToken = task.getResult().getToken();
                    fetchExercises(idToken, callback);
                } else {
                    callback.onError("Failed to get authentication token");
                }
            });
    }
    
    private void fetchExercises(String idToken, ExerciseCallback callback) {
        JsonArrayRequest request = new JsonArrayRequest(
            Request.Method.GET,
            API_URL,
            null,
            response -> {
                try {
                    List<JSONObject> exercises = new ArrayList<>();
                    for (int i = 0; i < response.length(); i++) {
                        exercises.add(response.getJSONObject(i));
                    }
                    callback.onSuccess(exercises);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing exercise data", e);
                    callback.onError("Error parsing exercise data");
                }
            },
            error -> {
                Log.e(TAG, "Error fetching exercises: " + error.toString());
                callback.onError("Error fetching exercises");
            }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + idToken);
                return headers;
            }
        };
        
        requestQueue.add(request);
    }

    public void deleteExercise(String exerciseId, DeleteCallback callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("User not authenticated");
            return;
        }

        currentUser.getIdToken(true)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String idToken = task.getResult().getToken();
                    try {
                        // Extract the ID from the MongoDB ObjectId format if necessary
                        String id = exerciseId;
                        if (exerciseId.contains("$oid")) {
                            // Parse the MongoDB ObjectId format
                            JSONObject jsonId = new JSONObject(exerciseId);
                            id = jsonId.getString("$oid");
                        }
                        
                        String url = API_URL + "/" + id;

                        JsonObjectRequest request = new JsonObjectRequest(
                            Request.Method.DELETE,
                            url,
                            null,
                            response -> callback.onSuccess(),
                            error -> {
                                String errorMessage = "Failed to delete exercise";
                                if (error.networkResponse != null) {
                                    errorMessage += " (Status: " + error.networkResponse.statusCode + ")";
                                }
                                callback.onError(errorMessage);
                            }
                        ) {
                            @Override
                            public Map<String, String> getHeaders() throws AuthFailureError {
                                Map<String, String> headers = new HashMap<>();
                                headers.put("Authorization", "Bearer " + idToken);
                                return headers;
                            }
                        };

                        requestQueue.add(request);
                    } catch (Exception e) {
                        callback.onError("Error processing exercise ID: " + e.getMessage());
                    }
                } else {
                    callback.onError("Failed to get authentication token");
                }
            });
    }
} 