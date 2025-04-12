package com.github.qurore.fittrack.repository;

import android.content.Context;
import android.util.Log;

import com.github.qurore.fittrack.services.RecommendationService;

import org.json.JSONObject;

public class RecommendationRepository {
    private static final String TAG = "RecommendationRepository";
    private static RecommendationRepository instance;
    
    private final RecommendationService recommendationService;
    
    private RecommendationRepository(Context context) {
        recommendationService = new RecommendationService(context.getApplicationContext());
    }
    
    /**
     * Get the singleton instance of the repository
     */
    public static synchronized RecommendationRepository getInstance(Context context) {
        if (instance == null) {
            instance = new RecommendationRepository(context);
        }
        return instance;
    }
    
    /**
     * Get today's exercise recommendation
     */
    public void getTodayRecommendation(RecommendationCallback callback) {
        recommendationService.getTodayRecommendation(new RecommendationService.RecommendationCallback() {
            @Override
            public void onSuccess(JSONObject recommendedExercise) {
                callback.onSuccess(recommendedExercise);
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error fetching recommendation: " + error);
                callback.onError(error);
            }
        });
    }
    
    /**
     * Callback interface for recommendation operations
     */
    public interface RecommendationCallback {
        void onSuccess(JSONObject recommendedExercise);
        void onError(String error);
    }
} 