package ucne.edu.notablelists.presentation.users

sealed interface UserEvent {
    data object CreateUser : UserEvent
    data object LoginUser : UserEvent
    data object Logout : UserEvent
    data object ClearError : UserEvent
    data object ClearSuccess : UserEvent
    data class UserNameChanged(val value: String) : UserEvent
    data class PasswordChanged(val value: String) : UserEvent
    data object ShowSkipDialog : UserEvent
    data object DismissSkipDialog : UserEvent
}