package ucne.edu.notablelists.presentation.users

sealed interface UserNavigation

sealed interface UserEvent {
    data object SwitchToLoginMode : UserEvent
    data object SwitchToRegisterMode : UserEvent
    data object SubmitAuth : UserEvent
    data object AuthFooterClicked : UserEvent
    data object Logout : UserEvent
    data object ClearError : UserEvent
    data object ClearSuccess : UserEvent
    data class UserNameChanged(val value: String) : UserEvent
    data class PasswordChanged(val value: String) : UserEvent
    data object ShowSkipDialog : UserEvent
    data object DismissSkipDialog : UserEvent
    data object NavigationHandled : UserEvent
    data object ToProfile : UserNavigation
    data object ToLogin : UserNavigation
    data object ToRegister : UserNavigation
}