package com.choo.moviefinder.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
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

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: PreferencesRepositoryImpl

    @Before
    fun setup() {
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("test_settings.preferences_pb") }
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
}