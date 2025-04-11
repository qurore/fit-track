package com.github.qurore.fittrack.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.github.qurore.fittrack.services.ExerciseService;

import org.json.JSONObject;

import java.util.List;

/**
 * Repository for exercise data that provides a clean API to the rest of the app
 * and centralizes data management.
 */
public class ExerciseRepository {
    private static final String TAG = "ExerciseRepository";
    private static ExerciseRepository instance;
    
    private final ExerciseService exerciseService;
    private final MutableLiveData<List<JSONObject>> exercisesLiveData;
    private final MutableLiveData<Boolean> isLoadingLiveData;
    private final MutableLiveData<String> errorMessageLiveData;
    
    private ExerciseRepository(Context context) {
        exerciseService = new ExerciseService(context.getApplicationContext());
        exercisesLiveData = new MutableLiveData<>();
        isLoadingLiveData = new MutableLiveData<>(false);
        errorMessageLiveData = new MutableLiveData<>();
    }
    
    /**
     * Get the singleton instance of the repository
     */
    public static synchronized ExerciseRepository getInstance(Context context) {
        if (instance == null) {
            instance = new ExerciseRepository(context);
        }
        return instance;
    }
    
    /**
     * Refresh exercise data from the backend
     */
    public void refreshExercises() {
        isLoadingLiveData.setValue(true);
        
        exerciseService.getAllExercises(new ExerciseService.ExerciseCallback() {
            @Override
            public void onSuccess(List<JSONObject> exercises) {
                exercisesLiveData.setValue(exercises);
                isLoadingLiveData.setValue(false);
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error fetching exercises: " + error);
                errorMessageLiveData.setValue(error);
                isLoadingLiveData.setValue(false);
            }
        });
    }
    
    /**
     * Save a new exercise and refresh data when successful
     */
    public void saveExercise(JSONObject exerciseData, String idToken, 
                            ExerciseService.SaveCallback callback) {
        exerciseService.saveExercise(exerciseData, idToken, new ExerciseService.SaveCallback() {
            @Override
            public void onSuccess() {
                // Refresh the data after successful save
                refreshExercises();
                if (callback != null) {
                    callback.onSuccess();
                }
            }
            
            @Override
            public void onError(String error) {
                errorMessageLiveData.setValue(error);
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }
    
    /**
     * Update an existing exercise and refresh data when successful
     */
    public void updateExercise(String exerciseId, JSONObject exerciseData, String idToken,
                              ExerciseService.SaveCallback callback) {
        exerciseService.updateExercise(exerciseId, exerciseData, idToken, new ExerciseService.SaveCallback() {
            @Override
            public void onSuccess() {
                // Refresh the data after successful update
                refreshExercises();
                if (callback != null) {
                    callback.onSuccess();
                }
            }
            
            @Override
            public void onError(String error) {
                errorMessageLiveData.setValue(error);
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }
    
    /**
     * Delete an exercise and refresh data when successful
     */
    public void deleteExercise(String exerciseId, ExerciseService.DeleteCallback callback) {
        exerciseService.deleteExercise(exerciseId, new ExerciseService.DeleteCallback() {
            @Override
            public void onSuccess() {
                // Refresh the data after successful deletion
                refreshExercises();
                if (callback != null) {
                    callback.onSuccess();
                }
            }
            
            @Override
            public void onError(String error) {
                errorMessageLiveData.setValue(error);
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }
    
    /**
     * Get the LiveData object for exercises
     */
    public LiveData<List<JSONObject>> getExercises() {
        if (exercisesLiveData.getValue() == null) {
            refreshExercises();
        }
        return exercisesLiveData;
    }
    
    /**
     * Get the LiveData object for loading state
     */
    public LiveData<Boolean> getIsLoading() {
        return isLoadingLiveData;
    }
    
    /**
     * Get the LiveData object for error messages
     */
    public LiveData<String> getErrorMessage() {
        return errorMessageLiveData;
    }
} 