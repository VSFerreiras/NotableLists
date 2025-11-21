package ucne.edu.notablelists.presentation.users

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ucne.edu.notablelists.ui.theme.NotableListsTheme

@Composable
fun ProfileScreen(
    onNavigateToLogin: () -> Unit,
    viewModel: UserViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        Log.d("ProfileScreen", "currentUser = ${state.currentUser}")
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = state.currentUser.firstOrNull()?.uppercase() ?: "U",
                    style = MaterialTheme.typography.headlineLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Bienvenido, ${state.currentUser ?: "Usuario"}",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = {
                viewModel.onEvent(UserEvent.Logout)
                onNavigateToLogin()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Cerrar Sesión")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewProfileScreen() {
    NotableListsTheme {
        ProfileScreen(
            onNavigateToLogin = {},
            viewModel = hiltViewModel()
        )
    }
}
