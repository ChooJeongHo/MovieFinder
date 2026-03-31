package com.choo.moviefinder.presentation.detail

import app.cash.turbine.test
import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.domain.model.Memo
import com.choo.moviefinder.domain.usecase.DeleteMemoUseCase
import com.choo.moviefinder.domain.usecase.DeleteUserRatingUseCase
import com.choo.moviefinder.domain.usecase.GetMemosUseCase
import com.choo.moviefinder.domain.usecase.GetUserRatingUseCase
import com.choo.moviefinder.domain.usecase.SaveMemoUseCase
import com.choo.moviefinder.domain.usecase.SetUserRatingUseCase
import com.choo.moviefinder.domain.usecase.UpdateMemoUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DelegateTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---- MemoDelegate ----

    @Test
    fun `memos stateFlow collects from use case`() = runTest {
        val memoList = listOf(
            Memo(id = 1L, movieId = 10, content = "Great film", createdAt = 0L, updatedAt = 0L)
        )
        val getMemosUseCase = mockk<GetMemosUseCase>()
        every { getMemosUseCase(10) } returns flowOf(memoList)
        val snackbarChannel = Channel<ErrorType>(Channel.CONFLATED)

        val delegate = MemoDelegate(
            getMemosUseCase = getMemosUseCase,
            saveMemoUseCase = mockk(),
            updateMemoUseCase = mockk<UpdateMemoUseCase>(),
            deleteMemoUseCase = mockk(),
            movieId = 10,
            viewModelScope = backgroundScope,
            snackbarChannel = snackbarChannel
        )

        // stateIn(WhileSubscribed) emits initial value first, then the upstream value
        delegate.memos.test {
            val first = awaitItem()
            if (first.isEmpty()) {
                val second = awaitItem()
                assertEquals(1, second.size)
                assertEquals("Great film", second[0].content)
            } else {
                assertEquals(1, first.size)
                assertEquals("Great film", first[0].content)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saveMemo calls use case`() = runTest {
        val getMemosUseCase = mockk<GetMemosUseCase>()
        every { getMemosUseCase(10) } returns flowOf(emptyList())
        val saveMemoUseCase = mockk<SaveMemoUseCase>()
        coEvery { saveMemoUseCase(10, "My note") } returns Unit
        val snackbarChannel = Channel<ErrorType>(Channel.CONFLATED)
        // Use Main dispatcher scope so advanceUntilIdle() drives it
        val delegateScope = CoroutineScope(Dispatchers.Main)

        val delegate = MemoDelegate(
            getMemosUseCase = getMemosUseCase,
            saveMemoUseCase = saveMemoUseCase,
            updateMemoUseCase = mockk(),
            deleteMemoUseCase = mockk(),
            movieId = 10,
            viewModelScope = delegateScope,
            snackbarChannel = snackbarChannel
        )

        delegate.saveMemo("My note")
        advanceUntilIdle()

        coVerify { saveMemoUseCase(10, "My note") }
    }

    @Test
    fun `deleteMemo calls use case`() = runTest {
        val getMemosUseCase = mockk<GetMemosUseCase>()
        every { getMemosUseCase(10) } returns flowOf(emptyList())
        val deleteMemoUseCase = mockk<DeleteMemoUseCase>()
        coEvery { deleteMemoUseCase(99L) } returns Unit
        val snackbarChannel = Channel<ErrorType>(Channel.CONFLATED)
        val delegateScope = CoroutineScope(Dispatchers.Main)

        val delegate = MemoDelegate(
            getMemosUseCase = getMemosUseCase,
            saveMemoUseCase = mockk(),
            updateMemoUseCase = mockk(),
            deleteMemoUseCase = deleteMemoUseCase,
            movieId = 10,
            viewModelScope = delegateScope,
            snackbarChannel = snackbarChannel
        )

        delegate.deleteMemo(99L)
        advanceUntilIdle()

        coVerify { deleteMemoUseCase(99L) }
    }

    @Test
    fun `saveMemo sends snackbar on error`() = runTest {
        val getMemosUseCase = mockk<GetMemosUseCase>()
        every { getMemosUseCase(10) } returns flowOf(emptyList())
        val saveMemoUseCase = mockk<SaveMemoUseCase>()
        coEvery { saveMemoUseCase(any(), any()) } throws RuntimeException("DB error")
        val snackbarChannel = Channel<ErrorType>(Channel.CONFLATED)
        val delegateScope = CoroutineScope(Dispatchers.Main)

        val delegate = MemoDelegate(
            getMemosUseCase = getMemosUseCase,
            saveMemoUseCase = saveMemoUseCase,
            updateMemoUseCase = mockk(),
            deleteMemoUseCase = mockk(),
            movieId = 10,
            viewModelScope = delegateScope,
            snackbarChannel = snackbarChannel
        )

        delegate.saveMemo("Note")
        advanceUntilIdle()

        snackbarChannel.receiveAsFlow().test {
            assertEquals(ErrorType.UNKNOWN, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ---- UserRatingDelegate ----

    @Test
    fun `userRating stateFlow collects from use case`() = runTest {
        val getUserRatingUseCase = mockk<GetUserRatingUseCase>()
        every { getUserRatingUseCase(10) } returns flowOf(3.5f)
        val snackbarChannel = Channel<ErrorType>(Channel.CONFLATED)

        val delegate = UserRatingDelegate(
            getUserRatingUseCase = getUserRatingUseCase,
            setUserRatingUseCase = mockk(),
            deleteUserRatingUseCase = mockk(),
            movieId = 10,
            viewModelScope = backgroundScope,
            snackbarChannel = snackbarChannel
        )

        // stateIn(WhileSubscribed) emits initial value (null) first, then upstream
        delegate.userRating.test {
            val first = awaitItem()
            if (first == null) {
                assertEquals(3.5f, awaitItem())
            } else {
                assertEquals(3.5f, first)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setUserRating calls use case`() = runTest {
        val getUserRatingUseCase = mockk<GetUserRatingUseCase>()
        every { getUserRatingUseCase(10) } returns flowOf(null)
        val setUserRatingUseCase = mockk<SetUserRatingUseCase>()
        coEvery { setUserRatingUseCase(10, 4.0f) } returns Unit
        val snackbarChannel = Channel<ErrorType>(Channel.CONFLATED)
        val delegateScope = CoroutineScope(Dispatchers.Main)

        val delegate = UserRatingDelegate(
            getUserRatingUseCase = getUserRatingUseCase,
            setUserRatingUseCase = setUserRatingUseCase,
            deleteUserRatingUseCase = mockk(),
            movieId = 10,
            viewModelScope = delegateScope,
            snackbarChannel = snackbarChannel
        )

        delegate.setUserRating(4.0f)
        advanceUntilIdle()

        coVerify { setUserRatingUseCase(10, 4.0f) }
    }

    @Test
    fun `deleteUserRating calls use case`() = runTest {
        val getUserRatingUseCase = mockk<GetUserRatingUseCase>()
        every { getUserRatingUseCase(10) } returns flowOf(null)
        val deleteUserRatingUseCase = mockk<DeleteUserRatingUseCase>()
        coEvery { deleteUserRatingUseCase(10) } returns Unit
        val snackbarChannel = Channel<ErrorType>(Channel.CONFLATED)
        val delegateScope = CoroutineScope(Dispatchers.Main)

        val delegate = UserRatingDelegate(
            getUserRatingUseCase = getUserRatingUseCase,
            setUserRatingUseCase = mockk(),
            deleteUserRatingUseCase = deleteUserRatingUseCase,
            movieId = 10,
            viewModelScope = delegateScope,
            snackbarChannel = snackbarChannel
        )

        delegate.deleteUserRating()
        advanceUntilIdle()

        coVerify { deleteUserRatingUseCase(10) }
    }

    @Test
    fun `setUserRating sends snackbar on error`() = runTest {
        val getUserRatingUseCase = mockk<GetUserRatingUseCase>()
        every { getUserRatingUseCase(10) } returns flowOf(null)
        val setUserRatingUseCase = mockk<SetUserRatingUseCase>()
        coEvery { setUserRatingUseCase(any(), any()) } throws RuntimeException("DB error")
        val snackbarChannel = Channel<ErrorType>(Channel.CONFLATED)
        val delegateScope = CoroutineScope(Dispatchers.Main)

        val delegate = UserRatingDelegate(
            getUserRatingUseCase = getUserRatingUseCase,
            setUserRatingUseCase = setUserRatingUseCase,
            deleteUserRatingUseCase = mockk(),
            movieId = 10,
            viewModelScope = delegateScope,
            snackbarChannel = snackbarChannel
        )

        delegate.setUserRating(3.0f)
        advanceUntilIdle()

        snackbarChannel.receiveAsFlow().test {
            assertEquals(ErrorType.UNKNOWN, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
