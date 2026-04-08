package com.github.qurore.fittrack.services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.github.qurore.fittrack.DashboardActivity;
import com.github.qurore.fittrack.R;
import com.github.qurore.fittrack.TimerAlarmReceiver;
import com.github.qurore.fittrack.data.AppDatabase;
import com.github.qurore.fittrack.data.TimerUiState;
import com.github.qurore.fittrack.data.entity.TimerStateEntity;
import com.github.qurore.fittrack.repository.TimerRepository;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TimerForegroundService extends Service {

    public static final String EXTRA_ACTION = "extra_action";
    public static final String ACTION_START_STOPWATCH = "action_start_stopwatch";
    public static final String ACTION_PAUSE_STOPWATCH = "action_pause_stopwatch";
    public static final String ACTION_RESET_STOPWATCH = "action_reset_stopwatch";
    public static final String ACTION_START_TIMER = "action_start_timer";
    public static final String ACTION_PAUSE_TIMER = "action_pause_timer";
    public static final String ACTION_RESET_TIMER = "action_reset_timer";
    public static final String EXTRA_DURATION_MS = "extra_duration_ms";

    private static final String CHANNEL_RUNNING = "timer_running";
    private static final String CHANNEL_COMPLETED = "timer_completed";
    private static final int NOTIFICATION_ID = 2001;
    private static final int COMPLETION_NOTIFICATION_ID = 2002;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    private TimerUiState.State stopwatchState = TimerUiState.State.IDLE;
    private long swStartElapsedRealtime;
    private long swAccumulatedMillis;

    private TimerUiState.State timerState = TimerUiState.State.IDLE;
    private long timerTargetDurationMillis;
    private long timerEndElapsedRealtime;
    private long timerRemainingMillis;

    private TimerRepository repository;
    private long lastNotificationUpdate;

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            tick();
            if (isAnyRunning()) {
                handler.postDelayed(this, 10);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
        repository = TimerRepository.getInstance();
        restoreFromRoom();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildOngoingNotification());

        if (intent != null) {
            String action = intent.getStringExtra(EXTRA_ACTION);
            if (action != null) {
                handleAction(action, intent);
            }
        }

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(tickRunnable);
        dbExecutor.shutdown();
        super.onDestroy();
    }

    private void handleAction(String action, Intent intent) {
        switch (action) {
            case ACTION_START_STOPWATCH:
                startStopwatch();
                break;
            case ACTION_PAUSE_STOPWATCH:
                pauseStopwatch();
                break;
            case ACTION_RESET_STOPWATCH:
                resetStopwatch();
                break;
            case ACTION_START_TIMER:
                long duration = intent.getLongExtra(EXTRA_DURATION_MS, 0);
                startTimer(duration);
                break;
            case ACTION_PAUSE_TIMER:
                pauseTimer();
                break;
            case ACTION_RESET_TIMER:
                resetTimer();
                break;
        }
    }

    private void startStopwatch() {
        if (stopwatchState == TimerUiState.State.PAUSED) {
            swStartElapsedRealtime = SystemClock.elapsedRealtime();
        } else if (stopwatchState == TimerUiState.State.IDLE) {
            swStartElapsedRealtime = SystemClock.elapsedRealtime();
            swAccumulatedMillis = 0;
        }
        stopwatchState = TimerUiState.State.RUNNING;
        persistState("STOPWATCH");
        startTicking();
    }

    private void pauseStopwatch() {
        if (stopwatchState == TimerUiState.State.RUNNING) {
            swAccumulatedMillis += SystemClock.elapsedRealtime() - swStartElapsedRealtime;
            stopwatchState = TimerUiState.State.PAUSED;
            persistState("STOPWATCH");
            emitStopwatchState();
            stopIfAllIdle();
        }
    }

    private void resetStopwatch() {
        stopwatchState = TimerUiState.State.IDLE;
        swAccumulatedMillis = 0;
        swStartElapsedRealtime = 0;
        persistState("STOPWATCH");
        emitStopwatchState();
        stopIfAllIdle();
    }

    private void startTimer(long durationMillis) {
        if (timerState == TimerUiState.State.PAUSED) {
            timerEndElapsedRealtime = SystemClock.elapsedRealtime() + timerRemainingMillis;
        } else {
            timerTargetDurationMillis = durationMillis;
            timerRemainingMillis = durationMillis;
            timerEndElapsedRealtime = SystemClock.elapsedRealtime() + durationMillis;
        }
        timerState = TimerUiState.State.RUNNING;
        scheduleAlarm();
        persistState("TIMER");
        startTicking();
    }

    private void pauseTimer() {
        if (timerState == TimerUiState.State.RUNNING) {
            timerRemainingMillis = timerEndElapsedRealtime - SystemClock.elapsedRealtime();
            if (timerRemainingMillis < 0) timerRemainingMillis = 0;
            timerState = TimerUiState.State.PAUSED;
            cancelAlarm();
            persistState("TIMER");
            emitTimerState();
            stopIfAllIdle();
        }
    }

    private void resetTimer() {
        timerState = TimerUiState.State.IDLE;
        timerRemainingMillis = 0;
        timerEndElapsedRealtime = 0;
        cancelAlarm();
        persistState("TIMER");
        emitTimerState();
        stopIfAllIdle();
    }

    private void onTimerCompleted() {
        timerState = TimerUiState.State.COMPLETED;
        timerRemainingMillis = 0;
        cancelAlarm();
        persistState("TIMER");
        emitTimerState();

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(COMPLETION_NOTIFICATION_ID, buildCompletionNotification());

        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 500, 200, 500}, -1));
        }
    }

    private void tick() {
        if (stopwatchState == TimerUiState.State.RUNNING) {
            emitStopwatchState();
        }
        if (timerState == TimerUiState.State.RUNNING) {
            long remaining = timerEndElapsedRealtime - SystemClock.elapsedRealtime();
            if (remaining <= 0) {
                onTimerCompleted();
            } else {
                timerRemainingMillis = remaining;
                emitTimerState();
            }
        }

        long now = SystemClock.elapsedRealtime();
        if (now - lastNotificationUpdate >= 1000) {
            lastNotificationUpdate = now;
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.notify(NOTIFICATION_ID, buildOngoingNotification());
        }
    }

    private void startTicking() {
        handler.removeCallbacks(tickRunnable);
        handler.post(tickRunnable);
    }

    private boolean isAnyRunning() {
        return stopwatchState == TimerUiState.State.RUNNING || timerState == TimerUiState.State.RUNNING;
    }

    private void stopIfAllIdle() {
        if (stopwatchState == TimerUiState.State.IDLE && timerState == TimerUiState.State.IDLE) {
            stopForeground(true);
            stopSelf();
        } else if (!isAnyRunning()) {
            handler.removeCallbacks(tickRunnable);
        }
    }

    private void emitStopwatchState() {
        long elapsed = swAccumulatedMillis;
        if (stopwatchState == TimerUiState.State.RUNNING) {
            elapsed += SystemClock.elapsedRealtime() - swStartElapsedRealtime;
        }
        repository.updateStopwatchState(new TimerUiState(
                stopwatchState, elapsed, TimerUiState.TimerType.STOPWATCH, 0));
    }

    private void emitTimerState() {
        repository.updateTimerState(new TimerUiState(
                timerState, timerRemainingMillis, TimerUiState.TimerType.TIMER, timerTargetDurationMillis));
    }

    private void createNotificationChannels() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        NotificationChannel running = new NotificationChannel(
                CHANNEL_RUNNING, "Timer Running", NotificationManager.IMPORTANCE_LOW);
        running.setDescription("Shows while stopwatch or timer is active");
        nm.createNotificationChannel(running);

        NotificationChannel completed = new NotificationChannel(
                CHANNEL_COMPLETED, "Timer Completed", NotificationManager.IMPORTANCE_HIGH);
        completed.setDescription("Alert when countdown timer finishes");
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        completed.setSound(alarmSound, null);
        completed.enableVibration(true);
        nm.createNotificationChannel(completed);
    }

    private Notification buildOngoingNotification() {
        String title;
        String content;

        if (stopwatchState == TimerUiState.State.RUNNING) {
            long elapsed = swAccumulatedMillis + (SystemClock.elapsedRealtime() - swStartElapsedRealtime);
            title = "Stopwatch Running";
            content = formatTime(elapsed, false);
        } else if (timerState == TimerUiState.State.RUNNING) {
            title = "Timer Running";
            content = formatTime(timerRemainingMillis, false) + " remaining";
        } else if (stopwatchState == TimerUiState.State.PAUSED) {
            title = "Stopwatch Paused";
            content = formatTime(swAccumulatedMillis, false);
        } else if (timerState == TimerUiState.State.PAUSED) {
            title = "Timer Paused";
            content = formatTime(timerRemainingMillis, false) + " remaining";
        } else {
            title = "FitTrack Timer";
            content = "Ready";
        }

        Intent tapIntent = new Intent(this, DashboardActivity.class);
        tapIntent.putExtra("SELECT_TAB", "timer");
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingTap = PendingIntent.getActivity(this, 0, tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_RUNNING)
                .setSmallIcon(R.drawable.ic_timer_24)
                .setContentTitle(title)
                .setContentText(content)
                .setOngoing(true)
                .setContentIntent(pendingTap)
                .setSilent(true)
                .build();
    }

    private Notification buildCompletionNotification() {
        Intent tapIntent = new Intent(this, DashboardActivity.class);
        tapIntent.putExtra("SELECT_TAB", "timer");
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingTap = PendingIntent.getActivity(this, 1, tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

        return new NotificationCompat.Builder(this, CHANNEL_COMPLETED)
                .setSmallIcon(R.drawable.ic_timer_24)
                .setContentTitle("Timer Complete")
                .setContentText("Your countdown timer has finished!")
                .setAutoCancel(true)
                .setContentIntent(pendingTap)
                .setSound(alarmSound)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
    }

    private void scheduleAlarm() {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = getAlarmPendingIntent();
        long triggerAtMillis = System.currentTimeMillis() + timerRemainingMillis;
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
    }

    private void cancelAlarm() {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.cancel(getAlarmPendingIntent());
    }

    private PendingIntent getAlarmPendingIntent() {
        Intent intent = new Intent(this, TimerAlarmReceiver.class);
        return PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void persistState(String type) {
        dbExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            TimerStateEntity entity = new TimerStateEntity();
            entity.timerType = type;
            entity.lastPersistedAt = System.currentTimeMillis();

            if ("STOPWATCH".equals(type)) {
                entity.state = stopwatchState.name();
                entity.startElapsedRealtime = swStartElapsedRealtime;
                entity.accumulatedMillis = swAccumulatedMillis;
            } else {
                entity.state = timerState.name();
                entity.targetDurationMillis = timerTargetDurationMillis;
                entity.targetEndTimeMillis = timerEndElapsedRealtime;
                entity.remainingMillis = timerRemainingMillis;
            }

            db.timerStateDao().upsert(entity);
        });
    }

    private void restoreFromRoom() {
        dbExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());

            TimerStateEntity swEntity = db.timerStateDao().getByType("STOPWATCH");
            if (swEntity != null && !"IDLE".equals(swEntity.state)) {
                swAccumulatedMillis = swEntity.accumulatedMillis;
                if ("RUNNING".equals(swEntity.state)) {
                    swStartElapsedRealtime = SystemClock.elapsedRealtime();
                    stopwatchState = TimerUiState.State.RUNNING;
                    handler.post(this::startTicking);
                } else if ("PAUSED".equals(swEntity.state)) {
                    stopwatchState = TimerUiState.State.PAUSED;
                }
                handler.post(this::emitStopwatchState);
            }

            TimerStateEntity tmEntity = db.timerStateDao().getByType("TIMER");
            if (tmEntity != null && !"IDLE".equals(tmEntity.state)) {
                timerTargetDurationMillis = tmEntity.targetDurationMillis;
                if ("RUNNING".equals(tmEntity.state)) {
                    timerRemainingMillis = tmEntity.remainingMillis;
                    timerEndElapsedRealtime = SystemClock.elapsedRealtime() + timerRemainingMillis;
                    timerState = TimerUiState.State.RUNNING;
                    handler.post(this::startTicking);
                } else if ("PAUSED".equals(tmEntity.state)) {
                    timerRemainingMillis = tmEntity.remainingMillis;
                    timerState = TimerUiState.State.PAUSED;
                } else if ("COMPLETED".equals(tmEntity.state)) {
                    timerState = TimerUiState.State.COMPLETED;
                    timerRemainingMillis = 0;
                }
                handler.post(this::emitTimerState);
            }
        });
    }

    public static String formatTime(long millis, boolean showCentiseconds) {
        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (showCentiseconds) {
            long centiseconds = (millis % 1000) / 10;
            return String.format(Locale.US, "%02d:%02d:%02d.%02d", hours, minutes, seconds, centiseconds);
        } else {
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
        }
    }
}
