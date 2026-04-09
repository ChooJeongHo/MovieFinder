---
name: kotlin-concurrency-expert
description: Diagnose and fix Kotlin coroutine race conditions, Flow issues, and thread safety bugs
user-invocable: true
disable-model-invocation: false
allowed-tools: Read, Grep, Glob, Agent
argument-hint: [file-path or symptom description]
---

# Kotlin Concurrency Expert

Analyze `$ARGUMENTS` for race conditions, coroutine misuse, and thread safety issues.

## Race Condition Patterns to Hunt

### 1. Duplicate Invocation (check-then-act gap)
```kotlin
// UNSAFE: two coroutines can both pass the check
if (!isLoading) {
    isLoading = true
    fetchData()
}

// SAFE: Mutex.tryLock() is atomic
if (loadMutex.tryLock()) {
    try { fetchData() } finally { loadMutex.unlock() }
}
```
→ In this project: `DetailViewModel` uses `Mutex.tryLock()` for API dedup, `toggleMutex.withLock()` for FAB debounce.

### 2. StateFlow/SharedFlow Emission Race
```kotlin
// UNSAFE: read-modify-write is not atomic
_uiState.value = _uiState.value.copy(isLoading = true)  // concurrent writers corrupt state

// SAFE: update{} is atomic
_uiState.update { it.copy(isLoading = true) }
```

### 3. Cold Flow Collected Multiple Times
```kotlin
// UNSAFE: each collector triggers independent upstream (DB query runs N times)
val data = repository.getData()  // cold Flow, no sharing

// SAFE: stateIn shares a single upstream subscription
val data = repository.getData()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
```

### 4. CancellationException Swallowed
```kotlin
// UNSAFE: catches CancellationException, breaks structured concurrency
try { ... } catch (e: Exception) { handleError(e) }

// SAFE: rethrow CancellationException
try { ... } catch (e: CancellationException) { throw e } catch (e: Exception) { handleError(e) }
```

### 5. Wrong Scope / Scope Escape
```kotlin
// UNSAFE: GlobalScope survives ViewModel destruction → leak
GlobalScope.launch { repository.save() }

// SAFE: viewModelScope (auto-cancelled on ViewModel clear)
viewModelScope.launch { repository.save() }

// For fire-and-forget that must outlive a specific coroutine:
// use a separate viewModelScope.launch{} block, NOT coroutineScope{}
```

### 6. Channel Hot/Cold Confusion
```kotlin
// In this project: snackbar events use Channel.CONFLATED
// CONFLATED = only latest event kept if consumer is slow
// Use receiveAsFlow() — NOT consumeAsFlow() (single-consumer only)
val events = _channel.receiveAsFlow()
```

### 7. Flow on Wrong Dispatcher
```kotlin
// UNSAFE: Room/Retrofit called on Main thread
flow { emit(dao.query()) }  // no dispatcher → runs on caller's dispatcher

// SAFE: flowOn moves upstream to IO
repository.getData().flowOn(Dispatchers.IO)

// In this project: use flowOn in Repository, NOT in ViewModel
```

### 8. Mutex Deadlock
```kotlin
// UNSAFE: nested withLock on same Mutex → deadlock
mutex.withLock {
    mutex.withLock { ... }  // hangs forever
}

// Check: loadMutex and toggleMutex must be SEPARATE instances in DetailViewModel
```

## This Project's Concurrency Inventory

| Component | Mechanism | Purpose |
|-----------|-----------|---------|
| `DetailViewModel.loadMutex` | `Mutex.tryLock()` | Prevent duplicate API calls |
| `DetailViewModel.toggleMutex` | `Mutex.withLock()` | FAB toggle debounce |
| `WatchGoalNotificationHelper` | `Mutex.withLock()` | Atomic goal-check + notify |
| `RateLimiter` | `AtomicLong.compareAndSet` | 2s retry cooldown |
| `CircuitBreaker` | `@Synchronized` | State transition atomicity |
| All ViewModels | `Channel.CONFLATED` | One-shot Snackbar events |
| Home/Detail flows | `stateIn(WhileSubscribed(5000))` | Shared upstream, 5s buffer |

## Diagnosis Workflow

1. **Find the symptom** — double API call? stale UI? crash on rotation?
2. **Locate the shared mutable state** — which variable is accessed from multiple coroutines?
3. **Trace the happens-before** — is there a lock/atomic between write and read?
4. **Check scope** — does the coroutine outlive its intended owner?
5. **Verify dispatcher** — is blocking I/O on IO dispatcher, UI updates on Main?

## Output Format

```
## Concurrency Analysis: [Target]

### Race Conditions Found
- **[CRITICAL/WARNING]** (file:line): Pattern → Risk → Fix

### Thread Safety Assessment
| Variable/Flow | Access Pattern | Safe? | Fix Needed |
|---------------|---------------|-------|------------|
| _uiState | update{} | ✅ | - |
| isLoading | read-then-write | ❌ | Use Mutex |

### Scope & Lifecycle
[Are all coroutines in appropriate scopes? Any leak risk?]

### Dispatcher Correctness
[Are blocking calls on IO? UI updates on Main?]

### Recommended Fixes
[Concrete before/after code for each issue]
```
