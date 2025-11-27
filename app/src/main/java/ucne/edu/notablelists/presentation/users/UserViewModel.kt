package ucne.edu.notablelists.presentation.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ucne.edu.notablelists.domain.users.usecase.PostUserUseCase
import ucne.edu.notablelists.domain.users.usecase.ValidateUserUseCase
import javax.inject.Inject
import ucne.edu.notablelists.data.remote.Resource

@HiltViewModel
class UserViewModel @Inject constructor(
    private val postUserUseCase: PostUserUseCase,
    private val validateUseCase: ValidateUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(UserState())
    val state: StateFlow<UserState> = _state

    fun onEvent(event: UserEvent) {
        when (event) {
            UserEvent.CreateUser -> createUser()
            UserEvent.LoginUser -> loginUser()
            UserEvent.Logout -> logout()
            UserEvent.ClearError -> clearError()
            UserEvent.ClearSuccess -> clearSuccess()
            is UserEvent.UserNameChanged -> userNameChanged(event.value)
            is UserEvent.PasswordChanged -> passwordChanged(event.value)
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
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isSuccess = true,
                                error = null,
                                usernameError = null,
                                passwordError = null,
                                username = "",
                                password = "",
                                currentUser = username
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
                _state.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        error = null,
                        usernameError = null,
                        passwordError = null,
                        username = "",
                        password = "",
                        currentUser = username
                    )
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
        _state.update {
            UserState(
                username = "",
                password = "",
                currentUser = ""
            )
        }
    }

    private fun clearError() {
        _state.update { it.copy(error = null, usernameError = null, passwordError = null) }
    }

    private fun clearSuccess() {
        _state.update { it.copy(isSuccess = false) }
    }
}
