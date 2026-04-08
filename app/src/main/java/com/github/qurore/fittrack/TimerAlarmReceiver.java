package com.github.qurore.fittrack;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.core.app.NotificationCompat;

import com.github.qurore.fittrack.data.AppDatabase;
import com.github.qurore.fittrack.data.TimerUiState;
import com.github.qurore.fittrack.data.entity.TimerStateEntity;
import com.github.qurore.fittrack.repository.TimerRepository;

import java.util.concurrent.Executors;

public class TimerAlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_COMPLETED = "timer_completed";
    private static final int COMPLETION_NOTIFICATION_ID = 2002;

    @Override
    public void onReceive(Context context, Intent intent) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            TimerStateEntity entity = db.timerStateDao().getByType("TIMER");
            if (entity != null && "RUNNING".equals(entity.state)) {
                entity.state = "COMPLETED";
                entity.remainingMillis = 0;
                entity.lastPersistedAt = System.currentTimeMillis();
                db.timerStateDao().upsert(entity);
            }
        });

        TimerRepository repo = TimerRepository.getInstance();
        repo.updateTimerState(new TimerUiState(
                TimerUiState.State.COMPLETED, 0, TimerUiState.TimerType.TIMER, 0));

        Intent tapIntent = new Intent(context, DashboardActivity.class);
        tapIntent.putExtra("SELECT_TAB", "timer");
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingTap = PendingIntent.getActivity(context, 1, tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_COMPLETED)
                .setSmallIcon(R.drawable.ic_timer_24)
                .setContentTitle("Timer Complete")
                .setContentText("Your countdown timer has finished!")
                .setAutoCancel(true)
                .setContentIntent(pendingTap)
                .setSound(alarmSound)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(COMPLETION_NOTIFICATION_ID, builder.build());

        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 500, 200, 500}, -1));
        }
    }
}
