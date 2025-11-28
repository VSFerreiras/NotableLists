package ucne.edu.notablelists.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import ucne.edu.notablelists.navigation.Screen
import ucne.edu.notablelists.presentation.Notes.edit.NoteEditScreen
import ucne.edu.notablelists.presentation.notes_list.NotesListRoute
import ucne.edu.notablelists.presentation.users.LoginScreen
import ucne.edu.notablelists.presentation.users.ProfileScreen
import ucne.edu.notablelists.presentation.users.RegisterScreen
import ucne.edu.notablelists.presentation.users.UserViewModel

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val userViewModel: UserViewModel = hiltViewModel()

    val bottomItems = listOf(
        BottomNavItem.Notes,
        BottomNavItem.SharedNotes,
        BottomNavItem.Profile
    )

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Screen.Notes.route,
        Screen.SharedNotes.route,
        Screen.Profile.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(item.icon, contentDescription = item.title)
                            },
                            label = { Text(item.title) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Notes.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Notes.route) {
                NotesListRoute(
                    onNavigateToDetail = { noteId ->
                        navController.navigate(Screen.NoteEdit.passId(noteId))
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
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.SharedNotes.route) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Pantalla de Notas Compartidas")
                }
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0)
                        }
                    },
                    viewModel = userViewModel
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
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
                RegisterScreen(
                    onNavigateToLogin = { navController.popBackStack() },
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