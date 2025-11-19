package ucne.edu.notablelists.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import ucne.edu.notablelists.presentation.users.LoginScreen
import ucne.edu.notablelists.presentation.users.ProfileScreen
import ucne.edu.notablelists.presentation.users.RegisterScreen
import ucne.edu.notablelists.presentation.users.UserViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import ucne.edu.notablelists.presentation.wip.WipScreen
import ucne.edu.notablelists.navigation.Screen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    val viewModel: UserViewModel = hiltViewModel()

    val bottomItems = listOf(
        BottomNavItem.Notes,
        BottomNavItem.SharedNotes,
        BottomNavItem.Profile
    )

    Scaffold(
        bottomBar = {
            val backStack = navController.currentBackStackEntryAsState().value
            val currentRoute = backStack?.destination?.route

            val showBottom = currentRoute in listOf(
                Screen.Profile.route,
                Screen.Notes.route,
                Screen.SharedNotes.route
            )

            if (showBottom) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    launchSingleTop = true
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
            startDestination = Screen.Login.route,
            modifier = Modifier.padding(paddingValues)
        ) {

            composable(Screen.Login.route) {
                LoginScreen(
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                    onNavigateToProfile = {
                        navController.navigate(Screen.Profile.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    viewModel = viewModel
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    onNavigateToLogin = { navController.popBackStack() },
                    onNavigateToProfile = {
                        navController.navigate(Screen.Profile.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    viewModel = viewModel
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Profile.route) { inclusive = true }
                        }
                    },
                    viewModel = viewModel
                )
            }

            composable(Screen.Notes.route) {
                WipScreen()
            }

            composable(Screen.SharedNotes.route) {
                WipScreen()
            }
        }
    }
}
