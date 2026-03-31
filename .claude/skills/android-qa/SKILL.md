---
name: android-qa
description: Run MovieFinder project QA pipeline - detekt, unit tests, jacoco coverage report
user-invocable: true
disable-model-invocation: false
allowed-tools: Bash, Read, Glob, Grep
argument-hint: [detekt|test|coverage|all]
---

# MovieFinder QA Pipeline

Run QA checks for the MovieFinder Android project based on the argument:

## Commands by argument

### `detekt` - Static analysis only
```bash
./gradlew :app:detekt
```
Report detekt issues found. If clean, confirm no issues.

### `test` - Unit tests only
```bash
./gradlew testDebugUnitTest
```
Parse test results from `app/build/reports/tests/testDebugUnitTest/index.html`.
Report total/passed/failed counts. If any test fails, read the failure details and suggest fixes.

### `coverage` - JaCoCo coverage report
```bash
./gradlew jacocoTestReport
```
Read coverage summary from `app/build/reports/jacoco/jacocoTestReport/html/index.html`.
Report line/branch/method coverage percentages.

### `all` (default if no argument) - Full pipeline
Run in order: detekt → unit tests → jacoco coverage.
Stop on first failure and report the issue.

## Output format

Summarize results in a table:

| Check | Status | Details |
|-------|--------|---------|
| Detekt | PASS/FAIL | N issues found |
| Unit Tests | PASS/FAIL | N/201 passed |
| Coverage | PASS/FAIL | Line: N%, Branch: N% |

If any step fails, provide actionable fix suggestions referencing specific files and line numbers.