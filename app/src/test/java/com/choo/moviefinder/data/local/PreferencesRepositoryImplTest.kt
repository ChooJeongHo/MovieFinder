package com.choo.moviefinder.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import app.cash.turbine.test
import com.choo.moviefinder.domain.model.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesRepositoryImplTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var dataStore: DataStore<UserSettings>
    private lateinit var repository: PreferencesRepositoryImpl

    @Before
    fun setup() {
        dataStore = DataStoreFactory.create(
            serializer = UserSettingsSerializer,
            scope = testScope,
            produceFile = { tmpFolder.newFile("test_user_settings.json") }
        )
        repository = PreferencesRepositoryImpl(dataStore)
    }

    @Test
    fun `getThemeMode returns SYSTEM as default`() = testScope.runTest {
        repository.getThemeMode().test {
            assertEquals(ThemeMode.SYSTEM, awaitItem())
        }
    }

    @Test
    fun `setThemeMode persists DARK mode`() = testScope.runTest {
        repository.setThemeMode(ThemeMode.DARK)

        repository.getThemeMode().test {
            assertEquals(ThemeMode.DARK, awaitItem())
        }
    }

    @Test
    fun `setThemeMode persists LIGHT mode`() = testScope.runTest {
        repository.setThemeMode(ThemeMode.LIGHT)

        repository.getThemeMode().test {
            assertEquals(ThemeMode.LIGHT, awaitItem())
        }
    }

    @Test
    fun `setThemeMode can change from DARK to SYSTEM`() = testScope.runTest {
        repository.setThemeMode(ThemeMode.DARK)
        repository.setThemeMode(ThemeMode.SYSTEM)

        repository.getThemeMode().test {
            assertEquals(ThemeMode.SYSTEM, awaitItem())
        }
    }

    @Test
    fun `getMonthlyWatchGoal returns 0 as default`() = testScope.runTest {
        repository.getMonthlyWatchGoal().test {
            assertEquals(0, awaitItem())
        }
    }

    @Test
    fun `setMonthlyWatchGoal persists goal value`() = testScope.runTest {
        repository.setMonthlyWatchGoal(10)

        repository.getMonthlyWatchGoal().test {
            assertEquals(10, awaitItem())
        }
    }

    @Test
    fun `setMonthlyWatchGoal can change goal value`() = testScope.runTest {
        repository.setMonthlyWatchGoal(10)
        repository.setMonthlyWatchGoal(20)

        repository.getMonthlyWatchGoal().test {
            assertEquals(20, awaitItem())
        }
    }

    @Test
    fun `getLastGoalNotifiedMonth returns empty string as default`() = testScope.runTest {
        repository.getLastGoalNotifiedMonth().test {
            assertEquals("", awaitItem())
        }
    }

    @Test
    fun `setLastGoalNotifiedMonth persists year-month`() = testScope.runTest {
        repository.setLastGoalNotifiedMonth("2026-03")

        repository.getLastGoalNotifiedMonth().test {
            assertEquals("2026-03", awaitItem())
        }
    }
}
