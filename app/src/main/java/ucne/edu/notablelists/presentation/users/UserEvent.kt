package ucne.edu.notablelists.presentation.users

sealed class UserEvent {
    object CreateUser : UserEvent()
    object LoginUser : UserEvent()
    object Logout : UserEvent()
    object ClearError : UserEvent()
    object ClearSuccess : UserEvent()
}