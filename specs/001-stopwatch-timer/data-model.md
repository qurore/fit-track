# Data Model: Stopwatch and Timer Module

**Feature**: 001-stopwatch-timer
**Date**: 2026-04-08

## Entities

### TimerStateEntity

Represents the persisted state of a stopwatch or timer instance.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | int | Primary Key, auto-generate | Unique row identifier |
| timerType | String | NOT NULL, UNIQUE | "STOPWATCH" or "TIMER" |
| state | String | NOT NULL | "IDLE", "RUNNING", "PAUSED", "COMPLETED" |
| startElapsedRealtime | long | default 0 | SystemClock.elapsedRealtime() at last start |
| accumulatedMillis | long | default 0 | Stopwatch: elapsed time accumulated before last pause |
| targetDurationMillis | long | default 0 | Timer: total countdown duration set by user |
| targetEndTimeMillis | long | default 0 | Timer: elapsedRealtime when countdown should end |
| remainingMillis | long | default 0 | Timer: remaining time when paused |
| lastPersistedAt | long | NOT NULL | System.currentTimeMillis() at last write |

**Relationships**: None (standalone entity)
**Max rows**: 2 (one per timer type)
**Conflict strategy**: REPLACE on insert (upsert by timerType)

### TimerUiState (in-memory only)

Represents the current UI state observed by fragments via LiveData.

| Field | Type | Description |
|-------|------|-------------|
| state | enum: IDLE, RUNNING, PAUSED, COMPLETED | Current timing state |
| displayTimeMillis | long | Elapsed (stopwatch) or remaining (timer) |
| timerType | enum: STOPWATCH, TIMER | Which timing mode |
| targetDurationMillis | long | Timer: original target duration |

## State Transitions

### Stopwatch

```
IDLE --[Start]--> RUNNING
RUNNING --[Pause]--> PAUSED
PAUSED --[Resume]--> RUNNING
RUNNING --[Reset]--> IDLE
PAUSED --[Reset]--> IDLE
```

### Timer

```
IDLE --[Start(duration)]--> RUNNING
RUNNING --[Pause]--> PAUSED
PAUSED --[Resume]--> RUNNING
RUNNING --[Countdown=0]--> COMPLETED
RUNNING --[Cancel]--> IDLE
PAUSED --[Cancel]--> IDLE
COMPLETED --[Dismiss]--> IDLE
```

## Validation Rules

- Timer cannot start with duration = 0
- State transitions must follow the defined state machine (invalid transitions are no-ops)
- Persisted state older than 24 hours is considered stale and reset to IDLE on restore
