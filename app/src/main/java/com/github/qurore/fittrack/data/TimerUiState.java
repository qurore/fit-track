package com.github.qurore.fittrack.data;

public class TimerUiState {

    public enum State {
        IDLE, RUNNING, PAUSED, COMPLETED
    }

    public enum TimerType {
        STOPWATCH, TIMER
    }

    private final State state;
    private final long displayTimeMillis;
    private final TimerType timerType;
    private final long targetDurationMillis;

    public TimerUiState(State state, long displayTimeMillis, TimerType timerType, long targetDurationMillis) {
        this.state = state;
        this.displayTimeMillis = displayTimeMillis;
        this.timerType = timerType;
        this.targetDurationMillis = targetDurationMillis;
    }

    public State getState() {
        return state;
    }

    public long getDisplayTimeMillis() {
        return displayTimeMillis;
    }

    public TimerType getTimerType() {
        return timerType;
    }

    public long getTargetDurationMillis() {
        return targetDurationMillis;
    }

    public static TimerUiState idle(TimerType type) {
        return new TimerUiState(State.IDLE, 0, type, 0);
    }
}
