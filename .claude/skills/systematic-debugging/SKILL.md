---
name: systematic-debugging
description: Systematically diagnose and fix bugs using evidence-driven root cause analysis
user-invocable: true
disable-model-invocation: false
allowed-tools: Read, Grep, Glob, Bash, Agent
argument-hint: [bug description or error message]
---

# Systematic Debugging

Debug `$ARGUMENTS` using evidence-driven root cause analysis. No guessing — follow the evidence.

## Phase 1: Reproduce & Classify (before touching any code)

### Collect Evidence
1. **Error message / stack trace** — read it fully, note the exact failing line
2. **Reproduction conditions** — always? intermittent? only on rotation? only offline?
3. **Recent changes** — `git log --oneline -20` to find what changed last
4. **Scope** — one screen? one flow? all flows using a specific component?

### Classify the Bug
| Class | Symptoms | First place to look |
|-------|----------|---------------------|
| **Crash** | Stack trace, ANR | Exception type + top frame |
| **Wrong state** | UI shows stale/incorrect data | ViewModel StateFlow, UseCase output |
| **Race condition** | Intermittent, rotation-sensitive | Mutex, scope, shared mutable state |
| **Compile error** | Build fails | Error line, import, API mismatch |
| **Test failure** | Red CI | Test assertion vs actual value, mock setup |
| **Performance** | Jank, slow load | `onDraw`, database query on Main thread |

## Phase 2: Hypothesis & Trace

Form **2-3 competing hypotheses** before reading code. Example:
- H1: DAO returns wrong data (sort order bug)
- H2: ViewModel maps entity incorrectly
- H3: Fragment collects wrong StateFlow

Then **eliminate** each with evidence:
```
H1: Read DAO query → ORDER BY correct ✅ eliminated
H2: Read mapper → field mapping correct ✅ eliminated  
H3: Read Fragment → collecting uiState2 instead of uiState ❌ BUG FOUND
```

## Phase 3: Code Investigation

### Android-Specific Checks

**Crash on rotation?**
- Missing `SavedStateHandle` for ViewModel args
- `_binding` not null-checked in callbacks after `onDestroyView()`
- Dialog shown after Fragment is detached

**Stale UI / not updating?**
- `StateFlow` collected outside `repeatOnLifecycle(STARTED)` → stops on background
- `stateIn(Eagerly)` instead of `WhileSubscribed` → replays old value
- `ListAdapter.submitList()` called with same list reference (use `.toList()` copy)

**Intermittent crash (coroutine)?**
- `CancellationException` swallowed in `catch (e: Exception)`
- `GlobalScope` or detached scope outliving Fragment
- Mutex never unlocked (missing `finally { mutex.unlock() }`)

**Nothing happens on button click?**
- Event emitted but Flow not collected (check `receiveAsFlow()` vs `consumeAsFlow()`)
- Channel buffer full (CONFLATED drops old — check if latest event matters)
- Click listener set in `onCreateView` but binding re-created in `onViewCreated`

**Test failure: mock not called?**
- `coEvery` needed for suspend functions (not `every`)
- Mock not initialized before test (`lateinit` not set in `@Before`)
- Wrong argument matcher (use `any()` to isolate)

**Build/compile error?**
- Check import: `kotlin.time.Clock` vs `kotlinx.datetime.Clock` (0.7.x migration)
- `@file:OptIn(kotlin.time.ExperimentalTime::class)` missing in call chain
- AGP 9: no `kotlinOptions` block, use `compileOptions` only

## Phase 4: Fix Verification

Before marking fixed:
- [ ] Root cause confirmed (not just symptom suppressed)
- [ ] Fix is minimal — only changed what was broken
- [ ] No new warnings introduced
- [ ] Related tests still pass (`./gradlew testDebugUnitTest`)
- [ ] If new test needed: write it to prevent regression

## Phase 5: Post-Fix Check

```bash
# Quick verification
./gradlew testDebugUnitTest --tests "*.AffectedClassTest"

# Full check before push
./gradlew testDebugUnitTest
./gradlew :app:detekt
```

## Anti-Patterns (don't do these)

- **Don't add null checks to hide NPE** — find why it's null
- **Don't catch Exception broadly** — classify and handle specifically  
- **Don't retry blindly** — if the same fix fails twice, the hypothesis is wrong
- **Don't add logging and call it fixed** — logging reveals; it doesn't fix
- **Don't suppress Detekt/lint** — fix the underlying issue

## Output Format

```
## Debug Report: [Bug Description]

### Evidence Collected
- Error: [exact message/stacktrace line]
- Reproduces: [always / intermittent / on rotation / etc.]
- Last relevant change: [git commit]

### Hypotheses
| # | Hypothesis | Evidence | Status |
|---|-----------|----------|--------|
| H1 | ... | checked file:line | ✅ Eliminated |
| H2 | ... | checked file:line | ❌ ROOT CAUSE |

### Root Cause
[One clear sentence: "X happens because Y at file:line"]

### Fix
[Minimal code change — file path, before/after]

### Verification
[How to confirm the fix works + which tests cover it]
```
