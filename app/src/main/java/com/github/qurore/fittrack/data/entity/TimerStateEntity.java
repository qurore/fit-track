package com.github.qurore.fittrack.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "timer_state", indices = {@Index(value = "timerType", unique = true)})
public class TimerStateEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String timerType; // "STOPWATCH" or "TIMER"

    @NonNull
    public String state; // "IDLE", "RUNNING", "PAUSED", "COMPLETED"

    public long startElapsedRealtime;
    public long accumulatedMillis;
    public long targetDurationMillis;
    public long targetEndTimeMillis;
    public long remainingMillis;
    public long lastPersistedAt;

    public TimerStateEntity() {
        this.timerType = "STOPWATCH";
        this.state = "IDLE";
    }
}
