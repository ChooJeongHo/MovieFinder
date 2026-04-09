---
name: claude-android-skill
description: Enforce Clean Architecture and Offline-first patterns in this MovieFinder codebase
user-invocable: true
disable-model-invocation: false
allowed-tools: Read, Grep, Glob, Agent
argument-hint: [file-path or feature-description]
---

# Android Clean Architecture & Offline-First Enforcer

Analyze or implement `$ARGUMENTS` with strict enforcement of this project's architectural rules.

## Layer Rules (Non-Negotiable)

```
Presentation → Domain (UseCase/Repository interface) only
Data → Domain interfaces only
Core → available everywhere
FORBIDDEN: Presentation → Data (direct DAO/API access)
FORBIDDEN: Domain → Android framework (except Paging)
```

## Clean Architecture Checklist

### UseCase
- [ ] One public operator fun `invoke()` only
- [ ] Delegates to exactly one repository method (no business logic in ViewModel)
- [ ] Returns `Flow<T>` or `suspend` result — never raw lists
- [ ] No Android imports (except `androidx.paging`)

### Repository Interface (Domain layer)
- [ ] ISP applied: split by domain (Favorite, Watchlist, Search, etc.)
- [ ] Methods return domain models only — no DTOs, no Room entities
- [ ] Suspend functions for mutations, Flow for observations

### Repository Implementation (Data layer)
- [ ] Maps DTO/Entity → domain model at the data boundary
- [ ] Room queries use `ORDER BY` for sorting (not `sortedBy` in ViewModel)
- [ ] Detekt LongMethod limit: extract helpers if method > 60 lines

### ViewModel
- [ ] Exposes `StateFlow<UiState>` (sealed class: Loading/Success/Error)
- [ ] Uses `Channel.CONFLATED` + `receiveAsFlow()` for one-shot events (Snackbar)
- [ ] No direct DAO/Retrofit calls — only UseCase invocations
- [ ] `SavedStateHandle` for arguments surviving process death
- [ ] `Mutex.tryLock()` for duplicate call prevention

## Offline-First Pattern (RemoteMediator)

Use this pattern only for HomeFragment tabs (NowPlaying, Popular):

```
Network available:  API → RemoteMediator → Room → PagingSource → UI
Network absent:     Room cache → PagingSource → UI (1h expiry via RemoteKeyEntity.lastUpdated)
```

**Rules:**
- [ ] Room DB is Single Source of Truth — never emit API response directly to UI
- [ ] `RemoteKeyEntity.lastUpdated` checked before REFRESH (skip if < 1h old)
- [ ] `PREPEND` always returns `MediatorResult.Success(endOfPaginationReached = true)`
- [ ] DB writes inside `withTransaction` (or `@Transaction` on DAO abstract class)

Search uses network-only `PagingSource` (no Room cache) — this is intentional.

## This Project's Patterns

### UI State Observation
```kotlin
// CORRECT: single repeatOnLifecycle block, parallel launches
viewLifecycleOwner.lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch { viewModel.uiState.collect { ... } }
        launch { viewModel.snackbarEvent.collect { ... } }
    }
}
```

### Error Handling
```kotlin
// Use launchWithErrorHandler from core/util/CoroutineExt.kt
viewModelScope.launchWithErrorHandler(snackbarChannel) {
    _uiState.value = UseCase()
}
```

### Navigation
```kotlin
// Safe Args always — no manual bundle.putInt
val action = FragmentDirections.actionToDetail(movieId = id)
findNavController().navigate(action)
```

## Output Format

```
## Architecture Review: [Target]

### Violations Found
- **[CRITICAL/WARNING]** (file:line): Rule violated → Correct approach

### Compliance Summary
| Layer | Status | Notes |
|-------|--------|-------|
| Presentation | ✅/⚠️/❌ | ... |
| Domain | ✅/⚠️/❌ | ... |
| Data | ✅/⚠️/❌ | ... |

### Offline-First Assessment
[Is the feature properly cache-backed or correctly network-only?]

### Recommended Changes
[Concrete file edits with before/after code snippets]
```
