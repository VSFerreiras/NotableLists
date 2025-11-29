package ucne.edu.notablelists.presentation.users

import ucne.edu.notablelists.domain.users.model.User

data class UserState(
    val users: List<User> = emptyList(),
    val username: String = "",
    val password: String = "",
    val currentUser: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val showSkipDialog: Boolean = false,
    val isSessionChecked: Boolean = false
)