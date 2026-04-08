# FitTrack: Android Fitness Tracking Application

## Project Overview

FitTrack is a Java-based Android fitness tracking application with an AWS Lambda (Python) serverless backend, Firebase Authentication, and MongoDB Atlas for data persistence.

- **Android**: Java, Min SDK 26, Target SDK 34, Gradle Kotlin DSL
- **Backend**: Python 3.9, AWS SAM, Lambda functions
- **Auth**: Firebase Authentication + Google Sign-In
- **Database**: MongoDB Atlas
- **Key Libraries**: Volley (HTTP), MPAndroidChart (charts), Material Design

---

## CRITICAL RULES: Agent Behavior

### English-Only Code Output

**All code output MUST be written in English.** Using other languages (Japanese, etc.) in code, comments, or resource files (except strings.xml translations) is STRICTLY PROHIBITED.

### Mandatory Opus Model for All Subagents

**ALL subagents via Task tool MUST use `model: "opus"`.** This applies to all SE Pipeline phases, Bar Raisers, and any other multi-agent workflow. Do NOT omit the model parameter or use a cheaper model.

---

## CRITICAL RULE: Pipeline Governance

### Intent Classification

**Before responding to ANY user message that requests implementation, classify intent:**

1. **Does it require file modifications?** NO -> **Advisory** (respond directly, no pipeline)
2. **Is it a trivial fix?** (1 file, <=3 lines, cosmetic only) -> YES: apply directly without pipeline
3. **Is it a complex feature, new screen, architectural change, or multi-module modification?** -> YES: **Full Lifecycle** -> `/se-pipeline`
4. **Is it a moderate change with clear requirements?** -> Use your best judgment; suggest `/se-pipeline` for anything touching 3+ files or requiring new classes

### When to Invoke the SE Pipeline

The SE Pipeline (`/se-pipeline`) MUST be invoked for:
- New Activity or Fragment creation
- New backend Lambda function
- Feature requiring changes across 3+ existing files
- Any change affecting both Android app and backend
- Database schema changes
- New user-facing screens or flows

The SE Pipeline SHOULD be suggested for:
- Changes touching multiple architectural layers (UI + Service + Repository)
- Features with unclear or ambiguous requirements
- Changes that could affect existing functionality

### Pipeline Overview

| Pipeline | Skill | Purpose |
|----------|-------|---------|
| **SE Pipeline** | `/se-pipeline` | Full lifecycle: new features, architecture, multi-module changes |

**Phases:**

| Phase | Name | Skill | Gate |
|-------|------|-------|------|
| 0 | Codebase Exploration | `/se-0-codebase-exploration` | Informational (no gate) |
| 1 | Prompt Analysis | `/se-1-prompt-analysis` | Scope validated |
| 2 | Prompt Requirements | `/se-2-prompt-requirements` | Traceable to Phase 1 |
| 3 | SE Planning | `/se-3-planning` | Feasible, dependencies correct |
| 4 | SE Requirements | `/se-4-requirements` | Complete, traceable |
| 5 | Analysis & Design | `/se-5-design` | All 4 stakeholders approve |
| 5.5 | UX Bar Raiser (Design) | `/se-5-5-bar-raiser` | Mandatory critique, forces Phase 5 redo |
| 6 | Implementation | `/se-6-implementation` | Per-task-group checkpoint |
| 7 | Testing | `/se-7-testing` | All tests pass, coverage adequate |
| 7.5 | UX Bar Raiser (Impl) | `/se-7-5-bar-raiser` | Mandatory critique, forces Phase 6+7 redo |
| 8 | Evaluation | `/se-8-evaluation` | R1+R2+R3 all pass |
| 9 | Final Approval | `/se-9-approval` | PM -> CTO -> CEO sequential |

**VIOLATION: Producing complex multi-file output without invoking the SE Pipeline is prohibited when the intent classification routes to it.** Exceptions: trivial fixes, advisory responses.

### Phase Skip Policy

| Output Type | Skipped Phases |
|-------------|---------------|
| Code + Tests (default) | None — full pipeline |
| Documentation only | Phase 7, Phase 7.5 |
| Code + Documentation | None — code present -> full |

---

## Architecture & Conventions

### Package Structure

```
com.github.qurore.fittrack/
  ├── adapters/        # RecyclerView adapters
  ├── repository/      # Data access layer (API calls)
  ├── services/        # Business logic layer
  └── [Activities/Fragments at root]
```

### Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Activities | PascalCase + "Activity" | `DashboardActivity` |
| Fragments | PascalCase + "Fragment" | `HistoryTabContentFragment` |
| Layouts | snake_case with prefix | `activity_dashboard`, `fragment_history` |
| Resource IDs | snake_case | `btn_record`, `tv_calories` |
| Java classes | PascalCase | `ExerciseRepository` |
| Methods | camelCase | `getExercises()` |
| Constants | UPPER_SNAKE_CASE | `BASE_URL` |

### Dependency Flow

```
Activity/Fragment -> Service -> Repository -> API (Volley)
```

Do NOT bypass layers (e.g., Activity directly calling Repository is a violation).

### Testing

| Scope | Framework | Location |
|-------|-----------|----------|
| Unit tests | JUnit 4 | `app/src/test/java/` |
| UI tests | Espresso | `app/src/androidTest/java/` |
| Test runner | AndroidJUnitRunner | build.gradle.kts |

### Backend Pattern

Lambda functions in `backend/` follow:
- One function per resource/domain
- Firebase token validation via `auth-function`
- MongoDB operations via PyMongo
- SAM template for deployment configuration

---

## Spec-kit Integration

This project also has GitHub Spec-kit installed for spec-driven development:
- Templates: `.specify/templates/`
- Scripts: `.specify/scripts/`
- Claude commands: `.claude/commands/speckit.*.md`

Spec-kit commands (`/speckit.*`) are available for specification-driven workflows as an alternative to the SE Pipeline.
