package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.PreferencesRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class CompleteOnboardingUseCaseTest {

    private lateinit var repository: PreferencesRepository
    private lateinit var useCase: CompleteOnboardingUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxUnitFun = true)
        useCase = CompleteOnboardingUseCase(repository)
    }

    @Test
    fun `invoke delegates to repository setOnboardingCompleted`() = runTest {
        useCase()

        coVerify(exactly = 1) { repository.setOnboardingCompleted() }
    }

    @Test
    fun `invoke calls setOnboardingCompleted exactly once`() = runTest {
        useCase()
        useCase()

        coVerify(exactly = 2) { repository.setOnboardingCompleted() }
    }
}
