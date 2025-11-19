package ucne.edu.notablelists.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Profile : Screen("profile")
    object Notes : Screen("notes")
    object SharedNotes : Screen("shared_notes")
}
