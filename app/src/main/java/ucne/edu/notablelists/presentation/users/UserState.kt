package ucne.edu.notablelists.presentation.users

import ucne.edu.notablelists.domain.users.model.User

data class UserState(
    val users: List<User> = emptyList(),
    val username: String = "",
    val password: String = "",
    val currentUser: String = "",
    val currentUserId: Int? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val showSkipDialog: Boolean = false,
    val isSessionChecked: Boolean = false,
    val navigationTarget: UserNavigation? = null,
    val isLoginMode: Boolean = true,
    val authTitlePrefix: String = "",
    val authTitleAction: String = "",
    val authSubtitle: String = "",
    val authButtonText: String = "",
    val authFooterQuestion: String = "",
    val authFooterAction: String = "",
    val authSkipDialogText: String = ""
)