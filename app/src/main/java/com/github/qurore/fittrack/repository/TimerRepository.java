package com.github.qurore.fittrack.repository;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.github.qurore.fittrack.data.TimerUiState;
import com.github.qurore.fittrack.services.TimerForegroundService;

public class TimerRepository {

    private static volatile TimerRepository instance;

    private final MutableLiveData<TimerUiState> stopwatchState;
    private final MutableLiveData<TimerUiState> timerState;

    private TimerRepository() {
        stopwatchState = new MutableLiveData<>(TimerUiState.idle(TimerUiState.TimerType.STOPWATCH));
        timerState = new MutableLiveData<>(TimerUiState.idle(TimerUiState.TimerType.TIMER));
    }

    public static TimerRepository getInstance() {
        if (instance == null) {
            synchronized (TimerRepository.class) {
                if (instance == null) {
                    instance = new TimerRepository();
                }
            }
        }
        return instance;
    }

    public LiveData<TimerUiState> getStopwatchState() {
        return stopwatchState;
    }

    public LiveData<TimerUiState> getTimerState() {
        return timerState;
    }

    public void updateStopwatchState(TimerUiState state) {
        stopwatchState.postValue(state);
    }

    public void updateTimerState(TimerUiState state) {
        timerState.postValue(state);
    }

    public void sendCommand(Context context, String action, Bundle extras) {
        Intent intent = new Intent(context, TimerForegroundService.class);
        intent.putExtra(TimerForegroundService.EXTRA_ACTION, action);
        if (extras != null) {
            intent.putExtras(extras);
        }
        ContextCompat.startForegroundService(context, intent);
    }

    public void sendCommand(Context context, String action) {
        sendCommand(context, action, null);
    }
}
