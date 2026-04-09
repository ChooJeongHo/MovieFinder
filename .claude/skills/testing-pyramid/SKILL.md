---
name: testing-pyramid
description: Write and fix Unit, Hilt integration, and Espresso tests following MovieFinder patterns
user-invocable: true
disable-model-invocation: false
allowed-tools: Read, Grep, Glob, Bash, Agent
argument-hint: [class-name or feature to test]
---

# Testing Pyramid

Write or fix tests for `$ARGUMENTS` following this project's exact test patterns.

## Layer 1: Unit Tests (app/src/test/)

### Setup Template
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class FooViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        // init mocks
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
```

### What to test per layer

**ViewModel**
- `UiState` transitions: Loading → Success → Error
- UseCase delegation (verify correct UseCase called with correct args)
- `SavedStateHandle` argument survival
- Snackbar/event emission via `Channel.CONFLATED`
- Mutex dedup: second call blocked while first in progress
- FAB toggle: optimistic update + DB sync

```kotlin
@Test
fun `load detail - success emits Success state`() = runTest {
    coEvery { getMovieDetailUseCase(123) } returns Result.success(fakeMovie)
    viewModel.loadDetail(123)
    testDispatcher.advanceUntilIdle()
    assertIs<DetailUiState.Success>(viewModel.uiState.value)
}
```

**UseCase**
- Delegation only: verify repository method called once with correct params
- No business logic testing here (it's in Repository or ViewModel)

```kotlin
@Test
fun `invoke delegates to repository`() = runTest {
    useCase(123)
    coVerify(exactly = 1) { repository.getMovie(123) }
}
```

**Repository Implementation**
- API success → correct domain model returned
- API failure → exception propagated
- DB insert → query returns inserted value
- Mapping: DTO fields → domain model fields (check nullability)

**PagingSource**
- `LoadResult.Page` with correct `nextKey`/`prevKey`
- Last page: `nextKey = null`
- API error: `LoadResult.Error`

```kotlin
@Test
fun `load returns page with nextKey`() = runTest {
    val result = pagingSource.load(
        PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false)
    )
    val page = assertIs<PagingSource.LoadResult.Page<Int, Movie>>(result)
    assertEquals(2, page.nextKey)
}
```

**RemoteMediator**
- `REFRESH` with expired cache → calls API, writes to DB
- `REFRESH` with fresh cache → skips API (`Success(endOfPaginationReached = false)`)
- `APPEND` with no more pages → `Success(endOfPaginationReached = true)`

**Notification / Helper**
- Goal = 0 or negative → early return, no DB call
- Goal not reached → no notification
- Goal reached, not yet notified this month → saves month + notifies
- Goal reached, already notified this month → skip

### Mock Patterns
```kotlin
// suspend function
coEvery { repository.getMovie(any()) } returns fakeMovie
coVerify(exactly = 1) { repository.getMovie(123) }

// Flow
every { repository.getFavorites() } returns flowOf(listOf(fakeMovie))

// void suspend
coEvery { repository.save(any()) } returns Unit

// ExperimentalTime opt-in if using Clock/Instant
@file:OptIn(kotlin.time.ExperimentalTime::class)
```

### Flow / Turbine
```kotlin
@Test
fun `favorites flow emits list`() = runTest {
    every { repository.getFavorites() } returns flowOf(listOf(fakeMovie))
    viewModel.favorites.test {
        assertEquals(listOf(fakeMovie), awaitItem())
        cancelAndIgnoreRemainingEvents()
    }
}
```

### testOptions setting
```kotlin
// build.gradle.kts — already set in this project:
testOptions {
    unitTests.isReturnDefaultValues = true  // Android API stubs return default values
}
```

## Layer 2: Hilt Integration Tests (app/src/androidTest/ — unit scope)

Used for: Repository with real Room DB, DataStore reads.

```kotlin
@HiltAndroidTest
class FavoriteRepositoryTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var repository: FavoriteRepository
    @Inject lateinit var db: MovieDatabase

    @Before
    fun setup() { hiltRule.inject() }

    @After
    fun tearDown() { db.close() }
}
```

**Key rules:**
- `testInstrumentationRunner = "com.choo.moviefinder.HiltTestRunner"` (already configured)
- `@HiltAndroidTest` + `HiltAndroidRule` on every test class
- Use in-memory Room: `Room.inMemoryDatabaseBuilder()`
- `kspAndroidTest(libs.hilt.compiler)` already in build.gradle.kts

## Layer 3: Espresso UI Tests (app/src/androidTest/)

```kotlin
@HiltAndroidTest
class MainActivityTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun bottomNav_clickSearch_navigatesToSearchFragment() {
        onView(withId(R.id.navigation_search)).perform(click())
        onView(withId(R.id.searchFragment)).check(matches(isDisplayed()))
    }
}
```

**Rules:**
- Only test navigation and screen-level smoke checks
- No business logic in Espresso tests
- Run with: `./gradlew connectedDebugAndroidTest`

## What NOT to test

- `@Binds` DI wiring (Hilt compile-time checked)
- Simple data class constructors
- Mapping functions that are just field assignments (covered by higher-level test)
- Android framework internals (View, Context, etc.)

## Common Failures & Fixes

| Failure | Cause | Fix |
|---------|-------|-----|
| `UninitializedPropertyAccessException` | Mock not set in `@Before` | Add `mockk()` init |
| `MockKException: no answer found` | suspend fun not stubbed with `coEvery` | Change `every` → `coEvery` |
| `ClassCastException` on `withTransaction` | Room transaction mocking limitation | Use `@Transaction` on DAO abstract class instead |
| Test passes locally, fails on CI | `Dispatchers.Main` not reset | Add `Dispatchers.resetMain()` in `@After` |
| Flow never emits in test | Missing `testDispatcher.advanceUntilIdle()` | Add after triggering the action |

## Output Format

```
## Test Plan: [Target Class]

### Coverage Target
[Which behaviors need tests? List as: "when X → should Y"]

### Test Cases
[For each test: method name + setup + assertion]

### Mock Setup
[Which dependencies need mocking + what to stub]

### Generated Tests
[Full Kotlin test code]
```
