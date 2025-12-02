package ucne.edu.notablelists.presentation.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ucne.edu.notablelists.data.remote.Resource
import ucne.edu.notablelists.domain.notes.usecase.ClearLocalNotesUseCase
import ucne.edu.notablelists.domain.session.usecase.ClearSessionUseCase
import ucne.edu.notablelists.domain.session.usecase.GetSessionUseCase
import ucne.edu.notablelists.domain.session.usecase.SaveSessionUseCase
import ucne.edu.notablelists.domain.users.usecase.PostUserUseCase
import ucne.edu.notablelists.domain.users.usecase.ValidateUserUseCase
import ucne.edu.notablelists.domain.utils.usecase.LoginUserUseCase
import ucne.edu.notablelists.domain.utils.usecase.SyncUserDataUseCase
import javax.inject.Inject

sealed interface UserSideEffect {
    data object NavigateToProfile : UserSideEffect
    data class ShowError(val message: String) : UserSideEffect
}

@HiltViewModel
class UserViewModel @Inject constructor(
    private val postUserUseCase: PostUserUseCase,
    private val validateUseCase: ValidateUserUseCase,
    private val getSessionUseCase: GetSessionUseCase,
    private val saveSessionUseCase: SaveSessionUseCase,
    private val clearSessionUseCase: ClearSessionUseCase,
    private val loginUserUseCase: LoginUserUseCase,
    private val clearLocalNotesUseCase: ClearLocalNotesUseCase,
    private val syncUserDataUseCase: SyncUserDataUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(UserState())
    val state: StateFlow<UserState> = _state

    private val _uiEffect = Channel<UserSideEffect>()
    val uiEffect = _uiEffect.receiveAsFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            getSessionUseCase().collect { user ->
                if (!user.isNullOrBlank()) {
                    _uiEffect.send(UserSideEffect.NavigateToProfile)
                }
                _state.update { it.copy(isSessionChecked = true) }
            }
        }
    }

    fun onEvent(event: UserEvent) {
        when (event) {
            UserEvent.CreateUser -> createUser()
            UserEvent.LoginUser -> loginUser()
            UserEvent.Logout -> logout()
            UserEvent.ClearError -> clearError()
            is UserEvent.UserNameChanged -> userNameChanged(event.value)
            is UserEvent.PasswordChanged -> passwordChanged(event.value)
            UserEvent.ShowSkipDialog -> _state.update { it.copy(showSkipDialog = true) }
            UserEvent.DismissSkipDialog -> _state.update { it.copy(showSkipDialog = false) }
            UserEvent.SkipLogin -> skipLogin()
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

    private fun passwordChanged(password: String) {
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
                when (val result = postUserUseCase(username, password)) {
                    is Resource.Success -> {
                        val newUser = result.data
                        val userId = newUser?.remoteId
                        if (userId != null) {
                            saveSessionUseCase(userId, username)
                            syncUserDataUseCase(userId)
                            _uiEffect.send(UserSideEffect.NavigateToProfile)
                        }
                        resetState()
                    }
                    is Resource.Error -> {
                        setFieldErrors(result.message ?: "Error desconocido")
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
                when (val result = loginUserUseCase(username, password)) {
                    is Resource.Success -> {
                        val loggedInUser = result.data
                        val userId = loggedInUser?.remoteId
                        if (userId != null) {
                            saveSessionUseCase(userId, username)
                            syncUserDataUseCase(userId)
                            _uiEffect.send(UserSideEffect.NavigateToProfile)
                        }
                        resetState()
                    }
                    is Resource.Error -> {
                        val errorMessage = result.message ?: "Error desconocido"
                        if (errorMessage.contains("400") || errorMessage.contains("404") || errorMessage.contains("401")) {
                            setFieldErrors("Usuario o contraseña incorrectos")
                        } else {
                            setFieldErrors(errorMessage)
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
        _state.update { it.copy(isLoading = false) }

        errorMessage?.let { message ->
            when {
                message.contains("usuario", ignoreCase = true) -> {
                    _state.update { it.copy(usernameError = message) }
                }
                message.contains("contraseña", ignoreCase = true) -> {
                    _state.update { it.copy(passwordError = message) }
                }
                message.contains("incorrectos", ignoreCase = true) -> {
                    _state.update { it.copy(error = message) }
                }
                else -> {
                    _state.update { it.copy(error = message) }
                }
            }
        }
    }

    private fun skipLogin() {
        _state.update { it.copy(showSkipDialog = false) }
        viewModelScope.launch {
            _uiEffect.send(UserSideEffect.NavigateToProfile)
        }
    }

    private fun logout() {
        viewModelScope.launch {
            clearSessionUseCase()
            clearLocalNotesUseCase()
            resetState()
        }
    }

    private fun resetState() {
        _state.update {
            UserState()
        }
    }

    private fun clearError() {
        _state.update { it.copy(error = null, usernameError = null, passwordError = null) }
    }
}