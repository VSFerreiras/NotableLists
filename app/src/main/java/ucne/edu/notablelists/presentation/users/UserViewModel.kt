package ucne.edu.notablelists.presentation.users

import ucne.edu.notablelists.domain.session.usecase.GetUserIdUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ucne.edu.notablelists.data.remote.Resource
import ucne.edu.notablelists.domain.auth.AuthRepository
import ucne.edu.notablelists.domain.notes.repository.NoteRepository
import ucne.edu.notablelists.domain.notes.usecase.ClearLocalNotesUseCase
import ucne.edu.notablelists.domain.session.usecase.ClearSessionUseCase
import ucne.edu.notablelists.domain.session.usecase.GetSessionUseCase
import ucne.edu.notablelists.domain.session.usecase.SaveSessionUseCase
import ucne.edu.notablelists.domain.users.usecase.PostUserUseCase
import ucne.edu.notablelists.domain.users.usecase.ValidateUserUseCase
import ucne.edu.notablelists.domain.utils.usecase.LoginUserUseCase
import ucne.edu.notablelists.domain.utils.usecase.SyncUserDataUseCase
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val postUserUseCase: PostUserUseCase,
    private val validateUseCase: ValidateUserUseCase,
    private val getSessionUseCase: GetSessionUseCase,
    private val saveSessionUseCase: SaveSessionUseCase,
    private val getUserIdUseCase: GetUserIdUseCase,
    private val clearSessionUseCase: ClearSessionUseCase,
    private val loginUserUseCase: LoginUserUseCase,
    private val clearLocalNotesUseCase: ClearLocalNotesUseCase,
    private val syncUserDataUseCase: SyncUserDataUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(UserState())
    val state: StateFlow<UserState> = _state

    init {
        viewModelScope.launch {
            getSessionUseCase().collect { user ->
                _state.update {
                    it.copy(
                        currentUser = user ?: "",
                        isSessionChecked = true
                    )
                }
            }
        }
    }

    fun onEvent(event: UserEvent) {
        when (event) {
            UserEvent.CreateUser -> createUser()
            UserEvent.LoginUser -> loginUser()
            UserEvent.Logout -> logout()
            UserEvent.ClearError -> clearError()
            UserEvent.ClearSuccess -> clearSuccess()
            is UserEvent.UserNameChanged -> userNameChanged(event.value)
            is UserEvent.PasswordChanged -> passwordChanged(event.value)
            UserEvent.ShowSkipDialog -> _state.update { it.copy(showSkipDialog = true) }
            UserEvent.DismissSkipDialog -> _state.update { it.copy(showSkipDialog = false) }
        }
    }

    private fun userNameChanged(username: String) {
        _state.update {
            it.copy(
                username = username,
                usernameError = null,
                error = null
            )
        }
    }

    fun passwordChanged(password: String) {
        _state.update {
            it.copy(
                password = password,
                passwordError = null,
                error = null
            )
        }
    }

    private fun createUser() {
        val username = _state.value.username
        val password = _state.value.password

        val validation = validateUseCase(username, password)
        if (validation is Resource.Error) {
            setFieldErrors(validation.message)
            return
        }

        _state.update { it.copy(isLoading = true, error = null, usernameError = null, passwordError = null) }

        viewModelScope.launch {
            try {
                val result = postUserUseCase(username, password)
                when (result) {
                    is Resource.Success -> {
                        val newUser = result.data
                        val userId = newUser?.remoteId
                        if (userId != null) {
                            saveSessionUseCase(userId, username)
                            syncUserDataUseCase(userId)
                        }
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isSuccess = true,
                                error = null,
                                usernameError = null,
                                passwordError = null,
                                username = "",
                                password = "",
                                currentUser = username,
                                currentUserId = userId
                            )
                        }
                    }
                    is Resource.Error -> {
                        setFieldErrors(result.message)
                    }
                    is Resource.Loading -> {
                        _state.update { it.copy(isLoading = true) }
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    private fun loginUser() {
        val username = _state.value.username
        val password = _state.value.password

        val validation = validateUseCase(username, password)
        if (validation is Resource.Error) {
            setFieldErrors(validation.message)
            return
        }

        _state.update { it.copy(isLoading = true, error = null, usernameError = null, passwordError = null) }

        viewModelScope.launch {
            try {
                val result = loginUserUseCase(username, password)
                when (result) {
                    is Resource.Success -> {
                        val loggedInUser = result.data
                        val userId = loggedInUser?.remoteId
                        if (userId != null) {
                            saveSessionUseCase(userId, username)
                            syncUserDataUseCase(userId)
                        }
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isSuccess = true,
                                error = null,
                                usernameError = null,
                                passwordError = null,
                                username = "",
                                password = "",
                                currentUser = username,
                                currentUserId = userId
                            )
                        }
                    }
                    is Resource.Error -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                    is Resource.Loading -> {
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    private fun setFieldErrors(errorMessage: String?) {
        errorMessage?.let { message ->
            when {
                message.contains("usuario", ignoreCase = true) -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            usernameError = message,
                            passwordError = null
                        )
                    }
                }
                message.contains("contraseña", ignoreCase = true) -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            usernameError = null,
                            passwordError = message
                        )
                    }
                }
                else -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            usernameError = null,
                            passwordError = null,
                            error = message
                        )
                    }
                }
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            clearSessionUseCase()
            clearLocalNotesUseCase()
            _state.update {
                UserState(
                    username = "",
                    password = "",
                    currentUser = "",
                    currentUserId = null,
                    isSessionChecked = true
                )
            }
        }
    }

    private fun clearError() {
        _state.update { it.copy(error = null, usernameError = null, passwordError = null) }
    }

    private fun clearSuccess() {
        _state.update { it.copy(isSuccess = false) }
    }
}