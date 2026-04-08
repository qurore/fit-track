# Implementation Plan: Stopwatch and Timer Module

**Branch**: `test_b_speckit` | **Date**: 2026-04-08 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-stopwatch-timer/spec.md`

## Summary

Add a stopwatch and countdown timer accessible from a 5th bottom navigation item. The stopwatch is the default view with a tab control to switch to timer. Timing must continue in the background and survive app process death. Timer completion triggers an audible/vibration alert.

## Technical Context

**Language/Version**: Java 11
**Primary Dependencies**: Material Design, AndroidX Lifecycle (ViewModel, LiveData), Room, ViewPager2
**Storage**: Room (local SQLite for timing state persistence)
**Testing**: JUnit 4.13.2 (unit), Espresso 3.6.1 (instrumentation)
**Target Platform**: Android 8.0+ (API 26), targeting API 34
**Project Type**: Mobile app (Android)
**Performance Goals**: Stopwatch display updates at 10ms intervals; timer alerts within 2 seconds of target
**Constraints**: Must work in Doze mode; must survive process death; max 5 bottom nav items
**Scale/Scope**: Single-user local feature, no backend changes

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Architecture Consistency | PASS | Follows Activity-Fragment-Repository pattern; visibility toggling for nav |
| II. Test-First for Business Logic | PASS | Unit tests planned for timing utilities |
| III. Android Platform Compliance | PASS | POST_NOTIFICATIONS for API 33+; foregroundServiceType declared; notification channels created |
| IV. Naming and Style Conformance | PASS | Follows existing PascalCase classes, camelCase methods, snake_case XML |
| V. Minimal Footprint | PASS | DashboardActivity changes limited to new nav item + visibility toggling |

No violations. Proceeding to Phase 0.

## Project Structure

### Documentation (this feature)

```text
specs/001-stopwatch-timer/
├── spec.md
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
└── checklists/
    └── requirements.md  # Spec quality checklist
```

### Source Code (new files)

```text
app/src/main/java/com/github/qurore/fittrack/
├── StopwatchFragment.java          # Stopwatch UI
├── CountdownTimerFragment.java     # Timer UI
├── TimerTabPagerAdapter.java       # ViewPager2 adapter for tabs
├── TimerAlarmReceiver.java         # BroadcastReceiver for timer backup
├── data/
│   ├── AppDatabase.java            # Room database singleton
│   ├── TimerUiState.java           # UI state data class
│   ├── entity/
│   │   └── TimerStateEntity.java   # Room entity
│   └── dao/
│       └── TimerStateDao.java      # Room DAO
├── repository/
│   └── TimerRepository.java        # Timer data repository
└── services/
    └── TimerForegroundService.java # Foreground service

app/src/main/res/
├── drawable/ic_timer_24.xml        # Nav icon
├── layout/fragment_stopwatch.xml
└── layout/fragment_countdown_timer.xml

app/src/test/java/.../
└── TimerFormatTest.java            # Unit tests
```

### Modified files

```text
app/build.gradle.kts                # Add Room, lifecycle deps
app/src/main/AndroidManifest.xml    # Permissions, service, receiver
app/src/main/res/menu/bottom_navigation_menu.xml  # 5th nav item
app/src/main/res/layout/activity_dashboard.xml     # Timer ScrollView
app/src/main/res/values/colors.xml  # Timer button colors
app/src/main/res/values/strings.xml # Timer strings
DashboardActivity.java              # Timer tab wiring
```

---

## Phase 0: Research

### Decision 1: Timing Mechanism
- **Decision**: Use `SystemClock.elapsedRealtime()` as canonical time source
- **Rationale**: Monotonic clock that includes deep sleep time; immune to wall-clock changes
- **Alternatives considered**: `System.currentTimeMillis()` (affected by user clock changes), `Handler` accumulation (drifts over time)

### Decision 2: Background Execution
- **Decision**: Foreground Service with `START_STICKY` + AlarmManager backup
- **Rationale**: Foreground service is the strongest guarantee against process death; AlarmManager provides redundancy for timer completion
- **Alternatives considered**: WorkManager (not suitable for real-time UI updates), JobScheduler (cannot guarantee exact timing)

### Decision 3: State Persistence
- **Decision**: Room database with `TimerStateEntity`
- **Rationale**: Provides typed, queryable persistence with LiveData integration; survives force-stop
- **Alternatives considered**: SharedPreferences (no typed queries, no LiveData), SQLite directly (more boilerplate)

### Decision 4: Annotation Processing
- **Decision**: Use `annotationProcessor` (not kapt) for Room compiler
- **Rationale**: Project is pure Java; kapt requires Kotlin plugin which is unnecessary overhead
- **Alternatives considered**: kapt (requires adding Kotlin Gradle plugin)

### Decision 5: Foreground Service Type
- **Decision**: `foregroundServiceType="specialUse"` with workout_timer subtype
- **Rationale**: Android 14 (API 34) requires foregroundServiceType; stopwatch/timer is not a recognized type
- **Alternatives considered**: `mediaPlayback` (incorrect semantics), no type (crashes on API 34)

---

## Phase 1: Design

### Data Model

**TimerStateEntity** (Room entity, table: `timer_state`)

| Field | Type | Description |
|-------|------|-------------|
| id | int (PK, auto) | Row ID |
| timerType | String (UNIQUE) | "STOPWATCH" or "TIMER" |
| state | String | "IDLE", "RUNNING", "PAUSED", "COMPLETED" |
| startElapsedRealtime | long | SystemClock.elapsedRealtime() at start |
| accumulatedMillis | long | Elapsed time before last pause |
| targetDurationMillis | long | Timer: total countdown duration |
| targetEndTimeMillis | long | Timer: elapsedRealtime when timer ends |
| remainingMillis | long | Timer: remaining when paused |
| lastPersistedAt | long | System.currentTimeMillis() at write |

**TimerUiState** (data class for UI observation)

| Field | Type | Description |
|-------|------|-------------|
| state | enum (IDLE/RUNNING/PAUSED/COMPLETED) | Current state |
| displayTimeMillis | long | Elapsed (stopwatch) or remaining (timer) |
| timerType | enum (STOPWATCH/TIMER) | Which timing mode |
| targetDurationMillis | long | Timer: original target duration |

### State Machines

**Stopwatch**: IDLE → RUNNING → PAUSED → RUNNING (cycle) | (RUNNING/PAUSED) → IDLE (reset)

**Timer**: IDLE → RUNNING → PAUSED → RUNNING (cycle) | RUNNING → COMPLETED → IDLE | (RUNNING/PAUSED) → IDLE (cancel)

### Notification Channels

| Channel | Importance | Purpose |
|---------|-----------|---------|
| `timer_running` | LOW | Ongoing notification while active |
| `timer_completed` | HIGH | Completion alert with sound/vibration |

### Implementation Order

1. **Dependencies**: Add Room, lifecycle-viewmodel, lifecycle-livedata to build.gradle.kts
2. **Data layer**: TimerStateEntity, TimerStateDao, AppDatabase, TimerUiState
3. **Repository**: TimerRepository (singleton, LiveData, service commands)
4. **Navigation**: bottom_navigation_menu.xml, ic_timer_24.xml, activity_dashboard.xml
5. **Tab adapter**: TimerTabPagerAdapter
6. **Fragments**: StopwatchFragment, CountdownTimerFragment (layouts + Java)
7. **Service**: TimerForegroundService (timing, notifications, Room persistence)
8. **Alarm backup**: TimerAlarmReceiver, AlarmManager scheduling
9. **Permissions**: POST_NOTIFICATIONS handling, manifest declarations
10. **DashboardActivity**: showTimerContent(), visibility toggling, badge dot
11. **Tests**: Unit tests for time formatting
