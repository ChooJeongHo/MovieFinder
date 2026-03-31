---
name: android-code-review
description: Review Android Kotlin code for architecture, performance, correctness, and best practices
user-invocable: true
disable-model-invocation: false
allowed-tools: Read, Grep, Glob, Agent
argument-hint: [file-path]
---

# Android Code Review

Review the Android Kotlin file at `$ARGUMENTS`.

## Review Checklist

Analyze the code across these 6 categories and assign a grade (A/B/C/D) to each:

### 1. Architecture & Clean Architecture Compliance
- Layer dependency rules (Presentation → Domain only, no Data layer references)
- Single Responsibility Principle
- Proper use of UseCase/Repository pattern
- ViewModel state management (StateFlow, sealed class UI state)

### 2. Coroutine & Concurrency Safety
- Proper scope usage (viewModelScope, lifecycleScope)
- CancellationException rethrow
- Thread safety (Mutex, atomic operations)
- Flow collection patterns (stateIn, WhileSubscribed)
- Potential race conditions

### 3. Memory & Resource Management
- ViewBinding null cleanup in onDestroyView
- Flow collection with repeatOnLifecycle
- Image loading cancellation (Coil dispose)
- Listener/callback leak risks
- Dialog lifecycle management

### 4. Error Handling
- Exception classification (ErrorType mapping)
- Partial failure handling (graceful degradation)
- User-facing error feedback (Snackbar, error UI)
- CancellationException preservation

### 5. Android Best Practices
- SavedStateHandle for process death survival
- Configuration change handling
- Navigation Safe Args usage
- Accessibility (contentDescription)
- Resource usage (@string, @dimen instead of hardcoded values)

### 6. Code Quality
- Naming conventions (Kotlin style)
- Code duplication (DRY)
- Function length and complexity
- Import organization
- Unnecessary code or over-engineering

## Output Format

```
## Code Review: [FileName]

### Summary
[1-2 sentence overall assessment]

### Category Grades
| Category | Grade | Notes |
|----------|-------|-------|
| Architecture | A/B/C/D | ... |
| Coroutine Safety | A/B/C/D | ... |
| Memory Management | A/B/C/D | ... |
| Error Handling | A/B/C/D | ... |
| Android Best Practices | A/B/C/D | ... |
| Code Quality | A/B/C/D | ... |

### Issues Found
[List each issue with severity: Critical / Warning / Info]
- **[Severity]** (line N): Description + suggested fix

### Good Patterns
[List well-implemented patterns worth noting]

### Suggested Improvements
[Actionable improvements with code examples if applicable]

### Overall Grade: [A/B/C/D]
```

When reviewing, also check related files (UI state class, Fragment, UseCase) if needed to understand the full context. Use Grep/Glob to find related files.