package ucne.edu.notablelists.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ucne.edu.notablelists.navigation.Screen
import ucne.edu.notablelists.presentation.Notes.edit.NoteEditScreen
import ucne.edu.notablelists.presentation.friends.FriendsScreen
import ucne.edu.notablelists.presentation.notes_list.NotesListRoute
import ucne.edu.notablelists.presentation.users.AuthScreen
import ucne.edu.notablelists.presentation.users.UserEvent
import ucne.edu.notablelists.presentation.users.UserViewModel

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val userViewModel: UserViewModel = hiltViewModel()
    val state by userViewModel.state.collectAsStateWithLifecycle()

    if (!state.isSessionChecked) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        val startDestination = if (state.currentUser.isNotBlank()) {
            Screen.Notes.route
        } else {
            Screen.Login.route
        }

        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable(Screen.Notes.route) {
                NotesListRoute(
                    userViewModel = userViewModel,
                    onNavigateToDetail = { noteId ->
                        navController.navigate(Screen.NoteEdit.passId(noteId))
                    },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0)
                        }
                    },
                    onNavigateToFriends = {
                        navController.navigate(Screen.Friends.route)
                    }
                )
            }

            composable(
                route = Screen.NoteEdit.route,
                arguments = listOf(
                    navArgument("noteId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {
                NoteEditScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route)
                    },
                    onNavigateToFriends = {
                        navController.navigate(Screen.Friends.route)
                    }
                )
            }

            composable(Screen.Friends.route) {
                FriendsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Login.route) {
                LaunchedEffect(Unit) {
                    userViewModel.onEvent(UserEvent.SwitchToLoginMode)
                }
                AuthScreen(
                    onNavigateToLogin = { },
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                    onNavigateToProfile = {
                        navController.navigate(Screen.Notes.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    viewModel = userViewModel
                )
            }

            composable(Screen.Register.route) {
                LaunchedEffect(Unit) {
                    userViewModel.onEvent(UserEvent.SwitchToRegisterMode)
                }
                AuthScreen(
                    onNavigateToLogin = { navController.popBackStack() },
                    onNavigateToRegister = { },
                    onNavigateToProfile = {
                        navController.navigate(Screen.Notes.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    viewModel = userViewModel
                )
            }
        }
    }
}