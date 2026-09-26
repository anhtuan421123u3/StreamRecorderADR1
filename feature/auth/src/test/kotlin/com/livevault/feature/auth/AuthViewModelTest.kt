package com.livevault.feature.auth

import app.cash.turbine.test
import com.livevault.core.common.Result
import com.livevault.core.network.dto.AuthResultDto
import com.livevault.core.network.dto.LoginRequest
import com.livevault.core.network.dto.TokenPairDto
import com.livevault.core.network.dto.UserProfileDto
import com.livevault.core.network.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: AuthViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AuthViewModel(authRepository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login success updates uiState and emits NavigateToHome event`() = runTest {
        val dummyUser = UserProfileDto(
            id = "usr_123",
            email = "test@example.com",
            displayName = "Tester",
            subscriptionTier = "PRO"
        )
        val dummyAuth = AuthResultDto(
            user = dummyUser,
            tokens = TokenPairDto(accessToken = "access_token", refreshToken = "refresh_token")
        )

        coEvery { authRepository.login(any()) } returns Result.Success(dummyAuth)

        viewModel.eventFlow.test {
            viewModel.login("test@example.com", "password123")
            testDispatcher.scheduler.advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is AuthEvent.NavigateToHome)
        }

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(dummyUser, state.user)
        assertNull(state.errorMessage)
    }

    @Test
    fun `login failure updates uiState with error message`() = runTest {
        val errorException = Exception("Invalid email or password")
        coEvery { authRepository.login(any()) } returns Result.Error(errorException)

        viewModel.login("wrong@example.com", "wrongpass")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.user)
        assertEquals("Invalid email or password", state.errorMessage)
    }

    @Test
    fun `blank credentials triggers validation error without repository call`() = runTest {
        viewModel.login("", "")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Please enter email and password", state.errorMessage)
    }
}
