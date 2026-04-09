---
name: gradle-build-performance
description: Optimize Gradle build speed with Configuration Cache, parallel execution, and AGP 9 tuning
user-invocable: true
disable-model-invocation: false
allowed-tools: Read, Grep, Glob, Bash
argument-hint: [slow task name or "full audit"]
---

# Gradle Build Performance

Analyze and optimize build performance for `$ARGUMENTS` (or run a full audit if no argument given).

## Quick Wins (check these first)

### 1. Configuration Cache
```bash
# Check current status
./gradlew assembleDebug --configuration-cache

# Enable permanently in gradle.properties
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=warn  # start with warn, move to fail later
```
**Benefit**: Skips configuration phase on subsequent builds (30-60% faster incremental builds).
**Gotcha**: Some plugins don't support it yet — check output for incompatibilities.

### 2. Parallel Execution
```properties
# gradle.properties
org.gradle.parallel=true
org.gradle.workers.max=4  # adjust to CPU core count - 1
```

### 3. Daemon & Memory
```properties
# gradle.properties
org.gradle.daemon=true
org.gradle.jvmargs=-Xmx4g -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
```

### 4. Build Cache
```properties
org.gradle.caching=true
```

## AGP 9 Specific

```properties
# Already required for this project — verify present
android.disallowKotlinSourceSets=false
android.enableR8.fullMode=true

# AGP 9 non-transitive R classes (faster builds, less R class bloat)
android.nonTransitiveRClass=true

# Disable unused features
android.defaults.buildfeatures.buildconfig=false  # only enable per-module if needed
android.defaults.buildfeatures.aidl=false
android.defaults.buildfeatures.renderscript=false
```

## KSP vs KAPT

This project uses KSP (already). Verify no KAPT stragglers:
```bash
# Should return nothing
grep -r "kapt" app/build.gradle.kts
```
KSP is ~2x faster than KAPT. Never mix both for the same processor.

## Profile a Slow Build

```bash
# Generate build scan (detailed task timeline)
./gradlew assembleDebug --scan

# Or local profile report
./gradlew assembleDebug --profile
# Report: build/reports/profile/

# Check which tasks are slowest
./gradlew assembleDebug --info 2>&1 | grep "Task.*took"
```

## Common Slow Tasks & Fixes

| Slow Task | Cause | Fix |
|-----------|-------|-----|
| `kspDebugKotlin` | Large annotation processing | Split into modules (future) |
| `testDebugUnitTest` | All tests always re-run | `--tests` filter for local dev |
| `detekt` | Full source scan | Run only on changed files (CI) |
| `jacocoTestReport` | Instrument + report | Run only on CI, not local |
| `lint` | Full lint pass | `./gradlew lintDebug` only when needed |
| `packageDebug` | Large assets | Check for accidentally committed large files |

## Incremental Build Health

```bash
# Run twice — second should be UP-TO-DATE for most tasks
./gradlew assembleDebug
./gradlew assembleDebug

# If tasks re-run on second build, they're not incremental
# Common culprit: task reads System.currentTimeMillis() or non-deterministic input
```

## Module-Level Optimization (future)

If build time exceeds 3 min, consider splitting:
```
:core        (pure Kotlin — fastest to compile)
:data        (Room, Retrofit)
:domain      (pure Kotlin UseCases)
:app         (presentation only)
```
Benefit: only changed modules recompile. Currently single-module is fine for this project size.

## Detekt Performance

```bash
# Run only on app module source, not test sources
./gradlew :app:detekt

# Already configured in this project — verify source.setFrom() in build.gradle.kts
```

## Pre-push Hook Optimization

Current hook runs full `testDebugUnitTest`. If it's too slow:
```bash
# Option: run only affected module tests
./gradlew :app:testDebugUnitTest --daemon --quiet

# Option: parallel test execution (already default in AGP 9)
# Verify in build.gradle.kts:
testOptions {
    unitTests.isParallelized = true  // add if not present
}
```

## Audit Output Format

```
## Build Performance Audit

### Current Baseline
- Clean build: Xs
- Incremental build: Xs
- Test only: Xs

### Configuration Cache Status
[Enabled/Disabled + any incompatible plugins]

### gradle.properties Gaps
[Missing optimizations vs recommended settings]

### Top 5 Slowest Tasks
[Task name → time → fix recommendation]

### Quick Win Priority
1. [Highest impact, lowest effort]
2. ...

### Estimated Improvement
[X% faster incremental builds after applying recommendations]
```
