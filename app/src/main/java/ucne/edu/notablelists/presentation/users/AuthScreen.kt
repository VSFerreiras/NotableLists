package ucne.edu.notablelists.presentation.users

import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ucne.edu.notablelists.ui.theme.NotableListsTheme

@Composable
fun AuthScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: UserViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.navigationTarget) {
        state.navigationTarget?.let { target ->
            when(target) {
                UserEvent.ToProfile -> onNavigateToProfile()
                UserEvent.ToLogin -> onNavigateToLogin()
                UserEvent.ToRegister -> onNavigateToRegister()
            }
            viewModel.onEvent(UserEvent.NavigationHandled)
        }
    }

    AuthContent(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateToProfile = onNavigateToProfile
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AuthContent(
    state: UserState,
    onEvent: (UserEvent) -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val scrollState = rememberScrollState()
    var isProcessingClick by remember { mutableStateOf(false) }
    var lastClickTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(state.isLoading) {
        isProcessingClick = state.isLoading
    }

    LaunchedEffect(state.error, state.usernameError, state.passwordError) {
        (state.error ?: state.usernameError ?: state.passwordError)?.let {
            isProcessingClick = false
        }
    }

    BackHandler(enabled = state.isLoading || isProcessingClick) {}

    state.showSkipDialog.takeIf { it }?.let {
        AlertDialog(
            onDismissRequest = { onEvent(UserEvent.DismissSkipDialog) },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("¿Estás seguro?") },
            text = { Text(state.authSkipDialogText) },
            confirmButton = {
                Button(
                    onClick = {
                        onEvent(UserEvent.DismissSkipDialog)
                        onNavigateToProfile()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) { Text("Omitir registro") }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(UserEvent.DismissSkipDialog) }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AndroidView(
                            factory = { ctx ->
                                ImageView(ctx).apply {
                                    val pm = ctx.packageManager
                                    runCatching {
                                        val appIcon = pm.getApplicationIcon(ctx.packageName)
                                        setImageDrawable(appIcon)
                                    }
                                    scaleType = ImageView.ScaleType.FIT_CENTER
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Notable Lists", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Column {
                    Text(
                        text = buildAnnotatedString {
                            append(state.authTitlePrefix)
                            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                append(state.authTitleAction)
                            }
                        },
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.authSubtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                state.error?.let {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(it, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                AuthTextField(
                    value = state.username,
                    onValueChange = { onEvent(UserEvent.UserNameChanged(it)) },
                    label = "Usuario",
                    error = state.usernameError,
                    icon = { Icon(Icons.Default.AccountCircle, null) },
                    enabled = !state.isLoading && !isProcessingClick,
                    hasError = state.usernameError != null
                )
                Spacer(modifier = Modifier.height(24.dp))

                AuthTextField(
                    value = state.password,
                    onValueChange = { onEvent(UserEvent.PasswordChanged(it)) },
                    label = "Contraseña",
                    error = state.passwordError,
                    isPassword = true,
                    enabled = !state.isLoading && !isProcessingClick,
                    hasError = state.passwordError != null
                )
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        val currentTime = System.currentTimeMillis()
                        val canClick = currentTime - lastClickTime > 1000 && !isProcessingClick && !state.isLoading
                        canClick.takeIf { it }?.let {
                            lastClickTime = currentTime
                            isProcessingClick = true
                            onEvent(UserEvent.SubmitAuth)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !state.isLoading && !isProcessingClick,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text(state.authButtonText, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Text(state.authFooterQuestion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(
                        onClick = {
                            val currentTime = System.currentTimeMillis()
                            val canClick = currentTime - lastClickTime > 1000 && !isProcessingClick && !state.isLoading
                            canClick.takeIf { it }?.let {
                                lastClickTime = currentTime
                                onEvent(UserEvent.AuthFooterClicked)
                            }
                        },
                        enabled = !state.isLoading && !isProcessingClick
                    ) {
                        Text(state.authFooterAction, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = {
                        val currentTime = System.currentTimeMillis()
                        val canClick = currentTime - lastClickTime > 1000 && !isProcessingClick && !state.isLoading
                        canClick.takeIf { it }?.let {
                            lastClickTime = currentTime
                            onEvent(UserEvent.ShowSkipDialog)
                        }
                    },
                    enabled = !state.isLoading && !isProcessingClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Continuar sin cuenta", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (state.isLoading || isProcessingClick) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.05f)).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = {}),
                    contentAlignment = Alignment.Center
                ) {
                    CircularWavyProgressIndicator(modifier = Modifier.size(48.dp), color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String? = null,
    isPassword: Boolean = false,
    icon: @Composable (() -> Unit)? = null,
    enabled: Boolean,
    hasError: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val cornerRadius by animateDpAsState(targetValue = if (isFocused) 8.dp else 24.dp, label = "shape")

    error?.let {
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp, start = 8.dp))
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = if (isPassword) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default,
        enabled = enabled,
        isError = hasError,
        shape = RoundedCornerShape(cornerRadius),
        interactionSource = interactionSource,
        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant, focusedBorderColor = MaterialTheme.colorScheme.primary),
        leadingIcon = icon
    )
}

@Preview(showBackground = true)
@Composable
private fun AuthScreenPreview() {
    NotableListsTheme {
        AuthContent(
            state = UserState(
                username = "PreviewUser",
                password = "password",
                isLoading = false
            ),
            onEvent = {},
            onNavigateToProfile = {}
        )
    }
}