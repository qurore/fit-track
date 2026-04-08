# Research: Stopwatch and Timer Module

**Feature**: 001-stopwatch-timer
**Date**: 2026-04-08

## Research Tasks

### 1. Android Timing Mechanisms

**Decision**: `SystemClock.elapsedRealtime()`
**Rationale**: Monotonic, includes deep sleep, unaffected by wall-clock adjustments. Standard Android practice for stopwatch implementations.
**Alternatives considered**:
- `System.currentTimeMillis()`: Affected by user changing system time or timezone
- `System.nanoTime()`: Does not include deep sleep; unsuitable for background timing
- `Handler.postDelayed()` accumulation: Drifts due to thread scheduling delays

### 2. Background Service Strategy

**Decision**: Foreground Service with `START_STICKY`
**Rationale**: Highest priority for background execution on Android. Persistent notification signals to OS that the app is performing user-visible work.
**Alternatives considered**:
- WorkManager: Designed for deferrable work, not real-time UI updates
- AlarmManager only: Cannot provide continuous UI tick updates
- Background Service: Killed aggressively on modern Android versions

### 3. Local Persistence for Timer State

**Decision**: Room database
**Rationale**: Type-safe, LiveData-compatible, handles migrations, survives force-stop. Integration with existing Repository-LiveData pattern.
**Alternatives considered**:
- SharedPreferences: No structured queries, no LiveData, manual serialization
- Direct SQLite: More boilerplate, no compile-time verification

### 4. Timer Completion in Doze Mode

**Decision**: `AlarmManager.setExactAndAllowWhileIdle()` as backup
**Rationale**: Fires even during Doze mode. Acts as redundant safety net in case foreground service is killed.
**Alternatives considered**:
- Relying solely on foreground service: Some OEMs kill services aggressively
- `setExact()`: Not guaranteed during Doze mode

### 5. Notification Permission (API 33+)

**Decision**: Request `POST_NOTIFICATIONS` lazily when timer is first started
**Rationale**: Avoids upfront permission prompts for users who only use stopwatch. Graceful degradation if denied (timer still works, just no background alert).
**Alternatives considered**:
- Request on app launch: Unnecessary if user never uses timer
- Never request: Foreground service notification exempt, but completion alert would be suppressed
