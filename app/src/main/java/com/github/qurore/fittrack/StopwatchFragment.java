package com.github.qurore.fittrack;

import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.qurore.fittrack.data.TimerUiState;
import com.github.qurore.fittrack.repository.TimerRepository;
import com.github.qurore.fittrack.services.TimerForegroundService;
import com.google.android.material.button.MaterialButton;

public class StopwatchFragment extends Fragment {

    private TextView stopwatchDisplay;
    private MaterialButton btnStartPause;
    private MaterialButton btnReset;
    private TimerRepository repository;
    private TimerUiState.State currentState = TimerUiState.State.IDLE;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stopwatch, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        stopwatchDisplay = view.findViewById(R.id.stopwatchDisplay);
        btnStartPause = view.findViewById(R.id.btnStopwatchStartPause);
        btnReset = view.findViewById(R.id.btnStopwatchReset);

        repository = TimerRepository.getInstance();

        btnStartPause.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
            if (currentState == TimerUiState.State.RUNNING) {
                repository.sendCommand(requireContext(), TimerForegroundService.ACTION_PAUSE_STOPWATCH);
            } else {
                repository.sendCommand(requireContext(), TimerForegroundService.ACTION_START_STOPWATCH);
            }
        });

        btnReset.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.REJECT);
            repository.sendCommand(requireContext(), TimerForegroundService.ACTION_RESET_STOPWATCH);
        });

        repository.getStopwatchState().observe(getViewLifecycleOwner(), this::updateUi);
    }

    private void updateUi(TimerUiState state) {
        currentState = state.getState();
        long millis = state.getDisplayTimeMillis();

        // Update display
        String displayText = TimerForegroundService.formatTime(millis, true);
        stopwatchDisplay.setText(displayText);

        // Update accessibility (throttled by LiveData, no centiseconds for TalkBack)
        String accessibleText = formatAccessibleTime(millis);
        stopwatchDisplay.setContentDescription("Stopwatch: " + accessibleText);

        // Update buttons
        switch (currentState) {
            case IDLE:
                btnStartPause.setText("Start");
                btnStartPause.setBackgroundTintList(
                        getResources().getColorStateList(R.color.stopwatch_start, null));
                btnReset.setEnabled(false);
                break;
            case RUNNING:
                btnStartPause.setText("Pause");
                btnStartPause.setBackgroundTintList(
                        getResources().getColorStateList(R.color.stopwatch_pause, null));
                btnReset.setEnabled(true);
                break;
            case PAUSED:
                btnStartPause.setText("Resume");
                btnStartPause.setBackgroundTintList(
                        getResources().getColorStateList(R.color.stopwatch_start, null));
                btnReset.setEnabled(true);
                break;
        }
    }

    private String formatAccessibleTime(long millis) {
        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append(hours == 1 ? " hour, " : " hours, ");
        if (minutes > 0) sb.append(minutes).append(minutes == 1 ? " minute, " : " minutes, ");
        sb.append(seconds).append(seconds == 1 ? " second" : " seconds");
        return sb.toString();
    }
}
