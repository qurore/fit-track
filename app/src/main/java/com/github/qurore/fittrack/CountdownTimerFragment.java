package com.github.qurore.fittrack;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.qurore.fittrack.data.TimerUiState;
import com.github.qurore.fittrack.repository.TimerRepository;
import com.github.qurore.fittrack.services.TimerForegroundService;
import com.google.android.material.button.MaterialButton;

public class CountdownTimerFragment extends Fragment {

    private NumberPicker hourPicker;
    private NumberPicker minutePicker;
    private NumberPicker secondPicker;
    private View pickerContainer;
    private TextView timerDisplay;
    private TextView timesUpText;
    private MaterialButton btnStartPause;
    private MaterialButton btnReset;

    private TimerRepository repository;
    private TimerUiState.State currentState = TimerUiState.State.IDLE;
    private long lastSetDurationMillis;

    private int pendingHours, pendingMinutes, pendingSeconds;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) {
                    Toast.makeText(requireContext(),
                            "Timer alerts will not be shown. Enable notifications in Settings.",
                            Toast.LENGTH_LONG).show();
                }
                startTimerWithDuration(pendingHours, pendingMinutes, pendingSeconds);
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_countdown_timer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pickerContainer = view.findViewById(R.id.pickerContainer);
        hourPicker = view.findViewById(R.id.hourPicker);
        minutePicker = view.findViewById(R.id.minutePicker);
        secondPicker = view.findViewById(R.id.secondPicker);
        timerDisplay = view.findViewById(R.id.timerDisplay);
        timesUpText = view.findViewById(R.id.timesUpText);
        btnStartPause = view.findViewById(R.id.btnTimerStartPause);
        btnReset = view.findViewById(R.id.btnTimerReset);

        setupPickers();

        repository = TimerRepository.getInstance();

        btnStartPause.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
            handleStartPause();
        });

        btnReset.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.REJECT);
            repository.sendCommand(requireContext(), TimerForegroundService.ACTION_RESET_TIMER);
        });

        repository.getTimerState().observe(getViewLifecycleOwner(), this::updateUi);
    }

    private void setupPickers() {
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        secondPicker.setMinValue(0);
        secondPicker.setMaxValue(59);
        minutePicker.setValue(5);
    }

    private void handleStartPause() {
        switch (currentState) {
            case IDLE:
                int h = hourPicker.getValue();
                int m = minutePicker.getValue();
                int s = secondPicker.getValue();

                if (h == 0 && m == 0 && s == 0) {
                    Toast.makeText(requireContext(), "Set a duration greater than zero", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        pendingHours = h;
                        pendingMinutes = m;
                        pendingSeconds = s;
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                        return;
                    }
                }

                startTimerWithDuration(h, m, s);
                break;

            case RUNNING:
                repository.sendCommand(requireContext(), TimerForegroundService.ACTION_PAUSE_TIMER);
                break;

            case PAUSED:
                repository.sendCommand(requireContext(), TimerForegroundService.ACTION_START_TIMER);
                break;

            case COMPLETED:
                repository.sendCommand(requireContext(), TimerForegroundService.ACTION_RESET_TIMER);
                break;
        }
    }

    private void startTimerWithDuration(int hours, int minutes, int seconds) {
        long durationMs = ((hours * 3600L) + (minutes * 60L) + seconds) * 1000L;
        lastSetDurationMillis = durationMs;
        Bundle extras = new Bundle();
        extras.putLong(TimerForegroundService.EXTRA_DURATION_MS, durationMs);
        repository.sendCommand(requireContext(), TimerForegroundService.ACTION_START_TIMER, extras);
    }

    private void updateUi(TimerUiState state) {
        currentState = state.getState();
        long millis = state.getDisplayTimeMillis();

        switch (currentState) {
            case IDLE:
                pickerContainer.setVisibility(View.VISIBLE);
                timerDisplay.setVisibility(View.GONE);
                timesUpText.setVisibility(View.GONE);
                btnStartPause.setText("Start");
                btnStartPause.setBackgroundTintList(
                        getResources().getColorStateList(R.color.stopwatch_start, null));
                btnReset.setVisibility(View.GONE);
                if (lastSetDurationMillis > 0) {
                    long totalSec = lastSetDurationMillis / 1000;
                    hourPicker.setValue((int) (totalSec / 3600));
                    minutePicker.setValue((int) ((totalSec % 3600) / 60));
                    secondPicker.setValue((int) (totalSec % 60));
                }
                break;

            case RUNNING:
                pickerContainer.setVisibility(View.GONE);
                timerDisplay.setVisibility(View.VISIBLE);
                timesUpText.setVisibility(View.GONE);
                timerDisplay.setText(TimerForegroundService.formatTime(millis, false));
                timerDisplay.setContentDescription("Timer: " + formatAccessibleTime(millis));
                btnStartPause.setText("Pause");
                btnStartPause.setBackgroundTintList(
                        getResources().getColorStateList(R.color.stopwatch_pause, null));
                btnReset.setVisibility(View.VISIBLE);
                btnReset.setText("Cancel");
                break;

            case PAUSED:
                pickerContainer.setVisibility(View.GONE);
                timerDisplay.setVisibility(View.VISIBLE);
                timesUpText.setVisibility(View.GONE);
                timerDisplay.setText(TimerForegroundService.formatTime(millis, false));
                timerDisplay.setContentDescription("Timer paused: " + formatAccessibleTime(millis));
                btnStartPause.setText("Resume");
                btnStartPause.setBackgroundTintList(
                        getResources().getColorStateList(R.color.stopwatch_start, null));
                btnReset.setVisibility(View.VISIBLE);
                btnReset.setText("Cancel");
                break;

            case COMPLETED:
                pickerContainer.setVisibility(View.GONE);
                timerDisplay.setVisibility(View.VISIBLE);
                timerDisplay.setText("00:00:00");
                timerDisplay.setContentDescription("Timer complete");
                timesUpText.setVisibility(View.VISIBLE);
                btnStartPause.setText("Dismiss");
                btnStartPause.setBackgroundTintList(
                        getResources().getColorStateList(R.color.stopwatch_start, null));
                btnReset.setVisibility(View.GONE);
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
