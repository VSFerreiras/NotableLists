package ucne.edu.notablelists

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import ucne.edu.notablelists.data.mappers.toDomain
import ucne.edu.notablelists.data.remote.AuthApiService
import ucne.edu.notablelists.data.remote.Resource
import ucne.edu.notablelists.data.remote.dto.AuthResponseDto
import ucne.edu.notablelists.data.remote.dto.UserRequestDto
import ucne.edu.notablelists.data.remote.dto.UserResponseDto
import ucne.edu.notablelists.data.repository.AuthRepositoryImpl

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImplTest {

    private lateinit var authRepository: AuthRepositoryImpl
    private lateinit var authApi: AuthApiService

    @Before
    fun setup() {
        authApi = mockk()
        authRepository = AuthRepositoryImpl(authApi)
    }

    @Test
    fun `login should return success when API returns successful response`() = runTest {

        val username = "testuser"
        val password = "password123"
        val userResponse = UserResponseDto(
            userId = 1,
            username = username,
            password = "hashed_password_from_server"
        )
        val authResponse = AuthResponseDto(
            success = true,
            message = "Login successful",
            user = userResponse
        )

        coEvery {
            authApi.login(UserRequestDto(username, password))
        } returns authResponse

        val result = authRepository.login(username, password)

        assertTrue(result is Resource.Success)
        val user = (result as Resource.Success).data

        assertEquals("1", user?.id)
        assertEquals(1, user?.remoteId)
        assertEquals(username, user?.username)
        assertEquals("hashed_password_from_server", user?.password)
    }

    @Test
    fun `login should return error when API returns unsuccessful response`() = runTest {

        val username = "testuser"
        val password = "wrongpassword"
        val authResponse = AuthResponseDto(
            success = false,
            message = "Invalid credentials",
            user = null
        )

        coEvery {
            authApi.login(UserRequestDto(username, password))
        } returns authResponse

        val result = authRepository.login(username, password)

        assertTrue(result is Resource.Error)
        assertEquals("Invalid credentials", (result as Resource.Error).message)
    }

    @Test
    fun `login should return error when API throws exception`() = runTest {

        val username = "testuser"
        val password = "password123"

        coEvery {
            authApi.login(UserRequestDto(username, password))
        } throws RuntimeException("Network error")

        val result = authRepository.login(username, password)

        assertTrue(result is Resource.Error)
        assertEquals("Network error", (result as Resource.Error).message)
    }

    @Test
    fun `login should return error when API returns success but user is null`() = runTest {
        // Given
        val username = "testuser"
        val password = "password123"
        val authResponse = AuthResponseDto(
            success = true,
            message = "Login successful",
            user = null
        )

        coEvery {
            authApi.login(UserRequestDto(username, password))
        } returns authResponse

        val result = authRepository.login(username, password)

        assertTrue(result is Resource.Error)
        assertEquals("Login successful", (result as Resource.Error).message)
    }

    @Test
    fun `register should return success when API returns successful response`() = runTest {

        val username = "newuser"
        val password = "password123"
        val userResponse = UserResponseDto(
            userId = 2,
            username = username,
            password = "hashed_password"
        )
        val authResponse = AuthResponseDto(
            success = true,
            message = "Registration successful",
            user = userResponse
        )

        coEvery {
            authApi.register(UserRequestDto(username, password))
        } returns authResponse

        val result = authRepository.register(username, password)

        assertTrue(result is Resource.Success)
        val user = (result as Resource.Success).data
        assertEquals("2", user?.id)
        assertEquals(2, user?.remoteId)
        assertEquals(username, user?.username)
        assertEquals("hashed_password", user?.password)
    }

    @Test
    fun `register should return error when API returns unsuccessful response`() = runTest {

        val username = "existinguser"
        val password = "password123"
        val authResponse = AuthResponseDto(
            success = false,
            message = "Username already exists",
            user = null
        )

        coEvery {
            authApi.register(UserRequestDto(username, password))
        } returns authResponse

        val result = authRepository.register(username, password)

        assertTrue(result is Resource.Error)
        assertEquals("Username already exists", (result as Resource.Error).message)
    }

    @Test
    fun `logout should always return success`() = runTest {

        val result = authRepository.logout()

        assertTrue(result is Resource.Success)
    }
}