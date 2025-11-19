package ucne.edu.notablelists.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.ui.graphics.vector.ImageVector
import ucne.edu.notablelists.navigation.Screen

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Notes : BottomNavItem(Screen.Notes.route, "Notas", Icons.Default.Edit)
    object SharedNotes : BottomNavItem(Screen.SharedNotes.route, "Notas Compartidas", Icons.Default.Outbox)
    object Profile : BottomNavItem(Screen.Profile.route, "Usuario", Icons.Default.AccountCircle)
}
