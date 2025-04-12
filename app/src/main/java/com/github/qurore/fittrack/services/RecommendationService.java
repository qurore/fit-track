package com.github.qurore.fittrack.services;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RecommendationService {
    private static final String TAG = "RecommendationService";
    private static final String API_URL = "https://xg95njnqd7.execute-api.us-west-2.amazonaws.com/Prod/recommendation";
    
    private final RequestQueue requestQueue;
    private final FirebaseAuth mAuth;
    
    public interface RecommendationCallback {
        void onSuccess(JSONObject recommendedExercise);
        void onError(String error);
    }
    
    public RecommendationService(Context context) {
        requestQueue = Volley.newRequestQueue(context);
        mAuth = FirebaseAuth.getInstance();
    }
    
    public void getTodayRecommendation(RecommendationCallback callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("User not authenticated");
            return;
        }
        
        currentUser.getIdToken(true)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String idToken = task.getResult().getToken();
                    fetchRecommendation(idToken, callback);
                } else {
                    callback.onError("Failed to get authentication token");
                }
            });
    }
    
    private void fetchRecommendation(String idToken, RecommendationCallback callback) {
        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.GET,
            API_URL,
            null,
            response -> {
                try {
                    callback.onSuccess(response);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing recommendation data", e);
                    callback.onError("Error parsing recommendation data");
                }
            },
            error -> {
                Log.e(TAG, "Error fetching recommendation: " + error.toString());
                callback.onError("Error fetching recommendation");
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
} 